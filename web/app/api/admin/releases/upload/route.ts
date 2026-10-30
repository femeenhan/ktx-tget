import { handleUpload, type HandleUploadBody } from '@vercel/blob/client';
import { requireAdmin } from '../../../../admin/require-admin';

export async function POST(request: Request): Promise<Response> {
  await requireAdmin();
  const body = (await request.json()) as HandleUploadBody;
  try {
    const json = await handleUpload({
      body,
      request,
      onBeforeGenerateToken: async () => ({
        allowedContentTypes: ['application/vnd.android.package-archive', 'application/octet-stream'],
        maximumSizeInBytes: 200 * 1024 * 1024,
        addRandomSuffix: true,
        tokenPayload: '',
      }),
      onUploadCompleted: async () => {},
    });
    return Response.json(json);
  } catch (e) {
    return Response.json({ error: (e as Error).message }, { status: 400 });
  }
}
