import { describe, expect, it } from 'vitest';
import { createMemoryRepo } from '../../../lib/repo.memory';
import type { DeviceRow } from '../../../lib/license';
import {
  approveDeviceCore,
  deleteDeviceCore,
  extendDeviceCore,
  revokeDeviceCore,
  saveNoteCore,
} from './device-admin';

const NOW = new Date('2026-10-30T00:00:00Z');
const ID = 'a1b2c3d4e5f60718';

function row(over: Partial<DeviceRow> = {}): DeviceRow {
  return {
    deviceId: ID, depositor: '홍길동', requestedPlan: 'month', pendingPlan: null, status: 'pending',
    expiresAt: null, agreedAt: NOW, note: null, appVersion: 5, requestedAt: NOW, approvedAt: null,
    lastSeenAt: NOW, revokedAt: null, ...over,
  };
}
async function setup(r: DeviceRow) {
  const repo = createMemoryRepo();
  await repo.devices.upsert(r);
  return repo;
}

describe('device-admin', () => {
  it('pending -> approve(month): active, 만료 +1개월', async () => {
    const repo = await setup(row());
    await approveDeviceCore(repo, ID, 'month', NOW);
    const d = (await repo.devices.get(ID))!;
    expect(d.status).toBe('active');
    expect(d.expiresAt).toEqual(new Date('2026-11-30T00:00:00Z'));
  });

  it('active에 approve는 throw', async () => {
    const repo = await setup(row({ status: 'active', expiresAt: new Date('2027-01-01T00:00:00Z') }));
    await expect(approveDeviceCore(repo, ID, 'month', NOW)).rejects.toThrow('not pending');
  });

  it('active + pendingPlan -> extend: pendingPlan 해제, 만료 연장', async () => {
    const exp = new Date('2026-12-01T00:00:00Z');
    const repo = await setup(row({ status: 'active', expiresAt: exp, pendingPlan: 'year' }));
    await extendDeviceCore(repo, ID, 'year', NOW);
    const d = (await repo.devices.get(ID))!;
    expect(d.pendingPlan).toBeNull();
    expect(d.expiresAt).toEqual(new Date('2027-12-01T00:00:00Z'));
  });

  it('pending에 extend는 throw', async () => {
    const repo = await setup(row());
    await expect(extendDeviceCore(repo, ID, 'month', NOW)).rejects.toThrow();
  });

  it('revoke: revoked', async () => {
    const repo = await setup(row());
    await revokeDeviceCore(repo, ID, NOW);
    expect((await repo.devices.get(ID))!.status).toBe('revoked');
  });

  it('saveNote: trim, 빈 문자열은 null', async () => {
    const repo = await setup(row());
    await saveNoteCore(repo, ID, '  메모  ');
    expect((await repo.devices.get(ID))!.note).toBe('메모');
    await saveNoteCore(repo, ID, '   ');
    expect((await repo.devices.get(ID))!.note).toBeNull();
  });

  it('delete: 제거', async () => {
    const repo = await setup(row());
    await deleteDeviceCore(repo, ID);
    expect(await repo.devices.get(ID)).toBeNull();
  });

  it('없는 id / 잘못된 plan은 throw', async () => {
    const repo = await setup(row());
    await expect(revokeDeviceCore(repo, 'nope', NOW)).rejects.toThrow('device not found');
    await expect(approveDeviceCore(repo, ID, 'week' as never, NOW)).rejects.toThrow('invalid plan');
  });
});

describe('통계 이벤트', () => {
  it('승인은 approve/new, 연장은 approve/extend로 남는다', async () => {
    const repo = await setup(row());
    await approveDeviceCore(repo, ID, 'day', NOW);
    await extendDeviceCore(repo, ID, 'month', NOW);
    const ev = await repo.events.list(new Date(0));
    expect(ev.map((e) => [e.kind, e.path, e.ref])).toEqual([['approve', 'new', 'day'], ['approve', 'extend', 'month']]);
  });
});
