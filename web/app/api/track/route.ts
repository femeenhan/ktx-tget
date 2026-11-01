import { TRACKED_PATHS, normalizeRef, recordEvent } from '../../../lib/events';
import { getRepo } from '../../../lib/repo';

export const dynamic = 'force-dynamic';

const NO_CONTENT = () => new Response(null, { status: 204 });

/** 페이지 방문 집계. 어떤 입력이든 204로 끝낸다(브라우저 beacon은 응답을 안 본다). */
export async function POST(req: Request): Promise<Response> {
  let body: { path?: unknown; ref?: unknown };
  try {
    body = JSON.parse(await req.text()) as typeof body;
  } catch {
    return NO_CONTENT();
  }
  const path = typeof body.path === 'string' ? body.path : '';
  if (!TRACKED_PATHS.includes(path)) return NO_CONTENT();
  const ownHost = new URL(req.url).hostname;
  const ref = normalizeRef(typeof body.ref === 'string' ? body.ref : null, ownHost);
  await recordEvent(getRepo(), { kind: 'view', path, ref });
  return NO_CONTENT();
}
