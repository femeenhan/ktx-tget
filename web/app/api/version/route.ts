import { getRepo } from '../../../lib/repo';

export const dynamic = 'force-dynamic';

export async function GET(): Promise<Response> {
  const r = await getRepo().releases.latest();
  if (!r) return Response.json({ error: 'no release' }, { status: 404 });
  return Response.json({
    versionCode: r.versionCode,
    versionName: r.versionName,
    url: r.blobUrl,
    sizeBytes: r.sizeBytes,
    notes: r.notes,
  });
}
