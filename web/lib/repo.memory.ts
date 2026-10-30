import type { DeviceRow } from './license';
import type { ReleaseRow, Repo } from './repo';

const DEFAULT_SETTINGS: Record<string, string> = {
  price_lifetime: '0',
  price_month: '0',
  price_year: '0',
  bank_account: '',
  openchat_url: 'https://open.kakao.com/o/s1cF7MUi',
  notice: '',
  biz_reg_no: '',
};

export function createMemoryRepo(): Repo {
  const devices = new Map<string, DeviceRow>();
  let releases: ReleaseRow[] = [];
  let nextId = 1;
  const settings = { ...DEFAULT_SETTINGS };

  return {
    devices: {
      async get(id) {
        const d = devices.get(id);
        return d ? { ...d } : null;
      },
      async upsert(row) {
        devices.set(row.deviceId, { ...row });
      },
      async list() {
        return [...devices.values()].map((d) => ({ ...d }));
      },
      async delete(id) {
        devices.delete(id);
      },
      async touch(id, now, appVersion) {
        const d = devices.get(id);
        if (d) devices.set(id, { ...d, lastSeenAt: now, appVersion: appVersion ?? d.appVersion });
      },
    },
    releases: {
      async latest() {
        const r = releases.find((x) => x.isLatest);
        return r ? { ...r } : null;
      },
      async list() {
        return releases.map((r) => ({ ...r })).sort((a, b) => b.versionCode - a.versionCode);
      },
      async insert(r) {
        if (releases.some((x) => x.versionCode === r.versionCode)) {
          throw Object.assign(new Error('duplicate version'), { code: 'DUPLICATE_VERSION' });
        }
        releases = releases.map((x) => ({ ...x, isLatest: false }));
        const row: ReleaseRow = { ...r, id: nextId++, isLatest: true, uploadedAt: new Date() };
        releases.push(row);
        return { ...row };
      },
      async setLatest(id) {
        releases = releases.map((x) => ({ ...x, isLatest: x.id === id }));
      },
    },
    settings: {
      async all() {
        return { ...settings };
      },
      async set(key, value) {
        settings[key] = value;
      },
    },
  };
}
