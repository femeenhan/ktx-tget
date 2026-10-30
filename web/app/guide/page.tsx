import type { Metadata } from 'next';
import Link from 'next/link';
import { SiteShell } from '../components/SiteShell';
import { contactHref, externalProps, loadSettings } from '../components/site-data';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: '사용 안내',
  description: '설치한 뒤 접근성·알림·배터리를 켜고, 활성화를 요청하고, 코레일톡 목록에서 시작하는 방법.',
};

const TOC: [string, string][] = [
  ['access', '접근성 켜기'],
  ['notify', '알림·배터리'],
  ['license', '활성화·연장'],
  ['korail', '코레일톡 준비'],
  ['settings', '앱 설정'],
  ['run', '시작과 중지'],
  ['device', '기기를 바꿨을 때'],
  ['faq', '자주 묻는 것'],
];

const SETTINGS: [string, string][] = [
  ['새로고침 간격', '−/+로 2.0~3.0초 사이, 0.25초 단위로 조절합니다.'],
  ['누를 좌석 칸', '취소표가 난 칸 중 어디를 누를지. 일반실만 / 특실만 / 둘 다(일반실 먼저).'],
  ['예매 알림', '결제 화면에 닿았을 때 알림. 끔 / 기본(알림과 짧은 진동) / 강함(끌 때까지 알람).'],
  ['좌석 유형 제외', '켜 둔 유형(자유석, 입석+좌석, 입석)만 남은 열차는 넘깁니다.'],
  ['제외할 열차번호', '열차번호를 넣고 「추가」. 칩의 ×로 뺍니다. 넣는 즉시 적용됩니다.'],
  ['정차 안내 자동 확인', '켜면 정차 안내 팝업에서 「확인」을 누르고 이어갑니다. 끄면 팝업에서 멈춥니다.'],
];

