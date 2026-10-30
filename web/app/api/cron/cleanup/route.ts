import { runCleanup } from '../../../../lib/cleanup';
import { getRepo } from '../../../../lib/repo';

export const dynamic = 'force-dynamic';

export async function GET(req: Request) {
  const secret = process.env.CRON_SECRET;
  if (!secret) return Response.json({ error: 'CRON_SECRET not set' }, { status: 500 });
  if (req.headers.get('authorization') !== `Bearer ${secret}`) {
    return Response.json({ error: 'unauthorized' }, { status: 401 });
  }
  return Response.json({ cleared: await runCleanup(getRepo(), new Date()) });
}
