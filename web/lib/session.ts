// Edge(middleware)에서도 돌아야 하므로 node:crypto 대신 Web Crypto 사용.
export const SESSION_COOKIE = 'tget_admin';
export const SESSION_MAX_AGE_SEC = 30 * 24 * 3600;
const MAX_AGE_MS = SESSION_MAX_AGE_SEC * 1000;
const FUTURE_SKEW_MS = 5 * 60 * 1000;

async function hmacHex(secret: string, msg: string): Promise<string> {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey('raw', enc.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  const sig = await crypto.subtle.sign('HMAC', key, enc.encode(msg));
  return Array.from(new Uint8Array(sig), (b) => b.toString(16).padStart(2, '0')).join('');
}

export function safeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let d = 0;
  for (let i = 0; i < a.length; i++) d |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return d === 0;
}

export async function signSession(secret: string, issuedAtMs: number): Promise<string> {
  return `${issuedAtMs}.${await hmacHex(secret, String(issuedAtMs))}`;
}

export async function verifySession(secret: string, cookie: string | undefined, nowMs: number): Promise<boolean> {
  if (!cookie) return false;
  const parts = cookie.split('.');
  if (parts.length !== 2 || !/^\d+$/.test(parts[0]) || !/^[0-9a-f]+$/.test(parts[1])) return false;
  const issuedAt = Number(parts[0]);
  if (!safeEqual(parts[1], await hmacHex(secret, parts[0]))) return false;
  if (nowMs - issuedAt > MAX_AGE_MS) return false;
  if (issuedAt - nowMs > FUTURE_SKEW_MS) return false;
  return true;
}

export async function isAuthenticated(req: Request): Promise<boolean> {
  const secret = process.env.SESSION_SECRET;
  if (!secret) return false;
  const header = req.headers.get('cookie') ?? '';
  const m = header.split(/;\s*/).find((p) => p.startsWith(`${SESSION_COOKIE}=`));
  if (!m) return false;
  return verifySession(secret, m.slice(SESSION_COOKIE.length + 1), Date.now());
}
