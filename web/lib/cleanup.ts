import type { DeviceRow } from './license';
import type { Repo } from './repo';

export const DEPOSITOR_RETENTION_MS = 30 * 24 * 3600 * 1000;

export function selectDepositorsToClear(rows: DeviceRow[], now: Date): string[] {
  return rows
    .filter((r) => {
      if (r.depositor === null) return false;
      // 연장 요청 중이면 새 입금자명을 대조해야 하므로 남긴다.
      if (r.pendingPlan !== null) return false;
      const times = [r.approvedAt, r.revokedAt].filter((t): t is Date => t !== null);
      if (times.length === 0) return false;
      const latest = Math.max(...times.map((t) => t.getTime()));
      return now.getTime() - latest > DEPOSITOR_RETENTION_MS;
    })
    .map((r) => r.deviceId);
}

export async function runCleanup(repo: Repo, now: Date): Promise<number> {
  const rows = await repo.devices.list();
  const ids = new Set(selectDepositorsToClear(rows, now));
  for (const r of rows) if (ids.has(r.deviceId)) await repo.devices.upsert({ ...r, depositor: null });
  return ids.size;
}
