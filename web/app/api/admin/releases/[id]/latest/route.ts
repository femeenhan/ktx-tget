import { getRepo } from '../../../../../../lib/repo';
import { requireAdmin } from '../../../../../admin/require-admin';

export async function POST(_req: Request, ctx: { params: Promise<{ id: string }> }): Promise<Response> {
  await requireAdmin();
  const id = Number((await ctx.params).id);
  const repo = getRepo();
  if (!Number.isInteger(id) || !(await repo.releases.list()).some((r) => r.id === id)) {
    return Response.json({ error: 'not found' }, { status: 404 });
  }
  await repo.releases.setLatest(id);
  return new Response(null, { status: 204 });
}
