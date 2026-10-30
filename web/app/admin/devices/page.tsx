import { computeStatus, type DeviceRow, type DeviceStatus } from '../../../lib/license';
import { getRepo } from '../../../lib/repo';
import { DeviceRowView } from './device-row';
import { formatKst, PLAN_LABEL, STATUS_LABEL } from './labels';

export const dynamic = 'force-dynamic';

const ORDER: Record<DeviceStatus, number> = { pending: 0, active: 1, expired: 2, revoked: 2 };

export default async function DevicesPage() {
  const now = new Date();
  const items = (await getRepo().devices.list())
    .map((row: DeviceRow) => ({ row, status: computeStatus(row, now) }))
    .sort((a, b) => ORDER[a.status] - ORDER[b.status] || b.row.requestedAt.getTime() - a.row.requestedAt.getTime());
  const pending = items.filter((i) => i.status === 'pending').length;

  return (
    <>
      <p className="pending-count">대기 {pending}건</p>
      <table>
        <thead>
          <tr>
            <th>요청 시각</th><th>입금자명</th><th>요청 기간</th><th>기기 ID</th>
            <th>상태</th><th>만료일</th><th>메모</th><th>동작</th>
          </tr>
        </thead>
        <tbody>
          {items.map(({ row, status }) => (
            <DeviceRowView
              key={row.deviceId}
              id={row.deviceId}
              status={status}
              statusLabel={STATUS_LABEL[status]}
              requestedAt={formatKst(row.requestedAt)}
              depositor={row.depositor ?? ''}
              requestedPlanLabel={PLAN_LABEL[row.requestedPlan]}
              requestedPlan={row.requestedPlan}
              pendingPlan={row.pendingPlan}
              expiresAt={status === 'pending' || status === 'revoked' ? '-' : row.expiresAt ? formatKst(row.expiresAt) : '영구'}
              note={row.note ?? ''}
            />
          ))}
        </tbody>
      </table>
    </>
  );
}
