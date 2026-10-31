import { PLANS, applyRequest, type Plan } from '../../../../lib/license';
import { buildStatusResponse, normalizeNotice, parseDeviceId } from '../../../../lib/device-response';
import { getRepo } from '../../../../lib/repo';

export const dynamic = 'force-dynamic';

const bad = (error: string) => Response.json({ error }, { status: 400 });

export async function POST(req: Request): Promise<Response> {
  let body: Record<string, unknown>;
  try {
    const parsed: unknown = await req.json();
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) return bad('invalid body');
    body = parsed as Record<string, unknown>;
  } catch {
    return bad('invalid json');
  }
  const deviceId = parseDeviceId(body.deviceId);
  if (!deviceId) return bad('invalid deviceId');
  const depositor = typeof body.depositor === 'string' ? body.depositor.trim() : '';
  if (depositor.length < 1 || depositor.length > 40) return bad('invalid depositor');
  if (!PLANS.includes(body.plan as Plan)) return bad('invalid plan');
  const versionCode = body.versionCode;
  if (typeof versionCode !== 'number' || !Number.isInteger(versionCode) || versionCode < 1) return bad('invalid versionCode');
  if (body.agreed !== true) return bad('agreement required');

  const repo = getRepo();
  const now = new Date();
  // 드문 호출이고 전체 행을 써야 하므로 get + applyRequest + upsert 유지.
  const row = applyRequest(await repo.devices.get(deviceId), {
    deviceId, depositor, plan: body.plan as Plan, versionCode, now,
  });
  await repo.devices.upsert(row);
  const notice = normalizeNotice((await repo.settings.all()).notice);
  return Response.json(buildStatusResponse(row, now, notice));
}
