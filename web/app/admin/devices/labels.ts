import type { DeviceStatus, Plan } from '../../../lib/license';

export const PLAN_LABEL: Record<Plan, string> = { lifetime: '영구', month: '1개월', year: '1년' };
export const STATUS_LABEL: Record<DeviceStatus, string> = { pending: '대기', active: '활성', expired: '만료', revoked: '회수' };

const fmt = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
});

/** yyyy-MM-dd HH:mm (Asia/Seoul) */
export function formatKst(d: Date | null): string {
  if (!d) return '-';
  const p = Object.fromEntries(fmt.formatToParts(d).map((x) => [x.type, x.value]));
  return `${p.year}-${p.month}-${p.day} ${p.hour}:${p.minute}`;
}
