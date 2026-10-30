'use client';
import { useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';

export function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const password = new FormData(e.currentTarget).get('password');
    setBusy(true);
    setError('');
    const res = await fetch('/api/admin/login', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ password }),
    });
    setBusy(false);
    if (res.status === 204) {
      const next = params.get('next');
      router.replace(next && next.startsWith('/admin') ? next : '/admin');
    } else if (res.status === 401) {
      setError('비밀번호가 올바르지 않습니다');
    } else {
      setError('로그인에 실패했습니다');
    }
  }

  return (
    <form onSubmit={onSubmit}>
      <label>
        비밀번호
        <input name="password" type="password" required autoFocus style={{ display: 'block', width: '100%', margin: '8px 0' }} />
      </label>
      <button type="submit" disabled={busy}>로그인</button>
      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}
    </form>
  );
}
