import { beforeEach, describe, expect, it, vi } from 'vitest';
vi.mock('../../../admin/require-admin', () => ({ requireAdmin: async () => {} }));
import { resetRepoForTests } from '../../../../lib/repo';
import { GET as versionGet } from '../../version/route';
import { GET as list, POST as create } from './route';
import { POST as setLatest } from './[id]/latest/route';

process.env.REPO = 'memory';

const version = () => versionGet(new Request('http://x/api/version'));
const body = (o: object) =>
  new Request('http://x/api/admin/releases', { method: 'POST', body: JSON.stringify(o), headers: { 'content-type': 'application/json' } });
const rel = (n: number) => ({ versionCode: n, versionName: `1.0.${n}`, blobUrl: `https://blob.example/apk/${n}.apk`, sizeBytes: 1000 + n, notes: 'n' });
const latest = (id: number) => setLatest(new Request('http://x', { method: 'POST' }), { params: Promise.resolve({ id: String(id) }) });

beforeEach(() => resetRepoForTests());

describe('releases API', () => {
  it('등록 -> 201, /api/version이 반환', async () => {
    const res = await create(body(rel(1)));
    expect(res.status).toBe(201);
    const v = await (await version()).json();
    expect(v.versionCode).toBe(1);
    expect(v.url).toBe('http://x/api/download?src=app');
  });

  it('두 번째 등록이 최신, setLatest로 되돌림', async () => {
    const first = await (await create(body(rel(1)))).json();
    await create(body(rel(2)));
    expect((await (await version()).json()).versionCode).toBe(2);
    const rows = await (await list()).json();
    expect(rows.map((r: { versionCode: number; isLatest: boolean }) => [r.versionCode, r.isLatest])).toEqual([[2, true], [1, false]]);
    expect((await latest(first.id)).status).toBe(204);
    expect((await (await version()).json()).versionCode).toBe(1);
  });

  it('중복 versionCode -> 409', async () => {
    await create(body(rel(1)));
    expect((await create(body(rel(1)))).status).toBe(409);
  });

  it.each([
    { ...rel(1), versionCode: 0 },
    { ...rel(1), versionName: '' },
    { ...rel(1), blobUrl: 'http://x/a.apk' },
    { ...rel(1), sizeBytes: 0 },
    { ...rel(1), versionName: 'x'.repeat(41) },
  ])('잘못된 본문 -> 400', async (o) => {
    expect((await create(body(o))).status).toBe(400);
  });

  it('없는 id latest -> 404', async () => {
    expect((await latest(999)).status).toBe(404);
  });
});
