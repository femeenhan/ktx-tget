import { getRepo } from '../../../lib/repo';
import { formatKst } from '../format';
import { LatestButton } from './latest-button';
import { UploadForm } from './upload-form';

export const dynamic = 'force-dynamic';

export default async function ReleasesPage() {
  const rows = await getRepo().releases.list();
  return (
    <>
      <UploadForm />
      <table>
        <thead>
          <tr><th>버전</th><th>크기</th><th>올린 날짜</th><th></th></tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td>{r.versionName} ({r.versionCode})</td>
              <td>{(r.sizeBytes / 1024 / 1024).toFixed(1)} MB</td>
              <td>{formatKst(r.uploadedAt)}</td>
              <td>{r.isLatest ? <span className="badge">최신</span> : <LatestButton id={r.id} />}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
