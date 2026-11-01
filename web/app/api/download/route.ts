import { recordEvent } from '../../../lib/events';
import { getRepo } from '../../../lib/repo';

export const dynamic = 'force-dynamic';

/** 최신 APK로 302. 내려받기 횟수를 세려고 Blob 주소를 직접 노출하지 않는다. ?src=app이면 앱의 「새 버전 받기」. */
export async function GET(req: Request): Promise<Response> {
  const repo = getRepo();
  const r = await repo.releases.latest();
  if (!r) return Response.json({ error: 'no release' }, { status: 404 });
  const src = new URL(req.url).searchParams.get('src') === 'app' ? 'app' : 'web';
  await recordEvent(repo, { kind: 'download', path: src, ref: null });
  return new Response(null, { status: 302, headers: { location: r.blobUrl, 'cache-control': 'no-store' } });
}
