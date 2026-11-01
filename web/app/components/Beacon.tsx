'use client';
import { usePathname } from 'next/navigation';
import { useEffect } from 'react';

/**
 * 공개 페이지 방문을 한 번 센다. 같은 탭에서 같은 경로를 다시 열면 세지 않는다(sessionStorage).
 * 개인 정보는 보내지 않는다: 경로와 document.referrer뿐이고, 서버는 referrer를 호스트만 남긴다.
 */
export function Beacon() {
  const pathname = usePathname();
  useEffect(() => {
    if (!pathname) return;
    const key = `tget.v:${pathname}`;
    try {
      if (sessionStorage.getItem(key)) return;
      sessionStorage.setItem(key, '1');
    } catch {
      // 저장소를 못 쓰면 매번 세는 쪽을 택한다.
    }
    const body = JSON.stringify({ path: pathname, ref: document.referrer || null });
    try {
      if (!navigator.sendBeacon?.('/api/track', body)) {
        void fetch('/api/track', { method: 'POST', body, keepalive: true }).catch(() => undefined);
      }
    } catch {
      // 집계는 실패해도 된다.
    }
  }, [pathname]);
  return null;
}
