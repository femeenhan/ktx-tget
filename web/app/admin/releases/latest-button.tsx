'use client';
import { useRouter } from 'next/navigation';
import { useState } from 'react';

export function LatestButton({ id }: { id: number }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function go() {
    setBusy(true);
    setError('');
    const res = await fetch(`/api/admin/releases/${id}/latest`, { method: 'POST' }).catch(() => null);
    setBusy(false);
    if (!res || !res.ok) { setError('실패'); return; }
    router.refresh();
  }
  return (
    <>
      <button onClick={go} disabled={busy}>최신으로</button>
      {error && <span className="row-error"> {error}</span>}
    </>
  );
}
