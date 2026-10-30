import { beforeEach, describe, expect, it } from 'vitest';
import { DEPOSITOR_RETENTION_MS, RECORD_RETENTION_MS, runCleanup, selectDepositorsToClear, selectRowsToDelete } from './cleanup';
import { createMemoryRepo } from './repo';
import type { DeviceRow } from './license';
import { GET } from '../app/api/cron/cleanup/route';

const NOW = new Date('2026-10-30T00:00:00Z');
const ago = (d: number) => new Date(NOW.getTime() - d * 24 * 3600 * 1000);
const row = (o: Partial<DeviceRow>): DeviceRow => ({
  deviceId: 'a1b2c3d4e5f60718', depositor: '홍길동', status: 'active', pendingPlan: null,
  approvedAt: null, revokedAt: null, ...o,
} as DeviceRow);

describe('selectDepositorsToClear', () => {
  it('retention is 30 days', () => expect(DEPOSITOR_RETENTION_MS).toBe(30 * 86400000));
  it('29일 → 안 지움', () => expect(selectDepositorsToClear([row({ approvedAt: ago(29) })], NOW)).toEqual([]));
  it('31일 → 지움', () => expect(selectDepositorsToClear([row({ approvedAt: ago(31) })], NOW)).toEqual(['a1b2c3d4e5f60718']));
  it('pending 요청 29일 → 안 지움', () =>
    expect(selectDepositorsToClear([row({ status: 'pending', requestedAt: ago(29) })], NOW)).toEqual([]));
  it('pending 요청 31일 → 지움', () =>
    expect(selectDepositorsToClear([row({ status: 'pending', requestedAt: ago(31) })], NOW)).toEqual(['a1b2c3d4e5f60718']));
  it('나중 시각 기준', () => {
    expect(selectDepositorsToClear([row({ approvedAt: ago(60), revokedAt: ago(31) })], NOW)).toHaveLength(1);
    expect(selectDepositorsToClear([row({ approvedAt: ago(60), revokedAt: ago(5) })], NOW)).toEqual([]);
  });
  it('active row with pendingPlan and approvedAt 60d ago → not selected', () =>
    expect(selectDepositorsToClear([row({ status: 'active', pendingPlan: 'month', approvedAt: ago(60) })], NOW)).toEqual([]));
  it('depositor null → 안 지움', () => expect(selectDepositorsToClear([row({ depositor: null, approvedAt: ago(40) })], NOW)).toEqual([]));
});

describe('selectRowsToDelete', () => {
  it('retention is 365 days', () => expect(RECORD_RETENTION_MS).toBe(365 * 86400000));
  it('만료 364일 → 안 지움', () =>
    expect(selectRowsToDelete([row({ status: 'active', expiresAt: ago(364) })], NOW)).toEqual([]));
  it('만료 366일 → 지움 (저장 status가 active여도 계산 상태로 판단)', () =>
    expect(selectRowsToDelete([row({ status: 'active', expiresAt: ago(366) })], NOW)).toEqual(['a1b2c3d4e5f60718']));
  it('회수 364일 → 안 지움', () =>
    expect(selectRowsToDelete([row({ status: 'revoked', revokedAt: ago(364) })], NOW)).toEqual([]));
  it('회수 366일 → 지움', () =>
    expect(selectRowsToDelete([row({ status: 'revoked', revokedAt: ago(366) })], NOW)).toEqual(['a1b2c3d4e5f60718']));
  it('회수 시각과 만료일 중 나중 기준', () =>
    expect(selectRowsToDelete([row({ status: 'revoked', expiresAt: ago(400), revokedAt: ago(100) })], NOW)).toEqual([]));
  it('활성 행은 지우지 않는다 (영구, 만료 전, 오래된 승인)', () =>
    expect(selectRowsToDelete([
      row({ deviceId: 'p', status: 'active', expiresAt: null, approvedAt: ago(1000) }),
      row({ deviceId: 'q', status: 'active', expiresAt: new Date(NOW.getTime() + 86400000), approvedAt: ago(1000) }),
    ], NOW)).toEqual([]));
  it('pending 행은 지우지 않는다', () =>
    expect(selectRowsToDelete([row({ status: 'pending', requestedAt: ago(1000) })], NOW)).toEqual([]));
});

describe('runCleanup', () => {
  it('입금자명을 비우고 오래된 행을 지운 뒤 개수를 돌려준다', async () => {
    const repo = createMemoryRepo();
    await repo.devices.upsert(row({ deviceId: 'keep', approvedAt: ago(31), note: '메모', requestedAt: ago(31) }));
    await repo.devices.upsert(row({ deviceId: 'old', status: 'revoked', revokedAt: ago(400), requestedAt: ago(500) }));
    await repo.devices.upsert(row({ deviceId: 'spam', status: 'pending', requestedAt: ago(31) }));
    expect(await runCleanup(repo, NOW)).toEqual({ cleared: 2, deleted: 1 });
    const left = await repo.devices.list();
    expect(left.map((r) => r.deviceId).sort()).toEqual(['keep', 'spam']);
    expect(left.every((r) => r.depositor === null)).toBe(true);
    expect(left.find((r) => r.deviceId === 'keep')!.note).toBe('메모');
  });
});

describe('MemoryRepo.clearDepositor', () => {
  it('지정한 행의 depositor만 비운다', async () => {
    const repo = createMemoryRepo();
    await repo.devices.upsert(row({ deviceId: 'a', note: 'n' }));
    await repo.devices.upsert(row({ deviceId: 'b' }));
    await repo.devices.clearDepositor(['a', 'missing']);
    expect(await repo.devices.get('a')).toEqual(row({ deviceId: 'a', note: 'n', depositor: null }));
    expect((await repo.devices.get('b'))!.depositor).toBe('홍길동');
    await repo.devices.clearDepositor([]);
  });
});

describe('GET /api/cron/cleanup', () => {
  beforeEach(() => { process.env.REPO = 'memory'; process.env.CRON_SECRET = 's3cret'; });
  const call = (auth: string) => GET(new Request('http://x/api/cron/cleanup', { headers: { authorization: auth } }));
  it('틀린 bearer → 401', async () => expect((await call('Bearer nope')).status).toBe(401));
  it('맞는 bearer → 200', async () => {
    const res = await call('Bearer s3cret');
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ cleared: 0, deleted: 0 });
  });
});
