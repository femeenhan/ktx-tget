import { beforeEach, describe, expect, it } from 'vitest';
import { POST as login } from './route';
import { POST as logout } from '../logout/route';

beforeEach(() => {
  process.env.ADMIN_PASSWORD = 'pw-1234';
  process.env.SESSION_SECRET = 'sec';
});

const req = (body: unknown) =>
  new Request('http://x/api/admin/login', { method: 'POST', body: JSON.stringify(body) });

describe('POST /api/admin/login', () => {
  it('틀린 비밀번호 -> 401', async () => {
    expect((await login(req({ password: 'nope' }))).status).toBe(401);
    expect((await login(req({}))).status).toBe(401);
    expect((await login(new Request('http://x', { method: 'POST' }))).status).toBe(401);
  });
  it('맞으면 204 + Set-Cookie', async () => {
    const res = await login(req({ password: 'pw-1234' }));
    expect(res.status).toBe(204);
    const c = res.headers.get('set-cookie')!;
    expect(c).toContain('tget_admin=');
    expect(c).toContain('HttpOnly');
    expect(c).toContain('SameSite=Lax');
    expect(c).toContain('Max-Age=2592000');
  });
  it('ADMIN_PASSWORD 미설정 -> 500', async () => {
    delete process.env.ADMIN_PASSWORD;
    expect((await login(req({ password: 'x' }))).status).toBe(500);
  });
  it('logout -> 204, 쿠키 만료', async () => {
    const res = await logout();
    expect(res.status).toBe(204);
    expect(res.headers.get('set-cookie')).toContain('Max-Age=0');
  });
});
