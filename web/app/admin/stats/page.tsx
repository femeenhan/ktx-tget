import Link from 'next/link';
import { PATH_LABEL, TRACKED_PATHS, pct, summarize } from '../../../lib/events';
import { getRepo } from '../../../lib/repo';

export const dynamic = 'force-dynamic';

const PERIODS = [7, 30, 90] as const;

export default async function StatsPage({ searchParams }: { searchParams: Promise<{ days?: string }> }) {
  const { days: raw } = await searchParams;
  const days = (PERIODS as readonly number[]).includes(Number(raw)) ? Number(raw) : 30;
  const now = new Date();
  const events = await getRepo().events.list(new Date(now.getTime() - days * 24 * 3600 * 1000));
  const s = summarize(events, now, days);
  const home = s.views['/'] ?? 0;
  const dl = s.views['/download'] ?? 0;
  const downloads = s.downloads.web + s.downloads.app;

  return (
    <>
      <p className="stats-period">
        기간:{' '}
        {PERIODS.map((d) => (
          <Link key={d} href={`/admin/stats?days=${d}`} aria-current={d === days ? 'true' : undefined}>최근 {d}일</Link>
        ))}
      </p>
      <p className="stats-note">
        방문은 브라우저가 보내는 집계라 자바스크립트를 끈 방문자와 대부분의 봇은 빠집니다. 같은 탭에서 같은 페이지를 다시 열면 세지 않습니다. 모두 한국 시간 기준입니다.
      </p>

      <div className="stats-grid">
        <table>
          <caption>깔때기 (최근 {days}일)</caption>
          <tbody>
            <tr><td>홈 방문</td><td className="num">{home}</td><td /></tr>
            <tr><td>다운로드 페이지 방문</td><td className="num">{dl}</td><td className="num">홈 대비 {pct(dl, home)}</td></tr>
            <tr><td>APK 내려받기 (웹)</td><td className="num">{s.downloads.web}</td><td className="num">페이지 대비 {pct(s.downloads.web, dl)}</td></tr>
            <tr><td>활성화 요청 (신규)</td><td className="num">{s.requests.new}</td><td className="num">내려받기 대비 {pct(s.requests.new, s.downloads.web)}</td></tr>
            <tr><td>승인 (신규)</td><td className="num">{s.approvals.new}</td><td className="num">요청 대비 {pct(s.approvals.new, s.requests.new)}</td></tr>
            <tr><td>연장 요청 / 연장 승인</td><td className="num">{s.requests.renewal} / {s.approvals.extend}</td><td /></tr>
            <tr><td>앱에서 새 버전 받기</td><td className="num">{s.downloads.app}</td><td /></tr>
          </tbody>
        </table>

        <table>
          <caption>페이지별 방문</caption>
          <tbody>
            {TRACKED_PATHS.map((p) => (
              <tr key={p}><td>{PATH_LABEL[p] ?? p}</td><td className="num">{s.views[p] ?? 0}</td></tr>
            ))}
          </tbody>
        </table>

        <table>
          <caption>유입 출처 (우리 사이트 안 이동 제외)</caption>
          <tbody>
            {s.refs.length === 0 && <tr><td>직접 접속·앱 안 링크만 있음</td><td /></tr>}
            {s.refs.map((r) => <tr key={r.ref}><td>{r.ref}</td><td className="num">{r.count}</td></tr>)}
          </tbody>
        </table>
      </div>

      <table>
        <caption style={{ textAlign: 'left', fontWeight: 'bold', padding: '0 0 6px' }}>일별 (내려받기는 웹+앱, 요청·승인은 신규+연장)</caption>
        <thead><tr><th>날짜</th><th className="num">방문</th><th className="num">내려받기</th><th className="num">요청</th><th className="num">승인</th></tr></thead>
        <tbody>
          {s.daily.map((d) => (
            <tr key={d.date}>
              <td>{d.date}</td><td className="num">{d.views}</td><td className="num">{d.downloads}</td>
              <td className="num">{d.requests}</td><td className="num">{d.approvals}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="stats-note" style={{ marginTop: 12 }}>총 {downloads}회 내려받기 · 집계는 1년 뒤 자동 삭제됩니다.</p>
    </>
  );
}
