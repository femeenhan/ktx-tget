import { describe, expect, it } from 'vitest';
import type { DeviceRow } from '../../../lib/license';
import { countPending, sortDevices } from './device-list';

const NOW = new Date('2026-10-30T00:00:00Z');
const ago = (d: number) => new Date(NOW.getTime() - d * 86400000);
const later = (d: number) => new Date(NOW.getTime() + d * 86400000);

function row(id: string, over: Partial<DeviceRow> = {}): DeviceRow {
  return {
    deviceId: id, depositor: '홍길동', requestedPlan: 'month', pendingPlan: null, status: 'pending',
    expiresAt: null, agreedAt: ago(100), note: null, appVersion: 1, requestedAt: ago(100), approvedAt: null,
    lastSeenAt: null, revokedAt: null, ...over,
  };
}

const rows = [
  row('active-new', { status: 'active', expiresAt: later(30), requestedAt: ago(1), agreedAt: ago(1) }),
  row('revoked', { status: 'revoked', revokedAt: ago(3), requestedAt: ago(2) }),
  row('renew-old', { status: 'active', expiresAt: later(10), pendingPlan: 'year', requestedAt: ago(90), agreedAt: ago(5) }),
  row('pending-old', { requestedAt: ago(10), agreedAt: ago(10) }),
  row('renew-expired', { status: 'active', expiresAt: ago(1), pendingPlan: 'month', requestedAt: ago(80), agreedAt: ago(2) }),
  row('pending-new', { requestedAt: ago(4), agreedAt: ago(4) }),
  row('expired', { status: 'active', expiresAt: ago(5), requestedAt: ago(3) }),
];

describe('sortDevices', () => {
  it('대기 → 연장 요청(최근 요청순) → 활성 → 나머지', () => {
    expect(sortDevices(rows, NOW).map((i) => i.row.deviceId)).toEqual([
      'pending-new', 'pending-old', 'renew-expired', 'renew-old', 'active-new', 'revoked', 'expired',
    ]);
  });
  it('계산 상태를 같이 돌려준다', () => {
    const byId = Object.fromEntries(sortDevices(rows, NOW).map((i) => [i.row.deviceId, i.status]));
    expect(byId['renew-expired']).toBe('expired');
    expect(byId['renew-old']).toBe('active');
  });
});

describe('countPending', () => {
  it('대기 건수와 연장 요청 건수', () => expect(countPending(rows, NOW)).toEqual({ pending: 2, renewal: 2 }));
});
