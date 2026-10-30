import { Footer } from './Footer';
import { SiteHeader } from './SiteHeader';
import { contactHref } from './site-data';
import './site.css';

export function SiteShell({ children, settings }: { children: React.ReactNode; settings: Record<string, string> }) {
  return (
    <div className="site">
      <a href="#main" className="skip">본문으로 건너뛰기</a>
      <SiteHeader contact={contactHref(settings)} />
      <main id="main">{children}</main>
      <Footer settings={settings} />
    </div>
  );
}
