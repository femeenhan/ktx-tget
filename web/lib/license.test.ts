import { describe, expect, it } from 'vitest';
import {
  addPlan,
  applyRequest,
  approve,
  computeStatus,
  extend,
  revoke,
  type DeviceRow,
} from './license';

const d = (s: string) => new Date(s);
const NOW = d('2026-03-10T00:00:00Z');

function row(over: Partial<DeviceRow> = {}): DeviceRow {
  return {
    deviceId: 'dev1',
    depositor: '홍길동',
    requestedPlan: 'month',
    pendingPlan: null,
    status: 'pending',
    expiresAt: null,
    agreedAt: d('2026-01-01T00:00:00Z'),
    note: null,
    appVersion: 1,
    requestedAt: d('2026-01-01T00:00:00Z'),
    approvedAt: null,
    lastSeenAt: d('2026-01-01T00:00:00Z'),
    revokedAt: null,
    ...over,
  };
}

const input = { deviceId: 'dev1', depositor: '김철수', plan: 'year' as const, versionCode: 7, now: NOW };

describe('addPlan', () => {
  it('month clamps to month end', () => {
    expect(addPlan(d('2026-01-31T00:00:00Z'), 'month')).toEqual(d('2026-02-28T00:00:00Z'));
    expect(addPlan(d('2024-01-31T00:00:00Z'), 'month')).toEqual(d('2024-02-29T00:00:00Z'));
  });
  it('year adds 12 months with clamp', () => {
    expect(addPlan(d('2026-03-10T00:00:00Z'), 'year')).toEqual(d('2027-03-10T00:00:00Z'));
    expect(addPlan(d('2024-02-29T00:00:00Z'), 'year')).toEqual(d('2025-02-28T00:00:00Z'));
  });
  it('lifetime is null', () => {
    expect(addPlan(NOW, 'lifetime')).toBeNull();
  });
});

describe('computeStatus', () => {
  it('active with past expiry is expired', () => {
    expect(computeStatus(row({ status: 'active', expiresAt: d('2026-03-09T00:00:00Z') }), NOW)).toBe('expired');
  });
  it('active with null expiry stays active', () => {
    expect(computeStatus(row({ status: 'active' }), NOW)).toBe('active');
  });
  it('active with future expiry stays active', () => {
    expect(computeStatus(row({ status: 'active', expiresAt: d('2026-04-01T00:00:00Z') }), NOW)).toBe('active');
  });
  it('other statuses pass through', () => {
    expect(computeStatus(row({ status: 'revoked' }), NOW)).toBe('revoked');
  });
});

describe('applyRequest', () => {
  it('null creates pending', () => {
    expect(applyRequest(null, input)).toEqual({
      deviceId: 'dev1',
      depositor: '김철수',
      requestedPlan: 'year',
      pendingPlan: null,
      status: 'pending',
      expiresAt: null,
      agreedAt: NOW,
      note: null,
      appVersion: 7,
      requestedAt: NOW,
      approvedAt: null,
      lastSeenAt: NOW,
      revokedAt: null,
    });
  });
  it('pending updates depositor and plan', () => {
    const r = applyRequest(row(), input);
    expect(r.status).toBe('pending');
    expect(r.depositor).toBe('김철수');
    expect(r.requestedPlan).toBe('year');
    expect(r.appVersion).toBe(7);
    expect(r.agreedAt).toEqual(NOW);
    expect(r.lastSeenAt).toEqual(NOW);
  });
  it('active keeps status and sets pendingPlan', () => {
    const exp = d('2026-04-01T00:00:00Z');
    const r = applyRequest(row({ status: 'active', expiresAt: exp, requestedPlan: 'month' }), input);
    expect(r.status).toBe('active');
    expect(r.expiresAt).toEqual(exp);
    expect(r.pendingPlan).toBe('year');
    expect(r.requestedPlan).toBe('month');
    expect(r.depositor).toBe('김철수');
  });
  it('computed-expired active becomes pending', () => {
    const r = applyRequest(row({ status: 'active', expiresAt: d('2026-03-01T00:00:00Z') }), input);
    expect(r.status).toBe('pending');
    expect(r.pendingPlan).toBeNull();
    expect(r.expiresAt).toBeNull();
    expect(r.requestedPlan).toBe('year');
  });
  it.each(['expired', 'revoked'] as const)('%s becomes pending', (status) => {
    const r = applyRequest(
      row({ status, pendingPlan: 'month', approvedAt: d('2026-01-02T00:00:00Z'), revokedAt: d('2026-02-01T00:00:00Z') }),
      input,
    );
    expect(r.status).toBe('pending');
    expect(r.pendingPlan).toBeNull();
    expect(r.expiresAt).toBeNull();
    expect(r.approvedAt).toBeNull();
    expect(r.revokedAt).toBeNull();
    expect(r.requestedPlan).toBe('year');
    expect(r.requestedAt).toEqual(NOW);
  });
  it('does not mutate input', () => {
    const base = row();
    applyRequest(base, input);
    expect(base).toEqual(row());
  });
});

describe('approve', () => {
  it('activates with expiry', () => {
    const r = approve(row({ pendingPlan: 'year' }), 'month', NOW);
    expect(r.status).toBe('active');
    expect(r.approvedAt).toEqual(NOW);
    expect(r.expiresAt).toEqual(d('2026-04-10T00:00:00Z'));
    expect(r.pendingPlan).toBeNull();
    expect(r.revokedAt).toBeNull();
  });
  it('lifetime has null expiry', () => {
    expect(approve(row(), 'lifetime', NOW).expiresAt).toBeNull();
  });
});

describe('extend', () => {
  it('uses expiresAt when not yet expired', () => {
    const r = extend(row({ status: 'active', expiresAt: d('2026-04-01T00:00:00Z'), pendingPlan: 'month', approvedAt: d('2026-01-02T00:00:00Z') }), 'month', NOW);
    expect(r.expiresAt).toEqual(d('2026-05-01T00:00:00Z'));
    expect(r.pendingPlan).toBeNull();
    expect(r.status).toBe('active');
    expect(r.approvedAt).toEqual(d('2026-01-02T00:00:00Z'));
  });
  it('uses now when expired', () => {
    const r = extend(row({ status: 'expired', expiresAt: d('2026-03-01T00:00:00Z') }), 'month', NOW);
    expect(r.expiresAt).toEqual(d('2026-04-10T00:00:00Z'));
    expect(r.status).toBe('active');
    expect(r.approvedAt).toEqual(NOW);
  });
  it('lifetime gives null', () => {
    expect(extend(row({ status: 'active', expiresAt: d('2026-04-01T00:00:00Z') }), 'lifetime', NOW).expiresAt).toBeNull();
  });
});

describe('revoke', () => {
  it('marks revoked', () => {
    const r = revoke(row({ status: 'active', pendingPlan: 'month' }), NOW);
    expect(r.status).toBe('revoked');
    expect(r.revokedAt).toEqual(NOW);
    expect(r.pendingPlan).toBeNull();
  });
});
