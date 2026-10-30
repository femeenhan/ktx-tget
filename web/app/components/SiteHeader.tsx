import Link from 'next/link';
import { externalProps } from './site-data';

export function SiteHeader({ contact }: { contact: string }) {
  return (
    <header className="site-header">
      <div className="wrap site-header-inner">
        <Link href="/" className="wordmark">ktx-tget</Link>
        <nav aria-label="주 메뉴" className="site-nav">
          <Link href="/download">다운로드</Link>
          <Link href="/guide">사용 안내</Link>
          <a href={contact} {...externalProps(contact)}>문의</a>
        </nav>
      </div>
    </header>
  );
}