export default async function GuidePage() {
  const settings = await loadSettings();
  const contact = contactHref(settings);

  return (
    <SiteShell settings={settings}>
      <section className="hero" aria-labelledby="guide-title" style={{ paddingBottom: 24 }}>
        <div className="wrap wrap-narrow">
          <p className="eyebrow">사용 안내</p>
          <h1 id="guide-title">설치한 다음 할 일</h1>
          <p className="lede">
            아직 설치 전이라면 <Link href="/download" className="text-link">다운로드</Link>부터 하세요. 아래 순서대로 한 번만 설정하면 됩니다.
          </p>
          <nav aria-label="이 페이지 목차">
            <ul className="toc">
              {TOC.map(([id, label]) => (
                <li key={id}><a href={`#${id}`}>{label}</a></li>
              ))}
            </ul>
          </nav>
        </div>
      </section>

      <div className="wrap wrap-narrow" style={{ paddingBottom: 56 }}>
        <section id="access" className="guide-section" aria-labelledby="h-access">
          <h2 id="h-access">1. 접근성 켜기 <span className="muted small">(필수)</span></h2>
          <p>접근성이 꺼져 있으면 아무 동작도 하지 않습니다. 직접 설치한 앱은 「제한된 설정」으로 잠겨 있을 수 있으니 ①을 먼저 하세요.</p>
          <h3>① 제한된 설정 허용</h3>
          <p><b>설정 → 애플리케이션 → ktx-tget → ⋮ → 제한된 설정 허용</b>, 잠금 화면(PIN·지문) 확인.</p>
          <h3>② 접근성 켜기</h3>
          <p>앱 홈의 「접근성 권한」 줄에서 「설정 열기」를 누르거나, <b>설정 → 접근성 → 설치된 앱 → ktx-tget</b>에서 <b>사용</b>을 켜고 <b>허용</b>합니다.</p>
          <p className="callout">ktx-tget이 「사용 중」이고 「제한된 설정으로 제어됨」 문구가 없으면 됩니다.</p>
        </section>

        <section id="notify" className="guide-section" aria-labelledby="h-notify">
          <h2 id="h-notify">2. 알림·배터리</h2>
          <h3>알림 <span className="muted small">(필수)</span></h3>
          <p>처음 「시작」을 누르면 알림 허용 창이 뜹니다. <b>허용</b>을 누르세요. 밤에 자면서 쓸 거라면:</p>
          <ul className="dot-list">
            <li>앱의 <b>예매 알림</b>을 「강함」으로 둡니다. 끌 때까지 알람이 울립니다.</li>
            <li><b>설정 → 애플리케이션 → 특수 액세스 → 전체 화면 알림</b>에서 ktx-tget을 허용합니다.</li>
            <li><b>설정 → 소리 및 진동</b>에서 알람 볼륨을 올립니다.</li>
          </ul>
          <h3>배터리 <span className="muted small">(권장)</span></h3>
          <p><b>설정 → 배터리 → 백그라운드 사용 제한</b>에서 ktx-tget을 <b>절대 절전 안 함</b>으로 둡니다. 밤새 써도 끊기지 않습니다.</p>
        </section>

        <section id="license" className="guide-section" aria-labelledby="h-license">
          <h2 id="h-license">3. 활성화·연장</h2>
          <p>앱을 처음 열면 「활성화 요청」 카드가 보입니다. 기기 ID나 키를 복사해 보낼 필요는 없습니다.</p>
          <ol className="flow" style={{ marginTop: 16 }}>
            <li><b>입금자명</b>을 적고 <b>기간</b>(영구·1개월·1년)을 고릅니다.</li>
            <li>「승인 후에는 환불되지 않는다는 점에 동의합니다」를 체크하고 <b>요청 보내기</b>.</li>
            <li>카드에 보이는 계좌로, 적은 입금자명 그대로 입금합니다.</li>
            <li>「승인 대기 중」으로 바뀝니다. 입금이 확인되면 판매자가 승인합니다.</li>
            <li>승인되면 앱이 <b>자동으로</b> 풀리고 「시작」 버튼이 나타납니다.</li>
          </ol>
          <h3>연장</h3>
          <p>만료 7일 전부터 홈에 「연장 요청」이 보입니다. 같은 방법으로 기간을 고르고 입금하면, 승인될 때 남은 기간 뒤에 이어 붙습니다.</p>
          <p className="muted small">환불 규정은 <Link href="/terms" className="text-link">이용약관</Link>에 있습니다. 승인 전에는 전액 환불, 승인 후에는 환불되지 않습니다.</p>
        </section>

        <section id="korail" className="guide-section" aria-labelledby="h-korail">
          <h2 id="h-korail">4. 코레일톡 준비</h2>
          <ol className="num-list">
            <li>같은 휴대폰에 <b>코레일톡</b>을 설치하고 로그인합니다.</li>
            <li>출발역·도착역·날짜로 조회해 <b>열차 목록</b>을 엽니다.</li>
            <li>원하는 시간대 열차가 <b>목록 맨 위</b>에 오도록 출발 시각을 맞춥니다. 화면에 보이는 열차만 감지합니다.</li>
          </ol>
        </section>

        <section id="settings" className="guide-section" aria-labelledby="h-settings">
          <h2 id="h-settings">5. 앱 설정</h2>
          <table className="settings-table">
            <caption className="muted small" style={{ textAlign: 'left', marginBottom: 8 }}>ktx-tget 홈 화면의 설정 항목</caption>
            <tbody>
              {SETTINGS.map(([k, v]) => (
                <tr key={k}><th scope="row">{k}</th><td>{v}</td></tr>
              ))}
            </tbody>
          </table>
        </section>

        <section id="run" className="guide-section" aria-labelledby="h-run">
          <h2 id="h-run">6. 시작과 중지</h2>
          <ol className="num-list">
            <li>맨 위 <b>「시작」</b>을 누르면 「실행 중」으로 바뀝니다.</li>
            <li><b>코레일톡 열차 목록 화면으로 돌아가</b> 그대로 둡니다. 새로고침을 반복하다가 취소표가 보이면 예매까지 진행합니다.</li>
            <li><b>결제 화면에 닿으면</b> 알림이 오고 스스로 멈춥니다. <b>결제는 직접</b> 하세요.</li>
            <li>멈추려면 ktx-tget에서 <b>「중지」</b>를 누릅니다.</li>
          </ol>
          <p className="callout">실행 중에도 설정을 바꿀 수 있고, 다음 새로고침부터 적용됩니다. 무슨 일이 있었는지는 홈의 「기록」에서 볼 수 있습니다.</p>
        </section>

        <section id="device" className="guide-section" aria-labelledby="h-device">
          <h2 id="h-device">7. 기기를 바꿨을 때</h2>
          <p>
            휴대폰을 바꾸거나 초기화하면 기기 ID가 바뀌어 라이선스가 따라가지 않습니다. 새 기기에서 앱을 열고 카드 아래쪽의 기기 ID를{' '}
            <a href={contact} className="text-link" {...externalProps(contact)}>오픈채팅</a>으로 알려 주세요. 기존 라이선스를 회수하고 새 기기를 승인해 드립니다.
          </p>
          <p className="muted small">같은 휴대폰에서 앱을 지웠다가 다시 설치한 경우에는 그대로 풀립니다.</p>
        </section>

        <section id="faq" className="guide-section" aria-labelledby="h-faq">
          <h2 id="h-faq">자주 묻는 것</h2>
          <div className="faq">
            <details>
              <summary>앱을 업데이트하면 다시 요청해야 하나요?</summary>
              <p>아니요. 새 APK를 덮어 설치하면 됩니다. 다만 업데이트 뒤 접근성이 꺼질 수 있으니 1번을 다시 확인하세요.</p>
            </details>
            <details>
              <summary>시작했는데 아무 일도 안 일어나요.</summary>
              <p>접근성이 켜져 있는지, 코레일톡 열차 목록이 화면 맨 앞에 있는지 확인하세요. 「기록」에 「코레일 앱이 앞에 있지 않습니다」가 보이면 코레일톡이 앞에 없는 것입니다.</p>
            </details>
            <details>
              <summary>코레일톡이 업데이트된 뒤로 안 돼요.</summary>
              <p>코레일톡 화면이 바뀌면 ktx-tget도 고쳐야 할 수 있습니다. 코레일톡 버전(설정 → 애플리케이션 → 코레일톡)과 「기록」 캡처를 오픈채팅으로 보내 주세요.</p>
            </details>
            <details>
              <summary>입금했는데 아직 승인 대기예요.</summary>
              <p>판매자가 입금을 직접 확인한 뒤 승인합니다. 승인되면 앱이 알아서 풀리고, 카드의 「다시 확인」으로 바로 확인할 수도 있습니다. 오래 걸리면 오픈채팅으로 알려 주세요.</p>
            </details>
            <details>
              <summary>계정이 제재될 수 있나요?</summary>
              <p>코레일 이용약관은 자동화 프로그램 사용을 제한할 수 있습니다. 계정 제재 가능성은 이용자 본인이 판단해 주세요.</p>
            </details>
          </div>
        </section>
      </div>
    </SiteShell>
  );
}
