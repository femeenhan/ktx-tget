'use client';

import Link from 'next/link';
import './components/site.css';

export default function Error({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div className="site">
      <main id="main">
        <div className="wrap wrap-narrow legal">
          <p className="eyebrow">ktx-tget</p>
          <h1>잠시 문제가 생겼습니다</h1>
          <p className="lede" style={{ marginTop: 14 }}>페이지를 불러오지 못했습니다. 잠시 뒤 다시 시도해 주세요.</p>
          <div className="btn-row">
            <button type="button" className="btn btn-primary" onClick={() => reset()}>다시 시도</button>
            <Link href="/" className="btn btn-secondary">처음으로</Link>
          </div>
        </div>
      </main>
    </div>
  );
}
