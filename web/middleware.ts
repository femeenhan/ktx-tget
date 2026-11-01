import { NextResponse, type NextRequest } from 'next/server';
import { isAuthenticated } from './lib/session';

/**
 * 2026-11-01 판매 중단. 공개 페이지·APK 내려받기·새 활성화 요청은 모두 404로 닫고,
 * 이미 승인된 기기의 상태 확인(/api/device/status, /api/config)과 관리자·크론만 남긴다.
 * 호스팅을 완전히 끄는 날까지의 임시 조치다.
 */
export const SALES_CLOSED = true;

export function isOpenWhileClosed(pathname: string): boolean {
  return (
    pathname === '/api/device/status' ||
    pathname === '/api/config' ||
    pathname.startsWith('/api/cron/') ||
    pathname === '/admin' ||
    pathname.startsWith('/admin/') ||
    pathname.startsWith('/api/admin/')
  );
}

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  if (SALES_CLOSED && !isOpenWhileClosed(pathname)) {
    return new NextResponse('Not Found', { status: 404, headers: { 'cache-control': 'no-store' } });
  }
  if (!pathname.startsWith('/admin') && !pathname.startsWith('/api/admin/')) return NextResponse.next();
  if (pathname === '/admin/login' || pathname === '/api/admin/login') return NextResponse.next();
  if (await isAuthenticated(req)) return NextResponse.next();
  if (pathname.startsWith('/api/')) {
    return NextResponse.json({ error: 'unauthorized' }, { status: 401 });
  }
  const url = req.nextUrl.clone();
  url.pathname = '/admin/login';
  url.search = `?next=${encodeURIComponent(pathname)}`;
  return NextResponse.redirect(url);
}

// _next 정적 자원만 제외하고 전부 미들웨어를 거친다(public/ 파일 포함).
export const config = { matcher: ['/((?!_next/static|_next/image).*)'] };
