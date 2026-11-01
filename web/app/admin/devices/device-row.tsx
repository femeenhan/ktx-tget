'use client';
import { useState, useTransition } from 'react';
import type { DeviceStatus, Plan } from '../../../lib/license';
import { approveDevice, deleteDevice, extendDevice, revokeDevice, saveNote } from './actions';
import { PLAN_LABEL } from './labels';

interface Props {
  id: string;
  status: DeviceStatus;
  statusLabel: string;
  requestedAt: string;
  depositor: string;
  requestedPlanLabel: string;
  requestedPlan: Plan;
  pendingPlan: Plan | null;
  /** 입금 확인 전 임시 이용 중(대기 행). expiresAt에 끝나는 시각이 들어 있다. */
  trial: boolean;
  expiresAt: string;
  note: string;
  /** 앱이 마지막으로 상태를 물은 시각(KST). 서비스가 켜져 있으면 30분마다, 대기·임시 이용 중이면 1분마다 갱신. */
  lastSeen: string;
  appVersion: number | null;
}

export function DeviceRowView(p: Props) {
  const [plan, setPlan] = useState<Plan>(p.status === 'pending' ? p.requestedPlan : (p.pendingPlan ?? 'month'));
  const [note, setNote] = useState(p.note);
  const [error, setError] = useState('');
  const [busy, start] = useTransition();

  function act(fn: () => Promise<void>) {
    setError('');
    start(async () => {
      try {
        await fn();
      } catch (e) {
        setError(e instanceof Error ? e.message : '실패했습니다');
      }
    });
  }

  const canPlan = p.status === 'pending' || p.status === 'active' || p.status === 'expired';
  return (
    <tr>
      <td>{p.requestedAt}</td>
      <td>{p.depositor}</td>
      <td>{p.requestedPlanLabel}</td>
      <td title={p.id}>{p.id.slice(0, 6)}</td>
      <td>
        {p.statusLabel}
        {(p.status === 'active' || p.status === 'expired') && p.pendingPlan && <> <span className="badge">연장 요청</span></>}
        {p.trial && <> <span className="badge">임시 이용 중</span></>}
      </td>
      <td>{p.expiresAt}</td>
      <td>
        {p.lastSeen}
        {p.appVersion !== null && <><br /><span className="muted-cell">v{p.appVersion}</span></>}
      </td>
      <td>
        <input value={note} onChange={(e) => setNote(e.target.value)} aria-label="메모" />{' '}
        <button type="button" disabled={busy} onClick={() => act(() => saveNote(p.id, note))}>저장</button>
      </td>
      <td className="actions">
        {canPlan && (
          <select value={plan} onChange={(e) => setPlan(e.target.value as Plan)} aria-label="기간">
            {(Object.keys(PLAN_LABEL) as Plan[]).map((k) => <option key={k} value={k}>{PLAN_LABEL[k]}</option>)}
          </select>
        )}
        {p.status === 'pending' && <button type="button" disabled={busy} onClick={() => act(() => approveDevice(p.id, plan))}>승인</button>}
        {(p.status === 'active' || p.status === 'expired') && (
          <button type="button" disabled={busy} onClick={() => act(() => extendDevice(p.id, plan))}>연장</button>
        )}
        {p.status !== 'revoked' && (
          <button type="button" disabled={busy} onClick={() => act(() => revokeDevice(p.id))}>회수</button>
        )}
        <button
          type="button"
          disabled={busy}
          onClick={() => { if (confirm('정말 삭제할까요?')) act(() => deleteDevice(p.id)); }}
        >
          삭제
        </button>
        {error && <p className="row-error" role="alert">{error}</p>}
      </td>
    </tr>
  );
}
