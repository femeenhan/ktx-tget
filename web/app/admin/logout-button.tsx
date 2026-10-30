'use client';
import { useRouter } from 'next/navigation';

export function LogoutButton() {
  const router = useRouter();
  return (
    <button
      type="button"
      onClick={async () => {
        await fetch('/api/admin/logout', { method: 'POST' });
        router.replace('/admin/login');
      }}
    >
      로그아웃
    </button>
  );
}
