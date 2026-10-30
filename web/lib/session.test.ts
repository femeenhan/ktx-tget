import { describe, expect, it } from 'vitest';
import { signSession, verifySession } from './session';

const S = 'secret';
const DAY = 24 * 3600 * 1000;
const NOW = 1_800_000_000_000;

describe('session', () => {
  it('올바른 서명 통과', async () => {
    expect(await verifySession(S, await signSession(S, NOW), NOW)).toBe(true);
  });
  it('변조 실패', async () => {
    const t = await signSession(S, NOW);
    expect(await verifySession(S, t.slice(0, -1) + (t.endsWith('0') ? '1' : '0'), NOW)).toBe(false);
    expect(await verifySession(S, t.replace(String(NOW), String(NOW + 1)), NOW)).toBe(false);
    expect(await verifySession('other', t, NOW)).toBe(false);
  });
  it('31일 지나면 실패, 29일은 통과', async () => {
    expect(await verifySession(S, await signSession(S, NOW - 31 * DAY), NOW)).toBe(false);
    expect(await verifySession(S, await signSession(S, NOW - 29 * DAY), NOW)).toBe(true);
  });
  it('미래 발급(5분 초과) 실패', async () => {
    expect(await verifySession(S, await signSession(S, NOW + 10 * 60000), NOW)).toBe(false);
  });
  it('형식 오류 실패', async () => {
    for (const c of [undefined, '', 'abc', '.', '1.', '.abc', 'x.y.z'])
      expect(await verifySession(S, c, NOW)).toBe(false);
  });
});
