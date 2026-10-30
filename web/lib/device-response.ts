import { computeStatus, type DeviceRow, type DeviceStatus } from './license';

export interface StatusResponse {
  status: 'none' | DeviceStatus;
  expiresAt: string | null;
  renewalPending: boolean;
  serverTime: string;
  notice: string | null;
}

export function buildStatusResponse(row: DeviceRow | null, now: Date, notice: string | null): StatusResponse {
  return {
    status: row ? computeStatus(row, now) : 'none',
    expiresAt: row?.expiresAt ? row.expiresAt.toISOString() : null,
    renewalPending: row ? row.pendingPlan !== null : false,
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
