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
  const row = await repo.devices.get(deviceId);
  // 전체 행 upsert 대신 touch: 관리자 승인/폐기와의 경합으로 stale 행이 덮어쓰이는 것을 막는다.
  if (row) await repo.devices.touch(deviceId, now, Number.isInteger(vc) && vc >= 1 ? vc : null);
  const notice = normalizeNotice((await repo.settings.all()).notice);
  return Response.json(buildStatusResponse(row, now, notice));
}
