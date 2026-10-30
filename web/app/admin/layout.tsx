import Link from 'next/link';
import './admin.css';
import { LogoutButton } from './logout-button';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="admin">
      <header className="admin-header">
        <nav>
          <Link href="/admin/devices">기기</Link>
          <Link href="/admin/releases">릴리스</Link>
          <Link href="/admin/settings">설정</Link>
        </nav>
        <LogoutButton />
      </header>
      <main>{children}</main>
    </div>
  );
}
