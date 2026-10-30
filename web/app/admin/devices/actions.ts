'use server';
import { revalidatePath } from 'next/cache';
import type { Plan } from '../../../lib/license';
import { getRepo } from '../../../lib/repo';
import { requireAdmin } from '../require-admin';
import {
  approveDeviceCore,
  deleteDeviceCore,
  extendDeviceCore,
  revokeDeviceCore,
  saveNoteCore,
} from './device-admin';

async function run(fn: () => Promise<void>): Promise<void> {
  await requireAdmin();
  await fn();
  revalidatePath('/admin/devices');
}

export async function approveDevice(id: string, plan: Plan): Promise<void> {
  await run(() => approveDeviceCore(getRepo(), id, plan, new Date()));
}
export async function extendDevice(id: string, plan: Plan): Promise<void> {
  await run(() => extendDeviceCore(getRepo(), id, plan, new Date()));
}
export async function revokeDevice(id: string): Promise<void> {
  await run(() => revokeDeviceCore(getRepo(), id, new Date()));
}
export async function saveNote(id: string, note: string): Promise<void> {
  await run(() => saveNoteCore(getRepo(), id, note));
}
export async function deleteDevice(id: string): Promise<void> {
  await run(() => deleteDeviceCore(getRepo(), id));
}
