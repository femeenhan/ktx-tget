import { computeStatus, isTrialActive, type DeviceRow, type DeviceStatus } from './license';

export interface StatusResponse {
  status: 'none' | DeviceStatus;
  expiresAt: string | null;
  renewalPending: boolean;
  /** 입금 확인 전 임시 이용 중이면 true. 이때 status는 active, expiresAt은 임시 이용 종료 시각. */
  trial: boolean;
  serverTime: string;
  notice: string | null;
}

export function buildStatusResponse(row: DeviceRow | null, now: Date, notice: string | null): StatusResponse {
  // 임시 이용 중인 대기 행은 앱에 active로 보여 바로 쓸 수 있게 한다. 끝나면 다시 pending으로 보인다.
  const trial = row !== null && isTrialActive(row, now);
  const status: StatusResponse['status'] = row === null ? 'none' : trial ? 'active' : computeStatus(row, now);
  const showExpiry = row !== null && (status === 'active' || status === 'expired');
  return {
    status,
    expiresAt: showExpiry && row.expiresAt ? row.expiresAt.toISOString() : null,
    renewalPending: row ? row.pendingPlan !== null : false,
    trial,
    serverTime: now.toISOString(),
    notice,
  };
}

export const normalizeNotice = (v: string | undefined): string | null => (v ? v : null);

export function parseDeviceId(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const id = v.trim().toLowerCase();
  return /^[a-f0-9]{16}$/.test(id) ? id : null;
}
