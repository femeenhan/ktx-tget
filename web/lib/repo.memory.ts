import type { EventRow } from './events';
import type { DeviceRow } from './license';
import type { ReleaseRow, Repo } from './repo';

const DEFAULT_SETTINGS: Record<string, string> = {
  price_day: '0',
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
  let events: EventRow[] = [];

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
      async clearDepositor(ids) {
        for (const id of ids) {
          const d = devices.get(id);
          if (d) devices.set(id, { ...d, depositor: null });
        }
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
    events: {
      async insert(e) {
        events.push({ ...e });
      },
      async list(since) {
        return events.filter((e) => e.ts >= since).sort((a, b) => a.ts.getTime() - b.ts.getTime()).map((e) => ({ ...e }));
      },
      async deleteBefore(before) {
        const n = events.length;
        events = events.filter((e) => e.ts >= before);
        return n - events.length;
      },
    },
  };
}
