import Link from 'next/link';

export function SiteHeader({ contact }: { contact: string }) {
  return (
    <header className="site-header">
      <div className="wrap site-header-inner">
        <Link href="/" className="wordmark">ktx-tget</Link>
        <nav aria-label="주 메뉴" className="site-nav">
          <Link href="/download">다운로드</Link>
          <Link href="/guide">사용 안내</Link>
          <a href={contact} target="_blank" rel="noopener noreferrer">문의</a>
        </nav>
      </div>
    </header>
  );
}
