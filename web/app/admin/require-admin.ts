import { cookies } from 'next/headers';
import { SESSION_COOKIE, verifySession } from '../../lib/session';

/** 서버 액션용 방어선. middleware가 이미 막지만 액션 단독 호출에 대비한다. */
export async function requireAdmin(): Promise<void> {
  const secret = process.env.SESSION_SECRET;
  const cookie = (await cookies()).get(SESSION_COOKIE)?.value;
  if (!secret || !(await verifySession(secret, cookie, Date.now()))) throw new Error('unauthorized');
}
