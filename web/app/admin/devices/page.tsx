import { getRepo } from '../../../lib/repo';
import { countPending, isRenewal, sortDevices } from './device-list';
import { DeviceRowView } from './device-row';
import { formatKst } from '../format';
import { PLAN_LABEL, STATUS_LABEL } from './labels';

export const dynamic = 'force-dynamic';

export default async function DevicesPage() {
  const now = new Date();
  const rows = await getRepo().devices.list();
  const items = sortDevices(rows, now);
  const { pending, renewal } = countPending(rows, now);

  return (
    <>
      <p className="pending-count">대기 {pending} · 연장 {renewal}</p>
      <table>
        <thead>
          <tr>
            <th>요청 시각</th><th>입금자명</th><th>요청 기간</th><th>기기 ID</th>
            <th>상태</th><th>만료일</th><th>메모</th><th>동작</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => {
            const { row, status } = item;
            const renew = isRenewal(item);
            return (
            <DeviceRowView
              key={row.deviceId}
              id={row.deviceId}
              status={status}
              statusLabel={STATUS_LABEL[status]}
              requestedAt={formatKst(renew ? row.agreedAt : row.requestedAt)}
              depositor={row.depositor ?? ''}
              requestedPlanLabel={renew && row.pendingPlan ? `연장: ${PLAN_LABEL[row.pendingPlan]}` : PLAN_LABEL[row.requestedPlan]}
              requestedPlan={row.requestedPlan}
              pendingPlan={row.pendingPlan}
              expiresAt={status === 'pending' || status === 'revoked' ? '-' : row.expiresAt ? formatKst(row.expiresAt) : '영구'}
              note={row.note ?? ''}
            />
            );
          })}
        </tbody>
      </table>
    </>
  );
}
