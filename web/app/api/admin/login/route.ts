import { SESSION_COOKIE, SESSION_MAX_AGE_SEC, safeEqual, signSession } from '../../../../lib/session';

export async function POST(req: Request) {
  const expected = process.env.ADMIN_PASSWORD;
  const secret = process.env.SESSION_SECRET;
  if (!expected) return Response.json({ error: 'ADMIN_PASSWORD not set' }, { status: 500 });
  if (!secret) return Response.json({ error: 'SESSION_SECRET not set' }, { status: 500 });
  let password: unknown;
  try {
    password = (await req.json())?.password;
  } catch {
    password = undefined;
  }
  if (typeof password !== 'string' || !safeEqual(password, expected)) {
    return Response.json({ error: 'invalid password' }, { status: 401 });
  }
  const token = await signSession(secret, Date.now());
  const secure = process.env.NODE_ENV === 'production' ? '; Secure' : '';
  return new Response(null, {
    status: 204,
    headers: {
      'Set-Cookie': `${SESSION_COOKIE}=${token}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${SESSION_MAX_AGE_SEC}${secure}`,
    },
  });
}
