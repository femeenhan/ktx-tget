import { beforeEach, describe, expect, it } from 'vitest';
import { DEPOSITOR_RETENTION_MS, runCleanup, selectDepositorsToClear } from './cleanup';
import { createMemoryRepo } from './repo';
import type { DeviceRow } from './license';
import { GET } from '../app/api/cron/cleanup/route';

const NOW = new Date('2026-10-30T00:00:00Z');
const ago = (d: number) => new Date(NOW.getTime() - d * 24 * 3600 * 1000);
const row = (o: Partial<DeviceRow>): DeviceRow => ({
  deviceId: 'a1b2c3d4e5f60718', depositor: '홍길동', status: 'approved',
  approvedAt: null, revokedAt: null, ...o,
} as DeviceRow);

describe('selectDepositorsToClear', () => {
  it('retention is 30 days', () => expect(DEPOSITOR_RETENTION_MS).toBe(30 * 86400000));
  it('29일 → 안 지움', () => expect(selectDepositorsToClear([row({ approvedAt: ago(29) })], NOW)).toEqual([]));
  it('31일 → 지움', () => expect(selectDepositorsToClear([row({ approvedAt: ago(31) })], NOW)).toEqual(['a1b2c3d4e5f60718']));
  it('pending → 안 지움', () => expect(selectDepositorsToClear([row({ status: 'pending' })], NOW)).toEqual([]));
  it('나중 시각 기준', () => {
    expect(selectDepositorsToClear([row({ approvedAt: ago(60), revokedAt: ago(31) })], NOW)).toHaveLength(1);
    expect(selectDepositorsToClear([row({ approvedAt: ago(60), revokedAt: ago(5) })], NOW)).toEqual([]);
  });
  it('depositor null → 안 지움', () => expect(selectDepositorsToClear([row({ depositor: null, approvedAt: ago(40) })], NOW)).toEqual([]));
});

describe('runCleanup', () => {
  it('입금자명을 비우고 개수를 돌려준다', async () => {
    const repo = createMemoryRepo();
    await repo.devices.upsert(row({ approvedAt: ago(31) }));
    expect(await runCleanup(repo, NOW)).toBe(1);
    expect((await repo.devices.list())[0].depositor).toBeNull();
  });
});

describe('GET /api/cron/cleanup', () => {
  beforeEach(() => { process.env.REPO = 'memory'; process.env.CRON_SECRET = 's3cret'; });
  const call = (auth: string) => GET(new Request('http://x/api/cron/cleanup', { headers: { authorization: auth } }));
  it('틀린 bearer → 401', async () => expect((await call('Bearer nope')).status).toBe(401));
  it('맞는 bearer → 200', async () => {
    const res = await call('Bearer s3cret');
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ cleared: 0 });
  });
});
