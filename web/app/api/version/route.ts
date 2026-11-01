import { getRepo } from '../../../lib/repo';

export const dynamic = 'force-dynamic';

export async function GET(req: Request): Promise<Response> {
  const r = await getRepo().releases.latest();
  if (!r) return Response.json({ error: 'no release' }, { status: 404 });
  // 앱의 「새 버전 받기」도 /api/download를 거치게 해 내려받기 수에 잡히게 한다(최신 Blob으로 302).
  return Response.json({
    versionCode: r.versionCode,
    versionName: r.versionName,
    url: `${new URL(req.url).origin}/api/download?src=app`,
    sizeBytes: r.sizeBytes,
    notes: r.notes,
  });
}
