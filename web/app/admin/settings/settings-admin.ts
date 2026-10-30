import type { Repo } from '../../../lib/repo';

export const SETTING_KEYS = ['price_lifetime', 'price_month', 'price_year', 'bank_account', 'openchat_url', 'notice', 'biz_reg_no'] as const;
const PRICE_KEYS = new Set(['price_lifetime', 'price_month', 'price_year']);

export async function saveSettingsCore(repo: Repo, values: Record<string, string>): Promise<void> {
  const out: [string, string][] = [];
  for (const key of SETTING_KEYS) {
    if (!(key in values)) continue;
    const v = values[key].trim();
    if (PRICE_KEYS.has(key) && !/^\d+$/.test(v)) throw new Error('invalid price');
    if (key === 'openchat_url' && v !== '' && !v.startsWith('https://')) throw new Error('invalid url');
    out.push([key, v]);
  }
  for (const [k, v] of out) await repo.settings.set(k, v);
}
