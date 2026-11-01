export type Plan = 'day' | 'month' | 'year' | 'lifetime';
/** 선택지 순서(짧은 것부터). 요청 검증과 관리 화면이 함께 쓴다. */
export const PLANS: readonly Plan[] = ['day', 'month', 'year', 'lifetime'];
export const DAY_MS = 24 * 60 * 60 * 1000;
/**
 * 임시 이용 시간. 기기가 처음 활성화를 요청하면 입금 확인 전에도 이만큼은 바로 쓸 수 있게 한다.
 * 판매자가 입금을 확인해 승인하기까지의 지연(외출 등)을 구매자가 기다리지 않게 하려는 것이다.
 * 기기 ID당 한 번뿐이다(행이 새로 만들어질 때만). 만료·회수 뒤 다시 요청해도 주지 않는다.
 */
export const TRIAL_MS = 60 * 60 * 1000;
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

/**
 * 대기(pending) 행의 expiresAt은 임시 이용이 끝나는 시각이다(승인 전까지 다른 뜻이 없다).
 * 승인되면 approve가 정식 만료일로 덮어쓴다. 지난 뒤에도 값은 남아 「이미 임시 이용을 받은 기기」임을 알려 준다.
 */
export const trialUntil = (row: DeviceRow): Date | null => (row.status === 'pending' ? row.expiresAt : null);

export function isTrialActive(row: DeviceRow, now: Date): boolean {
  const until = trialUntil(row);
  return until !== null && until > now;
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
      expiresAt: new Date(now.getTime() + TRIAL_MS),
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
