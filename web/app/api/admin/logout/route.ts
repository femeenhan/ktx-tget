import { SESSION_COOKIE } from '../../../../lib/session';

export async function POST() {
  const secure = process.env.NODE_ENV === 'production' ? '; Secure' : '';
  return new Response(null, {
    status: 204,
    headers: { 'Set-Cookie': `${SESSION_COOKIE}=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0${secure}` },
  });
}
