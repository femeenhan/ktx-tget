import { describe, expect, it } from 'vitest';
import {
  addPlan,
  applyRequest,
  approve,
  computeStatus,
  extend,
  isTrialActive,
  TRIAL_MS,
  trialUntil,
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
  it('day is exactly 24 hours', () => {
    expect(addPlan(d('2026-01-31T15:00:00Z'), 'day')).toEqual(d('2026-02-01T15:00:00Z'));
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
  it('null creates pending with trial window', () => {
    expect(applyRequest(null, input)).toEqual({
      deviceId: 'dev1',
      depositor: '김철수',
      requestedPlan: 'year',
      pendingPlan: null,
      status: 'pending',
      expiresAt: new Date(NOW.getTime() + TRIAL_MS),
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

describe('trial (임시 이용)', () => {
  it('새 기기의 첫 요청은 TRIAL_MS 동안 임시 이용', () => {
    const r = applyRequest(null, input);
    expect(r.status).toBe('pending');
    expect(trialUntil(r)?.getTime()).toBe(NOW.getTime() + TRIAL_MS);
    expect(isTrialActive(r, NOW)).toBe(true);
    expect(isTrialActive(r, new Date(NOW.getTime() + TRIAL_MS))).toBe(false);
    expect(computeStatus(r, new Date(NOW.getTime() + TRIAL_MS))).toBe('pending');
  });
  it('대기 중 다시 요청해도 임시 이용 시각은 그대로', () => {
    const first = applyRequest(null, input);
    const again = applyRequest(first, { ...input, now: new Date(NOW.getTime() + 10 * 60_000) });
    expect(trialUntil(again)).toEqual(trialUntil(first));
  });
  it('만료·회수 뒤 다시 요청하면 임시 이용 없음', () => {
    const expired = row({ status: 'active', expiresAt: d('2026-02-01T00:00:00Z') });
    expect(trialUntil(applyRequest(expired, input))).toBeNull();
    const revoked = row({ status: 'revoked', revokedAt: NOW });
    expect(trialUntil(applyRequest(revoked, input))).toBeNull();
  });
  it('승인되면 정식 만료일로 바뀌고 trial이 아니다', () => {
    const r = approve(applyRequest(null, input), 'day', NOW);
    expect(trialUntil(r)).toBeNull();
    expect(r.expiresAt?.getTime()).toBe(NOW.getTime() + 24 * 3600_000);
  });
  it('활성·회수 행에는 trial이 없다', () => {
    expect(trialUntil(row({ status: 'active', expiresAt: d('2030-01-01T00:00:00Z') }))).toBeNull();
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
    expect(r.approvedAt).toEqual(NOW);
  });
  it('extend sets approvedAt to now', () => {
    const r = extend(row({ status: 'active', expiresAt: d('2026-04-01T00:00:00Z'), approvedAt: d('2025-12-01T00:00:00Z') }), 'year', NOW);
    expect(r.approvedAt).toEqual(NOW);
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
