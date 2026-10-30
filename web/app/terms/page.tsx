import type { Metadata } from 'next';
import Link from 'next/link';
import { SiteShell } from '../components/SiteShell';
import { CONTACT_EMAIL, loadSettings } from '../components/site-data';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: '이용약관',
  description: 'ktx-tget 라이선스 이용약관과 환불 규정.',
};

export default async function TermsPage() {
  const settings = await loadSettings();
  return (
    <SiteShell settings={settings}>
      <div className="wrap wrap-narrow legal">
        <p className="eyebrow">이용약관</p>
        <h1>ktx-tget 이용약관</h1>
        <p className="meta">시행일 2026-11-01 · 빅시즌스</p>

        <div className="key">
          <p><b>요약.</b> ktx-tget 라이선스는 디지털 상품이며, 판매자가 승인하는 즉시 제공이 시작됩니다. 그래서 <b>승인 전에는 전액 환불</b>, <b>승인 후에는 청약철회·환불이 되지 않습니다.</b></p>
        </div>

        <article>
          <h2>제1조 (목적)</h2>
          <p>이 약관은 빅시즌스(이하 「판매자」)가 제공하는 안드로이드 앱 ktx-tget(이하 「앱」)의 사용 권한(이하 「라이선스」)을 구매·이용하는 사람(이하 「이용자」)과 판매자 사이의 권리와 의무를 정합니다.</p>
        </article>

        <article>
          <h2>제2조 (상품)</h2>
          <ol>
            <li>라이선스는 앱의 기능을 정해진 기간 동안 쓸 수 있게 하는 디지털 상품입니다. 기간은 영구, 1개월, 1년 세 가지입니다.</li>
            <li>1개월은 승인일로부터 달력 기준 1개월, 1년은 12개월입니다. 연장 승인 시 남은 기간 뒤에 이어 붙습니다.</li>
            <li>가격은 사이트와 앱에 표시된 금액을 따릅니다.</li>
          </ol>
        </article>

        <article>
          <h2>제3조 (구매와 승인)</h2>
          <ol>
            <li>이용자는 앱에서 입금자명과 기간을 적고 환불 규정에 동의한 뒤 활성화를 요청하고, 안내된 계좌로 입금합니다.</li>
            <li>판매자는 입금을 확인한 뒤 해당 기기를 승인합니다. 승인되는 즉시 앱의 기능이 열리며, 이때 상품 제공이 시작된 것으로 봅니다.</li>
          </ol>
        </article>

        <article>
          <h2>제4조 (청약철회와 환불)</h2>
          <ol>
            <li><b>승인 전</b>에는 이용자가 요청하면 입금액 전액을 환불합니다.</li>
            <li><b>승인 후</b>에는 청약철회와 환불이 되지 않습니다. 라이선스는 승인 즉시 제공이 시작되는 디지털 콘텐츠이므로, 「전자상거래 등에서의 소비자보호에 관한 법률」 제17조 제2항 제5호 및 같은 조항 단서에 따라 판매자는 이 사실을 구매 전에 사이트와 앱의 요청 화면에 미리 알리고, 이용자는 요청할 때 이에 동의합니다.</li>
            <li>판매자의 잘못으로 승인 후 앱을 전혀 쓸 수 없는 경우에는 판매자와 이용자가 협의하여 처리합니다.</li>
          </ol>
        </article>

        <article>
          <h2>제5조 (기기와 라이선스)</h2>
          <ol>
            <li>라이선스 하나는 기기 한 대에서만 쓸 수 있습니다.</li>
            <li>기기를 바꾸거나 초기화한 경우 이용자가 알리면, 판매자가 확인한 뒤 기존 라이선스를 회수하고 남은 기간 이상으로 새 기기에 승인합니다.</li>
            <li>라이선스를 다른 사람에게 팔거나 넘길 수 없습니다. 부정 사용이 확인되면 판매자는 라이선스를 회수할 수 있습니다.</li>
          </ol>
        </article>

        <article>
          <h2>제6조 (이용자의 책임)</h2>
          <ol>
            <li>앱은 코레일톡 화면을 읽어 예매 직전(결제 화면)까지 진행하며, 결제는 이용자가 직접 합니다.</li>
            <li>한국철도공사(코레일)의 이용약관은 자동화 프로그램 사용을 제한할 수 있습니다. 앱 사용으로 코레일 약관을 위반하여 생기는 계정 제재 등의 불이익은 이용자가 책임집니다.</li>
          </ol>
        </article>

        <article>
          <h2>제7조 (서비스의 변경과 중단)</h2>
          <ol>
            <li>코레일톡의 화면이나 동작이 바뀌면 앱이 일시적으로 또는 계속 동작하지 않을 수 있습니다.</li>
            <li>판매자는 이에 대응하는 업데이트를 제공하기 위해 노력하지만, 업데이트가 나오는 시점이나 기간은 보장하지 않습니다.</li>
            <li>이 조의 사유로 동작하지 않는 기간에 대해서는 제4조에 따라 환불하지 않습니다.</li>
          </ol>
        </article>

        <article>
          <h2>제8조 (약관의 변경)</h2>
          <p>판매자는 필요한 경우 이 약관을 바꿀 수 있으며, 바뀐 약관은 시행일 7일 전부터 이 페이지에 알립니다.</p>
        </article>

        <article>
          <h2>제9조 (분쟁 해결과 관할)</h2>
          <ol>
            <li>분쟁이 생기면 판매자와 이용자는 먼저 성실히 협의합니다.</li>
            <li>협의가 되지 않아 소송이 제기되는 경우 「민사소송법」에 따른 관할 법원으로 합니다.</li>
          </ol>
        </article>

        <article>
          <h2>제10조 (판매자 정보와 문의)</h2>
          <p>빅시즌스 · 대표 한철민 · 사업자등록번호 <span style={{ whiteSpace: 'nowrap' }}>674-66-00675</span> · 서울특별시 은평구 통일로 796 · 이메일 <a href={`mailto:${CONTACT_EMAIL}`} className="text-link">{CONTACT_EMAIL}</a></p>
          <p>개인정보 처리에 관한 내용은 <Link href="/privacy" className="text-link">개인정보 처리방침</Link>을 따릅니다.</p>
        </article>
      </div>
    </SiteShell>
  );
}
