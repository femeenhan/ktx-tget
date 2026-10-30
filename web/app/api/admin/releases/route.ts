import { getRepo } from '../../../../lib/repo';
import { requireAdmin } from '../../../admin/require-admin';

export const dynamic = 'force-dynamic';

export async function GET(): Promise<Response> {
  await requireAdmin();
  return Response.json(await getRepo().releases.list());
}

export async function POST(request: Request): Promise<Response> {
  await requireAdmin();
  const b = (await request.json().catch(() => null)) as Record<string, unknown> | null;
  const bad = () => Response.json({ error: 'invalid body' }, { status: 400 });
  if (!b || typeof b !== 'object') return bad();
  const { versionCode, versionName, blobUrl, sizeBytes, notes } = b;
  if (!Number.isInteger(versionCode) || (versionCode as number) < 1) return bad();
  if (typeof versionName !== 'string' || versionName.trim() === '' || versionName.length > 40) return bad();
  if (typeof blobUrl !== 'string' || !blobUrl.startsWith('https://')) return bad();
  if (!Number.isInteger(sizeBytes) || (sizeBytes as number) < 1) return bad();
  if (notes !== undefined && notes !== null && typeof notes !== 'string') return bad();
  try {
    const row = await getRepo().releases.insert({
      versionCode: versionCode as number,
      versionName: versionName.trim(),
      blobUrl,
      sizeBytes: sizeBytes as number,
      notes: typeof notes === 'string' && notes.trim() !== '' ? notes.trim() : null,
    });
    return Response.json(row, { status: 201 });
  } catch (e) {
    if ((e as { code?: string }).code === 'DUPLICATE_VERSION') return Response.json({ error: 'duplicate version' }, { status: 409 });
    throw e;
  }
}
