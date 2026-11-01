import { describe, expect, it } from 'vitest';
import { normalizeRef, pct, summarize, toKstDate, type EventRow } from './events';

const NOW = new Date('2026-11-01T06:00:00Z'); // KST 15:00
const h = (hours: number) => new Date(NOW.getTime() - hours * 3600_000);
const ev = (kind: EventRow['kind'], path: string | null, ref: string | null, ts: Date): EventRow => ({ ts, kind, path, ref });

describe('normalizeRef', () => {
  it('호스트만 남기고 자기 사이트·빈 값·깨진 값은 null', () => {
    expect(normalizeRef('https://www.google.com/search?q=ktx', 'tget.bigseasons.co.kr')).toBe('www.google.com');
    expect(normalizeRef('https://tget.bigseasons.co.kr/download', 'tget.bigseasons.co.kr')).toBeNull();
    expect(normalizeRef('', 'x')).toBeNull();
    expect(normalizeRef(null, 'x')).toBeNull();
    expect(normalizeRef('not a url', 'x')).toBeNull();
  });
});

describe('summarize', () => {
  const events: EventRow[] = [
    ev('view', '/', 'www.google.com', h(1)),
    ev('view', '/', null, h(2)),
    ev('view', '/download', 'www.google.com', h(1)),
    ev('view', '/admin', null, h(1)), // 집계 대상 아님(경로 허용 목록 밖) — 그래도 깨지지 않는다
    ev('download', 'web', null, h(1)),
    ev('download', 'app', null, h(30)), // 어제(KST)
    ev('request', 'new', 'day', h(1)),
    ev('request', 'renewal', 'month', h(1)),
    ev('approve', 'new', 'day', h(0.5)),
    ev('approve', 'extend', 'month', h(0.5)),
    ev('view', '/', null, h(24 * 40)), // 기간 밖
  ];
  it('깔때기 숫자', () => {
    const s = summarize(events, NOW, 30);
    expect(s.views['/']).toBe(2);
    expect(s.views['/download']).toBe(1);
    expect(s.downloads).toEqual({ web: 1, app: 1 });
    expect(s.requests).toEqual({ new: 1, renewal: 1 });
    expect(s.approvals).toEqual({ new: 1, extend: 1 });
    expect(s.refs).toEqual([{ ref: 'www.google.com', count: 2 }]);
  });
  it('일별은 최근 날짜부터 days개, 빈 날은 0', () => {
    const s = summarize(events, NOW, 7);
    expect(s.daily).toHaveLength(7);
    expect(s.daily[0]).toEqual({ date: '2026-11-01', views: 3, downloads: 1, requests: 2, approvals: 2 });
    expect(s.daily[1]).toEqual({ date: '2026-10-31', views: 0, downloads: 1, requests: 0, approvals: 0 });
    expect(s.daily[6].date).toBe('2026-10-26');
  });
  it('기간 밖 이벤트는 제외', () => {
    expect(summarize(events, NOW, 7).views['/']).toBe(2);
    expect(summarize(events, NOW, 90).views['/']).toBe(3);
  });
  it('KST 날짜와 전환율 표기', () => {
    expect(toKstDate(new Date('2026-11-01T15:30:00Z'))).toBe('2026-11-02');
    expect(pct(1, 4)).toBe('25%');
    expect(pct(1, 0)).toBe('-');
  });
});
