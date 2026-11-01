import { beforeEach, describe, expect, it } from 'vitest';
import { getRepo, resetRepoForTests } from '../../../lib/repo';
import { approve, TRIAL_MS } from '../../../lib/license';
import { formatRequestMessage } from '../../../lib/notify';
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
  it('정상 첫 요청 -> 임시 이용(active+trial), 소문자/trim 정규화', async () => {
    const before = Date.now();
    const res = await post({ ...valid, deviceId: `  ${ID.toUpperCase()} ` });
    expect(res.status).toBe(200);
    const j = await res.json();
    expect(j.status).toBe('active');
    expect(j.trial).toBe(true);
    expect(j.renewalPending).toBe(false);
    const until = new Date(j.expiresAt).getTime();
    expect(until).toBeGreaterThanOrEqual(before + TRIAL_MS);
    expect(until).toBeLessThan(before + TRIAL_MS + 60_000);
    const row = (await getRepo().devices.get(ID))!;
    expect(row.status).toBe('pending');
  });
  it('임시 이용이 끝나면 pending으로 보이고 expiresAt은 null', async () => {
    await post(valid);
    const repo = getRepo();
    const row = (await repo.devices.get(ID))!;
    await repo.devices.upsert({ ...row, expiresAt: new Date(Date.now() - 1000) });
    const j = await (await status(`deviceId=${ID}`)).json();
    expect(j).toMatchObject({ status: 'pending', trial: false, expiresAt: null });
  });
  it('임시 이용 중 승인하면 정식 기간으로 바뀜', async () => {
    await post({ ...valid, plan: 'day' });
    const repo = getRepo();
    const now = new Date();
    await repo.devices.upsert(approve((await repo.devices.get(ID))!, 'day', now));
    const j = await (await status(`deviceId=${ID}`)).json();
    expect(j.status).toBe('active');
    expect(j.trial).toBe(false);
    expect(new Date(j.expiresAt).getTime()).toBe(now.getTime() + 24 * 3600_000);
  });
  it('텔레그램 본문: 활성화 요청에 입금자명·기간·가격·임시 이용 종료 시각', async () => {
    await getRepo().settings.set('price_day', '4900');
    await post({ ...valid, plan: 'day' });
    const row = (await getRepo().devices.get(ID))!;
    const msg = formatRequestMessage(row, await getRepo().settings.all(), new Date());
    expect(msg).toContain('활성화 요청');
    expect(msg).toContain('홍길동');
    expect(msg).toContain('1일 4,900원');
    expect(msg).toContain(ID.slice(0, 6));
    expect(msg).toContain('임시 이용 중');
    expect(msg).toContain('/admin/devices');
  });
  it('텔레그램 본문: 연장 요청은 pendingPlan 기준', async () => {
    await post(valid);
    const repo = getRepo();
    const active = approve((await repo.devices.get(ID))!, 'month', new Date());
    await repo.devices.upsert(active);
    await post({ ...valid, plan: 'year' });
    const row = (await repo.devices.get(ID))!;
    const msg = formatRequestMessage(row, {}, new Date());
    expect(msg).toContain('연장 요청');
    expect(msg).toContain('기간: 1년');
    expect(msg).not.toContain('임시 이용');
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
