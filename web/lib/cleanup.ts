import { EVENT_RETENTION_MS } from './events';
import { computeStatus, type DeviceRow } from './license';
import type { Repo } from './repo';

const DAY_MS = 24 * 3600 * 1000;
/** 입금자명: 승인·회수 또는 (대기 중이면) 요청 후 30일 뒤 삭제. */
export const DEPOSITOR_RETENTION_MS = 30 * DAY_MS;
/** 기기 식별값·이용 기록: 만료·회수 후 1년 뒤 행 삭제. */
export const RECORD_RETENTION_MS = 365 * DAY_MS;

const latestTime = (times: (Date | null | undefined)[]): number | null => {
  const ms = times.filter((t): t is Date => t instanceof Date).map((t) => t.getTime());
  return ms.length === 0 ? null : Math.max(...ms);
};

export function selectDepositorsToClear(rows: DeviceRow[], now: Date): string[] {
  return rows
    .filter((r) => {
      if (r.depositor === null) return false;
      // 연장 요청 중이면 새 입금자명을 대조해야 하므로 남긴다.
      if (r.pendingPlan !== null) return false;
      const times = computeStatus(r, now) === 'pending' ? [r.requestedAt] : [r.approvedAt, r.revokedAt];
      const latest = latestTime(times);
      return latest !== null && now.getTime() - latest > DEPOSITOR_RETENTION_MS;
    })
    .map((r) => r.deviceId);
}

export function selectRowsToDelete(rows: DeviceRow[], now: Date): string[] {
  return rows
    .filter((r) => {
      const status = computeStatus(r, now);
      if (status !== 'expired' && status !== 'revoked') return false;
      const latest = latestTime([r.expiresAt, r.revokedAt]);
      return latest !== null && now.getTime() - latest > RECORD_RETENTION_MS;
    })
    .map((r) => r.deviceId);
}

export async function runCleanup(repo: Repo, now: Date): Promise<{ cleared: number; deleted: number; events: number }> {
  const rows = await repo.devices.list();
  const toDelete = selectRowsToDelete(rows, now);
  for (const id of toDelete) await repo.devices.delete(id);
  const gone = new Set(toDelete);
  const toClear = selectDepositorsToClear(rows, now).filter((id) => !gone.has(id));
  // 전체 행을 다시 쓰지 않고 depositor만 비워, 그 사이 들어온 요청·승인을 덮어쓰지 않는다.
  if (toClear.length > 0) await repo.devices.clearDepositor(toClear);
  const events = await repo.events.deleteBefore(new Date(now.getTime() - EVENT_RETENTION_MS));
  return { cleared: toClear.length, deleted: toDelete.length, events };
}
