import Link from 'next/link';
import { CONTACT_EMAIL, externalProps } from './site-data';

export function Footer({ settings }: { settings: Record<string, string> }) {
  const mailOrder = settings.biz_reg_no?.trim() || '신고 준비 중';
  const openchat = settings.openchat_url?.trim();
  return (
    <footer className="site-footer">
      <div className="wrap">
        <nav aria-label="약관과 문의" className="footer-links">
          <Link href="/terms">이용약관</Link>
          <Link href="/privacy">개인정보 처리방침</Link>
          {openchat && <a href={openchat} {...externalProps(openchat)}>오픈채팅</a>}
          <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>
        </nav>
        <dl className="biz">
          <div><dt>상호</dt><dd>빅시즌스</dd></div>
          <div><dt>대표</dt><dd>한철민</dd></div>
          <div><dt>사업자등록번호</dt><dd className="mono nowrap">674-66-00675</dd></div>
          <div><dt>통신판매업 신고번호</dt><dd>{mailOrder}</dd></div>
          <div className="biz-wide"><dt>사업장 소재지</dt><dd>서울특별시 은평구 통일로 796</dd></div>
        </dl>
      </div>
    </footer>
  );
}
