'use server';
import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import { getRepo } from '../../../lib/repo';
import { requireAdmin } from '../require-admin';
import { SETTING_KEYS, saveSettingsCore } from './settings-admin';

export async function saveSettings(formData: FormData): Promise<void> {
  await requireAdmin();
  const values: Record<string, string> = {};
  for (const k of SETTING_KEYS) {
    const v = formData.get(k);
    if (typeof v === 'string') values[k] = v;
  }
  await saveSettingsCore(getRepo(), values);
  revalidatePath('/admin/settings');
  redirect('/admin/settings?saved=1');
}
