import { getRepo } from '../../../lib/repo';
import { saveSettings } from './actions';

export const dynamic = 'force-dynamic';

const FIELDS: [string, string][] = [
  ['price_day', '1일(24시간) 가격(원)'],
  ['price_lifetime', '영구 가격(원)'],
  ['price_month', '1개월 가격(원)'],
  ['price_year', '1년 가격(원)'],
  ['bank_account', '입금 계좌'],
  ['openchat_url', '오픈채팅 링크'],
  ['biz_reg_no', '통신판매업 신고번호'],
];

export default async function SettingsPage({ searchParams }: { searchParams: Promise<{ saved?: string }> }) {
  const s = await getRepo().settings.all();
  const { saved } = await searchParams;
  return (
    <form action={saveSettings} className="settings-form">
      {saved === '1' && <p className="saved">저장되었습니다</p>}
      {FIELDS.map(([k, label]) => (
        <p key={k}>
          <label>{label}<br /><input name={k} defaultValue={s[k] ?? ''} size={40} /></label>
        </p>
      ))}
      <p>
        <label>공지<br /><textarea name="notice" defaultValue={s.notice ?? ''} rows={4} cols={50} /></label>
      </p>
      <button type="submit">저장</button>
    </form>
  );
}
