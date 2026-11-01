import { describe, expect, it } from 'vitest';
import { NextRequest } from 'next/server';
import { isOpenWhileClosed, middleware } from './middleware';

const req = (path: string, method = 'GET') => new NextRequest(`https://tget.example${path}`, { method });

describe('판매 중단 미들웨어', () => {
  it.each(['/', '/download', '/guide', '/terms', '/privacy', '/api/download?src=web', '/api/version', '/api/track', '/screens/01.png'])(
    '%s 는 404',
    async (p) => {
      const res = await middleware(req(p));
      expect(res.status).toBe(404);
    },
  );
  it('새 활성화 요청(POST /api/device/request)도 404', async () => {
    expect((await middleware(req('/api/device/request', 'POST'))).status).toBe(404);
  });
  it.each(['/api/device/status?deviceId=abc', '/api/config', '/api/cron/cleanup'])('%s 는 통과', async (p) => {
    const res = await middleware(req(p));
    expect(res.status).toBe(200);
    expect(res.headers.get('x-middleware-next')).toBe('1');
  });
  it('관리자 페이지는 남고, 미로그인은 로그인으로 보낸다', async () => {
    expect(isOpenWhileClosed('/admin/devices')).toBe(true);
    const res = await middleware(req('/admin/devices'));
    expect(res.status).toBe(307);
    expect(res.headers.get('location')).toContain('/admin/login?next=%2Fadmin%2Fdevices');
    expect((await middleware(req('/api/admin/releases'))).status).toBe(401);
  });
});
