import type { Metadata } from 'next';
import Image from 'next/image';
import Link from 'next/link';
import { SiteShell } from './components/SiteShell';
import { contactHref, externalProps, formatPrice, loadSettings } from './components/site-data';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: { absolute: 'ktx-tget · 코레일톡 취소표 자동 예매 도우미' },
  description: '코레일톡 열차 목록을 지켜보다가 빈자리가 나면 좌석을 잡고 결제 화면까지 진행합니다. 결제는 직접 하세요.',
};

const SHOTS: [string, string][] = [
  ['/screens/01.png', '열차 목록 감시'],
  ['/screens/02.png', '취소표 감지 → 좌석 선택'],
  ['/screens/03.png', '결제 화면 도착 알림'],
  ['/screens/04.png', '앱 홈'],
];

export default async function Home() {
  const settings = await loadSettings();
  const contact = contactHref(settings);
  const prices: [string, string][] = [
    ['1일 (24시간)', formatPrice(settings.price_day)],
    ['1개월', formatPrice(settings.price_month)],
    ['1년', formatPrice(settings.price_year)],
    ['영구', formatPrice(settings.price_lifetime)],
  ];

  return (
    <SiteShell settings={settings}>
      <section className="hero" aria-labelledby="hero-title">
        <div className="wrap">
          <p className="status-chip">안드로이드 · 코레일톡 전용</p>
          <h1 id="hero-title">취소표가 뜨면, 예매 직전까지 알아서 갑니다.</h1>
          <p className="lede">
            코레일톡 열차 목록을 지켜보다가 빈자리가 나면 좌석을 잡고 결제 화면까지 진행합니다. 결제는 직접 하세요.
          </p>
          <div className="btn-row">
            <Link href="/download" className="btn btn-primary">APK 다운로드</Link>
            <a href={contact} className="btn btn-secondary" {...externalProps(contact)}>오픈채팅 문의</a>
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="how-title">
        <div className="wrap">
          <div className="section-head">
            <p className="eyebrow">동작</p>
            <h2 id="how-title">목록을 띄워 두고 시작만 누르면 됩니다</h2>
            <p className="muted">새로고침을 대신 반복하다가 취소표가 보이면 좌석을 누르고, 결제 화면에 닿으면 알림을 울리고 멈춥니다.</p>
          </div>
          <ul className="shots" aria-label="앱 동작 화면">
            {SHOTS.map(([src, caption], i) => (
              <li key={src}>
                <figure>
                  <Image src={src} alt={`${caption} 화면`} width={1080} height={2280} sizes="(min-width: 768px) 240px, 64vw" />
                  <figcaption><span className="num">{String(i + 1).padStart(2, '0')}</span>{caption}</figcaption>
                </figure>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="section" aria-labelledby="price-title">
        <div className="wrap pricing-layout">
          <div className="section-head">
            <p className="eyebrow">가격</p>
            <h2 id="price-title">기간을 골라 한 번 입금</h2>
            <p className="muted">기기 한 대에 라이선스 하나입니다. 기간이 끝나기 전에 앱에서 바로 연장할 수 있습니다.</p>
          </div>
          <div>
            <dl className="price-list">
              {prices.map(([label, price]) => (
                <div key={label}><dt>{label}</dt><dd>{price}</dd></div>
              ))}
            </dl>
            <p className="price-note muted small">계좌이체 후 앱에서 활성화 요청 → 입금 확인 뒤 승인</p>
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="notes-title">
        <div className="wrap">
          <div className="section-head">
            <p className="eyebrow">알아 둘 것</p>
            <h2 id="notes-title">설치 전에 읽어 주세요</h2>
          </div>
          <div className="grid-2">
            <div className="card">
              <h3>왜 스토어에 없나요</h3>
              <p className="muted">
                접근성 서비스로 화면을 읽어 동작하는 앱이라 Play 스토어에 올릴 수 없습니다. 그래서 APK 파일로 직접 설치합니다.
              </p>
              <p className="muted">
                앱은 코레일톡 화면만 읽고, 다른 정보는 수집하지 않습니다. 자세한 내용은{' '}
                <Link href="/privacy" className="text-link">개인정보 처리방침</Link>에 있습니다.
              </p>
            </div>
            <div className="card">
              <h3>주의</h3>
              <ul className="dot-list muted">
                <li>취소표가 나는 순간을 잡아 주는 도구일 뿐, <b>예매 성공을 보장하지 않습니다.</b> 취소표가 안 나오거나 다른 사람이 먼저 잡을 수 있고, 결제는 직접 해야 합니다.</li>
                <li>안드로이드 전용입니다. 아이폰(iOS)에서는 쓸 수 없습니다.</li>
                <li>코레일 이용약관은 자동화 프로그램 사용을 제한할 수 있습니다. 계정 제재 가능성은 낮지만 있으며, 이용자 본인이 판단하고 감수합니다.</li>
                <li>코레일톡이 비정상 접근을 경고하면 앱은 즉시 멈추고 10분 쉽니다. 제재나 코레일 정책 변경은 환불 사유가 아닙니다. <Link href="/guide#policy" className="text-link">자세히</Link></li>
                <li>코레일톡 화면이 바뀌어 앱이 멈추면 업데이트로 고쳐 이어갑니다. 라이선스 기간 안의 업데이트는 무료입니다.</li>
              </ul>
            </div>
          </div>
          <div className="btn-row">
            <Link href="/download" className="btn btn-primary">APK 다운로드</Link>
            <Link href="/guide" className="btn btn-secondary">사용 안내 보기</Link>
          </div>
        </div>
      </section>
    </SiteShell>
  );
}
