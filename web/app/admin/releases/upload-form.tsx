'use client';
import { upload } from '@vercel/blob/client';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { versionCodeFromName } from '../../../lib/version-code';

export function UploadForm() {
  const router = useRouter();
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const [versionName, setVersionName] = useState('');
  const busy = status === '업로드 중…' || status === '등록 중…';
  const previewCode = versionCodeFromName(versionName.trim());

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    const file = fd.get('file');
    if (!(file instanceof File) || file.size === 0) { setError('APK 파일을 선택하세요'); return; }
    const versionName = String(fd.get('versionName') ?? '').trim();
    const versionCode = versionCodeFromName(versionName);
    if (versionCode === null) { setError('versionName은 X.Y.Z 숫자 꼴이어야 합니다 (예: 0.3.3)'); return; }
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
          versionCode,
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
      setVersionName('');
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
        <input
          name="versionName"
          maxLength={40}
          placeholder="versionName (예: 0.3.3)"
          required
          value={versionName}
          onChange={(e) => setVersionName(e.target.value)}
        />{' '}
        <small>versionCode {previewCode ?? '?'}</small>
      </p>
      <p><small>versionCode는 versionName에서 자동으로 계산합니다 (X.Y.Z → X×10000 + Y×100 + Z). 앱의 build.gradle.kts도 같은 공식을 씁니다.</small></p>
      <p><textarea name="notes" rows={3} cols={50} placeholder="변경 내용" /></p>
      <button type="submit" disabled={busy}>업로드</button> <span>{status}</span>
      {error && <p className="row-error">{error}</p>}
    </form>
  );
}
