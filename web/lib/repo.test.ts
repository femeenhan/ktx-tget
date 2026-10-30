import { describe, expect, it } from 'vitest';
import { createMemoryRepo } from './repo.memory';
import type { DeviceRow } from './license';

const NOW = new Date('2026-03-10T00:00:00Z');
function dev(id: string): DeviceRow {
  return {
    deviceId: id, depositor: 'a', requestedPlan: 'month', pendingPlan: null, status: 'pending',
    expiresAt: null, agreedAt: NOW, note: null, appVersion: 1, requestedAt: NOW,
    approvedAt: null, lastSeenAt: null, revokedAt: null,
  };
}
const rel = (code: number) => ({ versionCode: code, versionName: `v${code}`, blobUrl: `u${code}`, sizeBytes: 10, notes: null });

describe('MemoryRepo', () => {
  it('devices: upsert/get/list/delete', async () => {
    const r = createMemoryRepo();
    expect(await r.devices.get('a')).toBeNull();
    await r.devices.upsert(dev('a'));
    await r.devices.upsert({ ...dev('a'), depositor: 'b' });
    await r.devices.upsert(dev('c'));
    expect((await r.devices.get('a'))?.depositor).toBe('b');
    expect(await r.devices.list()).toHaveLength(2);
    await r.devices.delete('a');
    expect(await r.devices.get('a')).toBeNull();
  });

  it('devices: touch updates only lastSeenAt/appVersion', async () => {
    const r = createMemoryRepo();
    await r.devices.upsert({ ...dev('a'), note: 'keep', appVersion: 3 });
    const t = new Date('2026-04-01T00:00:00Z');
    await r.devices.touch('a', t, 9);
    let d = (await r.devices.get('a'))!;
    expect(d).toEqual({ ...dev('a'), note: 'keep', appVersion: 9, lastSeenAt: t });
    await r.devices.touch('a', NOW, null);
    d = (await r.devices.get('a'))!;
    expect(d.appVersion).toBe(9);
    expect(d.lastSeenAt).toEqual(NOW);
    await r.devices.touch('missing', NOW, 1);
    expect(await r.devices.get('missing')).toBeNull();
  });

  it('releases: insert makes the new one the only latest', async () => {
    const r = createMemoryRepo();
    expect(await r.releases.latest()).toBeNull();
    const a = await r.releases.insert(rel(1));
    const b = await r.releases.insert(rel(2));
    expect(a.id).not.toBe(b.id);
    expect((await r.releases.latest())?.versionCode).toBe(2);
    expect((await r.releases.list()).filter((x) => x.isLatest)).toHaveLength(1);
  });

  it('releases: duplicate versionCode throws DUPLICATE_VERSION', async () => {
    const r = createMemoryRepo();
    await r.releases.insert(rel(1));
    await expect(r.releases.insert(rel(1))).rejects.toMatchObject({ code: 'DUPLICATE_VERSION' });
    expect(await r.releases.list()).toHaveLength(1);
  });

  it('releases: setLatest clears others', async () => {
    const r = createMemoryRepo();
    const a = await r.releases.insert(rel(1));
    await r.releases.insert(rel(2));
    await r.releases.setLatest(a.id);
    expect((await r.releases.latest())?.id).toBe(a.id);
    expect((await r.releases.list()).filter((x) => x.isLatest)).toHaveLength(1);
  });

  it('settings: defaults to schema keys, set overrides', async () => {
    const r = createMemoryRepo();
    const s = await r.settings.all();
    expect(s.price_month).toBe('0');
    await r.settings.set('price_month', '3000');
    expect((await r.settings.all()).price_month).toBe('3000');
  });
});
