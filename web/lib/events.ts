import type { Repo } from './repo';

/**
 * 관리자 통계용 이벤트. 개인 식별 정보는 넣지 않는다(IP·쿠키·기기 ID 없음).
 * - view:     공개 페이지 방문. path=페이지 경로, ref=유입 출처 호스트(없으면 null). 탭 세션당 경로별 1회.
 * - download: APK 내려받기. path='web'(다운로드 페이지) | 'app'(앱의 새 버전 받기), ref=null.
 * - request:  활성화 요청. path='new' | 'renewal', ref=기간(plan).
 * - approve:  승인. path='new'(승인) | 'extend'(연장), ref=기간(plan).
 */
export type EventKind = 'view' | 'download' | 'request' | 'approve';

export interface EventRow {
  ts: Date;
  kind: EventKind;
  path: string | null;
  ref: string | null;
}

export const TRACKED_PATHS: readonly string[] = ['/', '/download', '/guide', '/terms', '/privacy'];
export const PATH_LABEL: Record<string, string> = {
  '/': '홈', '/download': '다운로드', '/guide': '안내', '/terms': '약관', '/privacy': '개인정보',
};

const DAY_MS = 24 * 3600 * 1000;
/** 이벤트 보관 기간. 지나면 cleanup이 지운다. */
export const EVENT_RETENTION_MS = 365 * DAY_MS;

/** 유입 출처를 호스트만 남긴다. 우리 사이트 안에서의 이동은 null. */
export function normalizeRef(referrer: string | null | undefined, ownHost: string): string | null {
  if (!referrer) return null;
  try {
    const host = new URL(referrer).hostname.toLowerCase();
    if (!host || host === ownHost.toLowerCase()) return null;
    return host.length > 100 ? host.slice(0, 100) : host;
  } catch {
    return null;
  }
}

/** 저장 실패가 요청 처리를 망치지 않게 삼킨다. */
export async function recordEvent(repo: Repo, e: Omit<EventRow, 'ts'> & { ts?: Date }): Promise<void> {
  try {
    await repo.events.insert({ ts: e.ts ?? new Date(), kind: e.kind, path: e.path, ref: e.ref });
  } catch (err) {
    console.error('event insert failed', err);
  }
}

const kstDate = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit' });
export const toKstDate = (d: Date): string => kstDate.format(d);

export interface DailyRow { date: string; views: number; downloads: number; requests: number; approvals: number }
export interface Summary {
  days: number;
  views: Record<string, number>;
  downloads: { web: number; app: number };
  requests: { new: number; renewal: number };
  approvals: { new: number; extend: number };
  refs: { ref: string; count: number }[];
  daily: DailyRow[];
}

/** [now - days, now] 범위의 이벤트를 깔때기·일별로 집계한다. daily는 최근 날짜부터, 이벤트 없는 날도 0으로 채운다. */
export function summarize(events: EventRow[], now: Date, days: number): Summary {
  const since = now.getTime() - days * DAY_MS;
  const s: Summary = {
    days,
    views: Object.fromEntries(TRACKED_PATHS.map((p) => [p, 0])),
    downloads: { web: 0, app: 0 },
    requests: { new: 0, renewal: 0 },
    approvals: { new: 0, extend: 0 },
    refs: [],
    daily: [],
  };
  const byDay = new Map<string, DailyRow>();
  for (let i = 0; i < days; i++) {
    const date = toKstDate(new Date(now.getTime() - i * DAY_MS));
    byDay.set(date, { date, views: 0, downloads: 0, requests: 0, approvals: 0 });
  }
  const refCount = new Map<string, number>();
  for (const e of events) {
    if (e.ts.getTime() < since || e.ts.getTime() > now.getTime()) continue;
    const day = byDay.get(toKstDate(e.ts));
    switch (e.kind) {
      case 'view':
        // 허용 목록 밖 경로는 집계하지 않는다(track 라우트도 거르지만 과거 데이터에 대비).
        if (e.path === null || !(e.path in s.views)) break;
        s.views[e.path] += 1;
        if (e.ref) refCount.set(e.ref, (refCount.get(e.ref) ?? 0) + 1);
        if (day) day.views += 1;
        break;
      case 'download':
        if (e.path === 'app') s.downloads.app += 1; else s.downloads.web += 1;
        if (day) day.downloads += 1;
        break;
      case 'request':
        if (e.path === 'renewal') s.requests.renewal += 1; else s.requests.new += 1;
        if (day) day.requests += 1;
        break;
      case 'approve':
        if (e.path === 'extend') s.approvals.extend += 1; else s.approvals.new += 1;
        if (day) day.approvals += 1;
        break;
    }
  }
  s.refs = [...refCount.entries()].map(([ref, count]) => ({ ref, count })).sort((a, b) => b.count - a.count).slice(0, 10);
  s.daily = [...byDay.values()];
  return s;
}

/** 전환율 표시용. 분모 0이면 '-'. */
export const pct = (num: number, den: number): string => (den > 0 ? `${Math.round((num / den) * 100)}%` : '-');
