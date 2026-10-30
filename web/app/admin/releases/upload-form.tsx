'use client';
import { upload } from '@vercel/blob/client';
import { useRouter } from 'next/navigation';
import { useState } from 'react';

export function UploadForm() {
  const router = useRouter();
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const busy = status === '업로드 중…' || status === '등록 중…';

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    const file = fd.get('file');
    if (!(file instanceof File) || file.size === 0) { setError('APK 파일을 선택하세요'); return; }
    const versionName = String(fd.get('versionName') ?? '').trim();
    setError('');
    try {
      setStatus('업로드 중…');
      const blob = await upload(`apk/ktx-tget-${versionName}.apk`, file, {
        access: 'public',
        handleUploadUrl: '/api/admin/releases/upload',
        contentType: 'application/vnd.android.package-archive',
      });
      setStatus('등록 중…');
      const res = await fetch('/api/admin/releases', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          versionCode: Number(fd.get('versionCode')),
          versionName,
          blobUrl: blob.url,
          sizeBytes: file.size,
          notes: String(fd.get('notes') ?? ''),
        }),
      });
      if (!res.ok) {
        const j = (await res.json().catch(() => ({}))) as { error?: string };
        throw new Error(j.error ?? `등록 실패 (${res.status})`);
      }
      setStatus('완료');
      form.reset();
      router.refresh();
    } catch (err) {
      setStatus('');
      setError((err as Error).message);
    }
  }

  return (
    <form onSubmit={onSubmit} className="upload-form">
      <p><input type="file" name="file" accept=".apk" required /></p>
      <p>
        <input name="versionCode" type="number" min={1} step={1} placeholder="versionCode" required />{' '}
        <input name="versionName" maxLength={40} placeholder="versionName" required />
      </p>
      <p><small>versionCode는 APK의 versionCode(app/build.gradle.kts)와 같아야 합니다. 다르면 앱에 「새 버전」 배너가 계속 뜹니다.</small></p>
      <p><textarea name="notes" rows={3} cols={50} placeholder="변경 내용" /></p>
      <button type="submit" disabled={busy}>업로드</button> <span>{status}</span>
      {error && <p className="row-error">{error}</p>}
    </form>
  );
}
