import type { Metadata } from 'next';
import { SiteShell } from '../components/SiteShell';
import { CONTACT_EMAIL, loadSettings } from '../components/site-data';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: '개인정보 처리방침',
  description: 'ktx-tget이 수집하는 정보와 보관 기간.',
};

export default async function PrivacyPage() {
  const settings = await loadSettings();
  const mail = <a href={`mailto:${CONTACT_EMAIL}`} className="text-link">{CONTACT_EMAIL}</a>;
  return (
    <SiteShell settings={settings}>
      <div className="wrap wrap-narrow legal">
        <p className="eyebrow">개인정보 처리방침</p>
        <h1>ktx-tget 개인정보 처리방침</h1>
        <p className="meta">시행일 2026-11-01 · 빅시즌스</p>

        <div className="key">
          <p><b>요약.</b> 라이선스를 주고 입금을 확인하는 데 필요한 네 가지만 받습니다. 이름·전화번호·코레일 계정 정보, 코레일톡 화면 내용은 서버로 보내지 않습니다.</p>
        </div>

        <article>
          <h2>제1조 (수집하는 항목)</h2>
          <ol>
            <li>기기 식별값(안드로이드 ANDROID_ID)</li>
            <li>입금자명(활성화·연장 요청 때 이용자가 직접 입력)</li>
            <li>앱 버전</li>
            <li>접속 시각(요청·상태 확인 시각)과 환불 규정 동의 시각</li>
          </ol>
          <p style={{ marginTop: 10 }}>웹사이트는 운영 참고용으로 페이지 경로·유입 출처(사이트 주소)·방문 시각·내려받기 횟수만 셉니다. IP 주소·쿠키·광고 식별자는 저장하지 않으며, 특정 개인을 구분하지 않습니다.</p>
          <p style={{ marginTop: 10 }}>앱은 동작하는 동안 코레일톡 화면을 기기 안에서만 읽으며, 그 내용은 저장하거나 전송하지 않습니다.</p>
        </article>

        <article>
          <h2>제2조 (이용 목적)</h2>
          <ol>
            <li>라이선스 발급·확인·연장·회수</li>
            <li>입금 확인(입금자명과 통장 내역 대조)</li>
            <li>새 버전 안내와 문의 대응</li>
          </ol>
        </article>

        <article>
          <h2>제3조 (보관 기간과 파기)</h2>
          <ol>
            <li>입금자명: 승인·회수 후 30일, 승인되지 않은 요청은 요청 후 30일이 지나면 자동으로 지웁니다.</li>
            <li>기기 식별값·앱 버전·접속 시각 등 이용 기록: 라이선스가 만료되거나 회수된 뒤 1년이 지나면 자동으로 지웁니다. 영구 라이선스는 이용하는 동안 보관합니다.</li>
            <li>웹사이트 방문·내려받기 집계: 1년이 지나면 자동으로 지웁니다.</li>
            <li>관계 법령이 따로 보관을 요구하는 경우 그 기간 동안 보관합니다.</li>
          </ol>
        </article>

        <article>
          <h2>제4조 (처리 위탁)</h2>
          <p>서비스 운영을 위해 아래 업체에 처리를 맡깁니다. 해외(미국 등)에 있는 서버에 저장될 수 있습니다.</p>
          <ul>
            <li>Vercel Inc. — 웹사이트·API 호스팅</li>
            <li>Neon Inc. — 데이터베이스 저장</li>
          </ul>
          <p style={{ marginTop: 10 }}>그 밖의 제3자에게 개인정보를 제공하지 않습니다.</p>
        </article>

        <article>
          <h2>제5조 (이용자의 권리)</h2>
          <p>이용자는 언제든 자신의 정보를 열람·정정·삭제하거나 처리 정지를 요청할 수 있습니다. 앱에 보이는 기기 ID와 함께 아래 이메일로 요청하면 지체 없이 처리합니다. 삭제하면 해당 기기의 라이선스도 더 이상 확인할 수 없습니다.</p>
        </article>

        <article>
          <h2>제6조 (개인정보 보호책임자와 문의)</h2>
          <p>보호책임자: 한철민(빅시즌스 대표) · 이메일 {mail}</p>
          <p>개인정보 침해 신고·상담은 개인정보침해신고센터(국번 없이 118), 개인정보분쟁조정위원회(1833-6972)에도 할 수 있습니다.</p>
        </article>

        <article>
          <h2>제7조 (변경)</h2>
          <p>이 방침이 바뀌면 시행 7일 전부터 이 페이지에 알립니다.</p>
        </article>
      </div>
    </SiteShell>
  );
}
