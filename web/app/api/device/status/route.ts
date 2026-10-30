import { buildStatusResponse, normalizeNotice, parseDeviceId } from '../../../../lib/device-response';
import { getRepo } from '../../../../lib/repo';

export const dynamic = 'force-dynamic';

export async function GET(req: Request): Promise<Response> {
  const q = new URL(req.url).searchParams;
  const deviceId = parseDeviceId(q.get('deviceId'));
  if (!deviceId) return Response.json({ error: 'invalid deviceId' }, { status: 400 });
  const vc = Number(q.get('versionCode'));

  const repo = getRepo();
  const now = new Date();
  let row = await repo.devices.get(deviceId);
  if (row) {
    row = { ...row, lastSeenAt: now, appVersion: Number.isInteger(vc) && vc >= 1 ? vc : row.appVersion };
    await repo.devices.upsert(row);
  }
  const notice = normalizeNotice((await repo.settings.all()).notice);
  return Response.json(buildStatusResponse(row, now, notice));
}
