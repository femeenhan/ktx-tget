import { PLANS, approve, computeStatus, extend, revoke, type DeviceRow, type Plan } from '../../../lib/license';
import type { Repo } from '../../../lib/repo';


function checkPlan(plan: string): asserts plan is Plan {
  if (!PLANS.includes(plan as Plan)) throw new Error('invalid plan');
}

async function load(repo: Repo, id: string): Promise<DeviceRow> {
  const row = await repo.devices.get(id);
  if (!row) throw new Error('device not found');
  return row;
}

export async function approveDeviceCore(repo: Repo, id: string, plan: Plan, now: Date): Promise<void> {
  checkPlan(plan);
  const row = await load(repo, id);
  if (computeStatus(row, now) !== 'pending') throw new Error('not pending');
  await repo.devices.upsert(approve(row, plan, now));
}

export async function extendDeviceCore(repo: Repo, id: string, plan: Plan, now: Date): Promise<void> {
  checkPlan(plan);
  const row = await load(repo, id);
  const s = computeStatus(row, now);
  if (s !== 'active' && s !== 'expired') throw new Error('not extendable');
  await repo.devices.upsert(extend(row, plan, now));
}

export async function revokeDeviceCore(repo: Repo, id: string, now: Date): Promise<void> {
  const row = await load(repo, id);
  if (computeStatus(row, now) === 'revoked') throw new Error('already revoked');
  await repo.devices.upsert(revoke(row, now));
}

export async function saveNoteCore(repo: Repo, id: string, note: string): Promise<void> {
  const row = await load(repo, id);
  const trimmed = note.trim();
  await repo.devices.upsert({ ...row, note: trimmed === '' ? null : trimmed });
}

export async function deleteDeviceCore(repo: Repo, id: string): Promise<void> {
  await load(repo, id);
  await repo.devices.delete(id);
}
