import { getRepo, type ReleaseRow } from '../../lib/repo';

export const CONTACT_EMAIL = 'bigseasons.dev@gmail.com';

/** DB 장애여도 공개 페이지는 떠야 하므로 실패하면 빈 설정(가격은 「문의」)으로 대신한다. */
export async function loadSettings(): Promise<Record<string, string>> {
  try {
    return await getRepo().settings.all();
  } catch (e) {
    console.error('loadSettings failed', e);
    return {};
  }
}

/** 「릴리스 없음」(release null, error false)과 DB 오류(error true)를 구분한다. */
export async function loadLatestRelease(): Promise<{ release: ReleaseRow | null; error: boolean }> {
  try {
    return { release: await getRepo().releases.latest(), error: false };
  } catch (e) {
    console.error('loadLatestRelease failed', e);
    return { release: null, error: true };
  }
}

/** http(s) 링크만 새 창으로 연다. mailto:는 그대로. */
export function externalProps(href: string): { target?: '_blank'; rel?: string } {
  return /^https?:\/\//.test(href) ? { target: '_blank', rel: 'noopener noreferrer' } : {};
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
