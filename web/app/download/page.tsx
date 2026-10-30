import type { Metadata } from 'next';
import Link from 'next/link';
import { SiteShell } from '../components/SiteShell';
import { contactHref, externalProps, formatKstDate, formatMb, loadLatestRelease, loadSettings } from '../components/site-data';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: '다운로드',
  description: 'ktx-tget 최신 APK를 받고 세 단계로 설치합니다.',
};

const svgProps = { viewBox: '0 0 240 140', fill: 'none', stroke: 'currentColor', strokeWidth: 1.5, 'aria-hidden': true } as const;

/** ① 보호 기능 끄기: 방패 + 꺼진 스위치 */
function ProtectOff() {
  return (
    <svg {...svgProps}>
      <path d="M70 26 L104 14 L138 26 V64 C138 90 122 108 104 118 C86 108 70 90 70 64 Z" />
      <rect x="152" y="58" width="52" height="28" rx="14" stroke="#9AA095" />
      <circle cx="166" cy="72" r="9" stroke="#9AA095" />
      <text x="178" y="108" fontSize="12" fill="#5F655B" stroke="none" textAnchor="middle" fontFamily="sans-serif">끔</text>
    </svg>
  );
}

/** ② 출처 허용: 설정 목록 + 켜진 스위치 */
function AllowSource() {
  return (
    <svg {...svgProps}>
      <rect x="28" y="14" width="184" height="34" rx="17" stroke="#9AA095" />
      <rect x="44" y="27" width="70" height="8" rx="4" fill="#9AA095" stroke="none" />
      <rect x="28" y="56" width="184" height="34" rx="17" />
      <rect x="44" y="69" width="88" height="8" rx="4" fill="currentColor" stroke="none" />
      <rect x="158" y="61" width="44" height="24" rx="12" fill="currentColor" />
      <circle cx="190" cy="73" r="8" fill="#E6EBE2" stroke="none" />
      <rect x="28" y="98" width="184" height="34" rx="17" stroke="#9AA095" />
      <rect x="44" y="111" width="56" height="8" rx="4" fill="#9AA095" stroke="none" />
    </svg>
  );
}

/** ③ 설치: 휴대폰 + 설치 버튼 */
function Install() {
  return (
    <svg {...svgProps}>
      <rect x="84" y="6" width="72" height="128" rx="14" />
      <rect x="104" y="28" width="32" height="32" rx="9" stroke="#9AA095" />
      <path d="M120 36 V52 M113 45 L120 52 L127 45" />
      <rect x="98" y="70" width="44" height="6" rx="3" fill="#9AA095" stroke="none" />
      <rect x="94" y="100" width="52" height="20" rx="10" fill="currentColor" />
      <text x="120" y="114" fontSize="10" fill="#E6EBE2" stroke="none" textAnchor="middle" fontFamily="sans-serif">설치</text>
    </svg>
  );
}

export default async function DownloadPage() {
  const [settings, { release, error }] = await Promise.all([loadSettings(), loadLatestRelease()]);
  const contact = contactHref(settings);

  return (
    <SiteShell settings={settings}>
      <section className="hero" aria-labelledby="dl-title">
        <div className="wrap wrap-narrow">
          <p className="eyebrow">다운로드</p>
          <h1 id="dl-title">APK 받고, 세 단계로 설치</h1>
          <p className="lede">스토어 앱이 아니라서 처음 한 번만 휴대폰 설정을 바꿔야 합니다. 5분이면 끝납니다.</p>

          <div className="release" style={{ marginTop: 28 }}>
            {release ? (
              <>
                <a href={release.blobUrl} className="btn btn-primary btn-block" download={`ktx-tget-${release.versionName}.apk`}>
                  ktx-tget {release.versionName} 다운로드
                </a>
                <p className="release-meta">
                  <span className="mono">{formatMb(release.sizeBytes)}</span>
                  <span><span className="mono">{formatKstDate(release.uploadedAt)}</span> 올림</span>
                  <span>안드로이드 전용</span>
                </p>
              </>
            ) : (
              <>
                <span className="btn btn-disabled btn-block" aria-disabled="true">{error ? '확인할 수 없음' : '준비 중'}</span>
                <p className="release-meta" role={error ? 'status' : undefined}>
                  <span>{error ? '지금은 확인할 수 없습니다. 잠시 뒤 다시 시도해 주세요.' : '첫 버전을 올리는 중입니다. 잠시 뒤 다시 와 주세요.'}</span>
                </p>
              </>
            )}
          </div>
        </div>
      </section>

      <section className="section" aria-labelledby="steps-title">
        <div className="wrap">
          <div className="section-head">
            <p className="eyebrow">설치 순서</p>
            <h2 id="steps-title">받은 파일을 열기 전에</h2>
          </div>
          <ol className="steps">
            <li className="step">
              <ProtectOff />
              <p className="step-num">01</p>
              <h3>보호 기능 잠시 끄기</h3>
              <p><b>Play 스토어 → 프로필 → Play 프로텍트 → ⚙</b>에서 앱 검사를 끕니다.</p>
              <p>갤럭시는 <b>설정 → 보안 및 개인정보 보호 → Auto Blocker</b>도 끕니다. 설치 뒤 다시 켜도 됩니다.</p>
            </li>
            <li className="step">
              <AllowSource />
              <p className="step-num">02</p>
              <h3>출처를 알 수 없는 앱 허용</h3>
              <p><b>설정 → 애플리케이션 → ⋮ → 특수 액세스 → 출처를 알 수 없는 앱 설치</b></p>
              <p><b>내 파일</b>(또는 쓰는 브라우저)을 골라 <b>이 출처 허용</b>을 켭니다.</p>
            </li>
            <li className="step">
              <Install />
              <p className="step-num">03</p>
              <h3>설치</h3>
              <p><b>내 파일 → 다운로드</b>에서 APK를 눌러 <b>설치</b>합니다.</p>
              <p>경고가 나오면 <b>자세히 → 그래도 설치</b>. 막히면 01을 다시 확인하세요.</p>
            </li>
          </ol>
          <div className="btn-row">
            <Link href="/guide" className="btn btn-primary">설치 후 할 일</Link>
          </div>
          <p className="muted small" style={{ marginTop: 16 }}>
            문제가 있으면{' '}
            <a href={contact} className="text-link" {...externalProps(contact)}>오픈채팅</a>
            으로 화면을 캡처해 보내 주세요.
          </p>
        </div>
      </section>
    </SiteShell>
  );
}
