import { beforeEach, describe, expect, it } from 'vitest';
import { getRepo, resetRepoForTests } from '../../../lib/repo';
import { approve } from '../../../lib/license';
import { POST as requestPost } from './request/route';
import { GET as statusGet } from './status/route';
import { GET as configGet } from '../config/route';
import { GET as versionGet } from '../version/route';

const ID = 'a1b2c3d4e5f60718';

beforeEach(() => {
  process.env.REPO = 'memory';
  resetRepoForTests();
});

const post = (body: unknown, raw = false) =>
  requestPost(new Request('http://x/api/device/request', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: raw ? (body as string) : JSON.stringify(body),
  }));
const status = (q: string) => statusGet(new Request(`http://x/api/device/status?${q}`));
const valid = { deviceId: ID, depositor: '홍길동', plan: 'month', versionCode: 5, agreed: true };

describe('POST /api/device/request', () => {
  it('빈 본문 / 깨진 JSON -> 400', async () => {
    expect((await requestPost(new Request('http://x', { method: 'POST' }))).status).toBe(400);
    const res = await post('{bad', true);
    expect(res.status).toBe(400);
    expect(typeof (await res.json()).error).toBe('string');
  });
  it('agreed 없음/false -> 400', async () => {
    const { agreed: _a, ...rest } = valid;
    void _a;
    expect((await post(rest)).status).toBe(400);
    expect((await post({ ...valid, agreed: false })).status).toBe(400);
  });
  it('plan 이상값 -> 400', async () => {
    expect((await post({ ...valid, plan: 'weekly' })).status).toBe(400);
  });
  it('deviceId / depositor / versionCode 검증', async () => {
    expect((await post({ ...valid, deviceId: 'xyz' })).status).toBe(400);
    expect((await post({ ...valid, depositor: '   ' })).status).toBe(400);
    expect((await post({ ...valid, depositor: 'a'.repeat(41) })).status).toBe(400);
    expect((await post({ ...valid, versionCode: 0 })).status).toBe(400);
    expect((await post({ ...valid, versionCode: 1.5 })).status).toBe(400);
  });
  it('정상 요청 -> pending, 소문자/trim 정규화', async () => {
    const res = await post({ ...valid, deviceId: `  ${ID.toUpperCase()} ` });
    expect(res.status).toBe(200);
    const j = await res.json();
    expect(j.status).toBe('pending');
    expect(j.renewalPending).toBe(false);
    expect(j.expiresAt).toBeNull();
    expect(await getRepo().devices.get(ID)).not.toBeNull();
  });
});

describe('GET /api/device/status', () => {
  it('모르는 기기 -> none', async () => {
    const j = await (await status(`deviceId=${ID}`)).json();
    expect(j).toMatchObject({ status: 'none', expiresAt: null, renewalPending: false, notice: null });
    expect(typeof j.serverTime).toBe('string');
  });
  it('잘못된 deviceId -> 400', async () => {
    expect((await status('deviceId=zz')).status).toBe(400);
    expect((await status('')).status).toBe(400);
  });
  it('승인 후 active + ISO expiresAt + serverTime', async () => {
    await post(valid);
    const repo = getRepo();
    const row = (await repo.devices.get(ID))!;
    await repo.devices.upsert(approve(row, 'month', new Date()));
    const j = await (await status(`deviceId=${ID}`)).json();
    expect(j.status).toBe('active');
    expect(new Date(j.expiresAt).toISOString()).toBe(j.expiresAt);
    expect(j.serverTime).toBeTruthy();
  });
  it('lastSeenAt / appVersion 갱신', async () => {
    await post(valid);
    const repo = getRepo();
    const row = (await repo.devices.get(ID))!;
    await repo.devices.upsert({ ...row, lastSeenAt: null, appVersion: 1 });
    await status(`deviceId=${ID}&versionCode=9`);
    const after = (await repo.devices.get(ID))!;
    expect(after.appVersion).toBe(9);
    expect(after.lastSeenAt).toBeInstanceOf(Date);
  });
  it('versionCode 없으면 appVersion 유지', async () => {
    await post(valid);
    await status(`deviceId=${ID}`);
    const after = (await getRepo().devices.get(ID))!;
    expect(after.appVersion).toBe(5);
    expect(after.lastSeenAt).toBeInstanceOf(Date);
  });
  it('notice: 빈 문자열 -> null, 값 있으면 문자열', async () => {
    await getRepo().settings.set('notice', '점검 예정');
    expect((await (await status(`deviceId=${ID}`)).json()).notice).toBe('점검 예정');
    await getRepo().settings.set('notice', '');
    expect((await (await status(`deviceId=${ID}`)).json()).notice).toBeNull();
  });
  it('갱신 요청 중이면 renewalPending true', async () => {
    await post(valid);
    const repo = getRepo();
    const row = (await repo.devices.get(ID))!;
    await repo.devices.upsert({ ...approve(row, 'year', new Date()), pendingPlan: 'month' });
    expect((await (await status(`deviceId=${ID}`)).json()).renewalPending).toBe(true);
  });
});

describe('GET /api/config', () => {
  it('settings -> 응답 형식', async () => {
    const repo = getRepo();
    await repo.settings.set('price_day', '4900');
    await repo.settings.set('price_lifetime', '30000');
    await repo.settings.set('price_month', '3000');
    await repo.settings.set('price_year', 'abc');
    await repo.settings.set('bank_account', '국민 123');
    await repo.settings.set('openchat_url', 'https://open.kakao.com/o/x');
    const j = await (await configGet()).json();
    expect(j).toEqual({
      prices: { day: 4900, lifetime: 30000, month: 3000, year: 0 },
      bankAccount: '국민 123',
      openchatUrl: 'https://open.kakao.com/o/x',
      termsUrl: '/terms',
    });
  });
});

describe('GET /api/version', () => {
  it('릴리스 없으면 404', async () => {
    const res = await versionGet();
    expect(res.status).toBe(404);
    expect(await res.json()).toEqual({ error: 'no release' });
  });
  it('최신 릴리스 반환', async () => {
    await getRepo().releases.insert({ versionCode: 7, versionName: '1.7', blobUrl: 'https://b/x.apk', sizeBytes: 123, notes: '메모' });
    expect(await (await versionGet()).json()).toEqual({
      versionCode: 7, versionName: '1.7', url: 'https://b/x.apk', sizeBytes: 123, notes: '메모',
    });
  });
});
