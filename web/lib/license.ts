export type Plan = 'day' | 'month' | 'year' | 'lifetime';
/** 선택지 순서(짧은 것부터). 요청 검증과 관리 화면이 함께 쓴다. */
export const PLANS: readonly Plan[] = ['day', 'month', 'year', 'lifetime'];
export const DAY_MS = 24 * 60 * 60 * 1000;
export type DeviceStatus = 'pending' | 'active' | 'expired' | 'revoked';

export interface DeviceRow {
  deviceId: string;
  depositor: string | null;
  requestedPlan: Plan;
  pendingPlan: Plan | null;
  status: DeviceStatus;
  expiresAt: Date | null;
  agreedAt: Date;
  note: string | null;
  appVersion: number | null;
  requestedAt: Date;
  approvedAt: Date | null;
  lastSeenAt: Date | null;
  revokedAt: Date | null;
}

function addMonths(from: Date, months: number): Date {
  const d = new Date(from.getTime());
  const day = d.getUTCDate();
  d.setUTCDate(1);
  d.setUTCMonth(d.getUTCMonth() + months);
  const lastDay = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0)).getUTCDate();
  d.setUTCDate(Math.min(day, lastDay));
  return d;
}

/** lifetime → null. day는 정확히 24시간. month/year는 UTC 달력 기준, 말일로 보정. */
export function addPlan(from: Date, plan: Plan): Date | null {
  if (plan === 'lifetime') return null;
  if (plan === 'day') return new Date(from.getTime() + DAY_MS);
  return addMonths(from, plan === 'month' ? 1 : 12);
}

export function computeStatus(row: DeviceRow, now: Date): DeviceStatus {
  if (row.status === 'active' && row.expiresAt !== null && row.expiresAt <= now) return 'expired';
  return row.status;
}

export function applyRequest(
  row: DeviceRow | null,
  input: { deviceId: string; depositor: string; plan: Plan; versionCode: number; now: Date },
): DeviceRow {
  const { deviceId, depositor, plan, versionCode, now } = input;
  if (row === null) {
    return {
      deviceId,
      depositor,
      requestedPlan: plan,
      pendingPlan: null,
      status: 'pending',
      expiresAt: null,
      agreedAt: now,
      note: null,
      appVersion: versionCode,
      requestedAt: now,
      approvedAt: null,
      lastSeenAt: now,
      revokedAt: null,
    };
  }
  const status = computeStatus(row, now);
  if (status === 'pending') {
    return { ...row, depositor, requestedPlan: plan, agreedAt: now, appVersion: versionCode, lastSeenAt: now };
  }
  if (status === 'active') {
    return { ...row, depositor, pendingPlan: plan, agreedAt: now, lastSeenAt: now };
  }
  return {
    ...row,
    status: 'pending',
    depositor,
    requestedPlan: plan,
    pendingPlan: null,
    expiresAt: null,
    approvedAt: null,
    revokedAt: null,
    agreedAt: now,
    requestedAt: now,
    lastSeenAt: now,
  };
}

export function approve(row: DeviceRow, plan: Plan, now: Date): DeviceRow {
  return { ...row, status: 'active', approvedAt: now, expiresAt: addPlan(now, plan), pendingPlan: null, revokedAt: null };
}

export function extend(row: DeviceRow, plan: Plan, now: Date): DeviceRow {
  const base = row.expiresAt && row.expiresAt > now ? row.expiresAt : now;
  return {
    ...row,
    status: 'active',
    expiresAt: addPlan(base, plan),
    pendingPlan: null,
    // 입금자명 보관 기간(30일)을 연장 승인 시점부터 다시 센다.
    approvedAt: now,
  };
}

export function revoke(row: DeviceRow, now: Date): DeviceRow {
  return { ...row, status: 'revoked', revokedAt: now, pendingPlan: null };
}
