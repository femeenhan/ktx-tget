import type { DeviceRow, Plan } from './license';
import { isTrialActive } from './license';

const PLAN_LABEL: Record<Plan, string> = { day: '1일', month: '1개월', year: '1년', lifetime: '영구' };
const PRICE_KEY: Record<Plan, string> = {
  day: 'price_day', month: 'price_month', year: 'price_year', lifetime: 'price_lifetime',
};
const ADMIN_URL = 'https://tget.bigseasons.co.kr/admin/devices';

const kst = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
});

/** 활성화·연장 요청을 판매자에게 알리는 텔레그램 본문. 순수 함수라 테스트한다. */
export function formatRequestMessage(row: DeviceRow, settings: Record<string, string>, now: Date): string {
  const renewal = row.pendingPlan !== null && row.status !== 'pending';
  const plan: Plan = renewal ? (row.pendingPlan as Plan) : row.requestedPlan;
  const price = Number(settings[PRICE_KEY[plan]] ?? 0);
  const priceText = price > 0 ? ` ${price.toLocaleString('ko-KR')}원` : '';
  const lines = [
    renewal ? '🔁 연장 요청' : '🔔 활성화 요청',
    `입금자명: ${row.depositor ?? '-'}`,
    `기간: ${PLAN_LABEL[plan]}${priceText}`,
    `기기: ${row.deviceId.slice(0, 6)}`,
  ];
  if (isTrialActive(row, now) && row.expiresAt) lines.push(`임시 이용 중 · ${kst.format(row.expiresAt)}까지`);
  lines.push(`승인: ${ADMIN_URL}`);
  return lines.join('\n');
}

/**
 * TELEGRAM_BOT_TOKEN·TELEGRAM_CHAT_ID가 있으면 텔레그램으로 보낸다. 없으면 조용히 건너뛴다.
 * 실패해도 던지지 않는다(요청 처리 자체는 이미 끝났으므로).
 */
export async function sendTelegram(text: string): Promise<boolean> {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  const chatId = process.env.TELEGRAM_CHAT_ID;
  if (!token || !chatId) return false;
  try {
    const res = await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ chat_id: chatId, text, disable_web_page_preview: true }),
      signal: AbortSignal.timeout(5000),
    });
    if (!res.ok) console.error('telegram sendMessage failed', res.status, await res.text().catch(() => ''));
    return res.ok;
  } catch (e) {
    console.error('telegram sendMessage error', e);
    return false;
  }
}
