import { getRepo } from '../../lib/repo';

export const CONTACT_EMAIL = 'bigseasons.dev@gmail.com';

export async function loadSettings(): Promise<Record<string, string>> {
  return getRepo().settings.all();
}

/** 오픈채팅 링크가 비어 있으면 이메일로 대신한다. */
export function contactHref(settings: Record<string, string>): string {
  const url = settings.openchat_url?.trim();
  return url ? url : `mailto:${CONTACT_EMAIL}`;
}

/** 0이거나 숫자가 아니면 「문의」. */
export function formatPrice(raw: string | undefined): string {
  const n = Number(raw);
  if (!raw || !Number.isFinite(n) || n <= 0) return '문의';
  return `${n.toLocaleString('ko-KR')}원`;
}

export function formatMb(bytes: number): string {
  return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
}

/** KST yyyy-MM-dd */
export function formatKstDate(d: Date): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit' }).format(d);
}
