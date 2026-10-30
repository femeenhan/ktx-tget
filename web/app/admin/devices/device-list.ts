import { computeStatus, type DeviceRow, type DeviceStatus } from '../../../lib/license';

export interface DeviceItem {
  row: DeviceRow;
  status: DeviceStatus;
}

/** 연장 요청: 활성 또는 (요청 후) 만료된 행에 pendingPlan이 있는 경우. */
export const isRenewal = (i: DeviceItem): boolean =>
  i.row.pendingPlan !== null && (i.status === 'active' || i.status === 'expired');

function rank(i: DeviceItem): number {
  if (i.status === 'pending') return 0;
  if (isRenewal(i)) return 1;
  if (i.status === 'active') return 2;
  return 3;
}

/** 연장 요청은 applyRequest가 agreedAt을 요청 시각으로 갱신하므로 그 시각으로 줄 세운다. */
const sortTime = (i: DeviceItem): number => (isRenewal(i) ? i.row.agreedAt : i.row.requestedAt).getTime();

export function sortDevices(rows: DeviceRow[], now: Date): DeviceItem[] {
  return rows
    .map((row) => ({ row, status: computeStatus(row, now) }))
    .sort((a, b) => rank(a) - rank(b) || sortTime(b) - sortTime(a));
}

export function countPending(rows: DeviceRow[], now: Date): { pending: number; renewal: number } {
  const items = rows.map((row) => ({ row, status: computeStatus(row, now) }));
  return {
    pending: items.filter((i) => i.status === 'pending').length,
    renewal: items.filter(isRenewal).length,
  };
}
