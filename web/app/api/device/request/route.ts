import { after } from 'next/server';
import { PLANS, applyRequest, type Plan } from '../../../../lib/license';
import { recordEvent } from '../../../../lib/events';
import { formatRequestMessage, sendTelegram } from '../../../../lib/notify';
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
  // 활성 중 요청은 연장(pendingPlan), 그 외는 신규 요청으로 센다.
  await recordEvent(repo, { ts: now, kind: 'request', path: row.pendingPlan !== null && row.status !== 'pending' ? 'renewal' : 'new', ref: body.plan as string });
  const settings = await repo.settings.all();
  const notify = () => sendTelegram(formatRequestMessage(row, settings, now));
  // 응답을 먼저 보내고 알림은 뒤에. 테스트처럼 요청 범위 밖이면 after가 던지므로 바로 실행한다.
  try {
    after(notify);
  } catch {
    void notify();
  }
  return Response.json(buildStatusResponse(row, now, normalizeNotice(settings.notice)));
}
