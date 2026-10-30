import type { DeviceRow, DeviceStatus, Plan } from './license';
import { sql } from './db';
import { createMemoryRepo } from './repo.memory';

export interface ReleaseRow {
  id: number;
  versionCode: number;
  versionName: string;
  blobUrl: string;
  sizeBytes: number;
  notes: string | null;
  isLatest: boolean;
  uploadedAt: Date;
}
export type ReleaseInsert = Omit<ReleaseRow, 'id' | 'isLatest' | 'uploadedAt'>;

export interface Repo {
  devices: {
    get(id: string): Promise<DeviceRow | null>;
    upsert(row: DeviceRow): Promise<void>;
    list(): Promise<DeviceRow[]>;
    delete(id: string): Promise<void>;
    /** lastSeenAt/appVersion만 갱신 (appVersion null이면 기존 값 유지). 다른 컬럼은 건드리지 않는다. */
    touch(id: string, now: Date, appVersion: number | null): Promise<void>;
    /** depositor만 null로. 다른 컬럼은 건드리지 않는다. */
    clearDepositor(ids: string[]): Promise<void>;
  };
  releases: {
    latest(): Promise<ReleaseRow | null>;
    list(): Promise<ReleaseRow[]>;
    insert(r: ReleaseInsert): Promise<ReleaseRow>;
    setLatest(id: number): Promise<void>;
  };
  settings: {
    all(): Promise<Record<string, string>>;
    set(key: string, value: string): Promise<void>;
  };
}

export { createMemoryRepo };

type R = Record<string, unknown>;
const toDate = (v: unknown): Date | null => (v == null ? null : v instanceof Date ? v : new Date(v as string));

function toDevice(r: R): DeviceRow {
  return {
    deviceId: r.device_id as string,
    depositor: (r.depositor as string | null) ?? null,
    requestedPlan: r.requested_plan as Plan,
    pendingPlan: (r.pending_plan as Plan | null) ?? null,
    status: r.status as DeviceStatus,
    expiresAt: toDate(r.expires_at),
    agreedAt: toDate(r.agreed_at)!,
    note: (r.note as string | null) ?? null,
    appVersion: r.app_version == null ? null : Number(r.app_version),
    requestedAt: toDate(r.requested_at)!,
    approvedAt: toDate(r.approved_at),
    lastSeenAt: toDate(r.last_seen_at),
    revokedAt: toDate(r.revoked_at),
  };
}

function toRelease(r: R): ReleaseRow {
  return {
    id: Number(r.id),
    versionCode: Number(r.version_code),
    versionName: r.version_name as string,
    blobUrl: r.blob_url as string,
    sizeBytes: Number(r.size_bytes),
    notes: (r.notes as string | null) ?? null,
    isLatest: Boolean(r.is_latest),
    uploadedAt: toDate(r.uploaded_at)!,
  };
}

export function createNeonRepo(): Repo {
  return {
    devices: {
      async get(id) {
        const rows = await sql`select * from devices where device_id = ${id}`;
        return rows[0] ? toDevice(rows[0]) : null;
      },
      async upsert(d) {
        await sql`
          insert into devices (device_id, depositor, requested_plan, pending_plan, status, expires_at,
            agreed_at, note, app_version, requested_at, approved_at, last_seen_at, revoked_at)
          values (${d.deviceId}, ${d.depositor}, ${d.requestedPlan}, ${d.pendingPlan}, ${d.status}, ${d.expiresAt},
            ${d.agreedAt}, ${d.note}, ${d.appVersion}, ${d.requestedAt}, ${d.approvedAt}, ${d.lastSeenAt}, ${d.revokedAt})
          on conflict (device_id) do update set
            depositor = excluded.depositor, requested_plan = excluded.requested_plan,
            pending_plan = excluded.pending_plan, status = excluded.status, expires_at = excluded.expires_at,
            agreed_at = excluded.agreed_at, note = excluded.note, app_version = excluded.app_version,
            requested_at = excluded.requested_at, approved_at = excluded.approved_at,
            last_seen_at = excluded.last_seen_at, revoked_at = excluded.revoked_at`;
      },
      async list() {
        return (await sql`select * from devices order by requested_at desc`).map(toDevice);
      },
      async delete(id) {
        await sql`delete from devices where device_id = ${id}`;
      },
      async touch(id, now, appVersion) {
        await sql`update devices set last_seen_at = ${now}, app_version = coalesce(${appVersion}::int, app_version)
          where device_id = ${id}`;
      },
      async clearDepositor(ids) {
        if (ids.length === 0) return;
        await sql`update devices set depositor = null where device_id = any(${ids})`;
      },
    },
    releases: {
      async latest() {
        const rows = await sql`select * from releases where is_latest = true limit 1`;
        return rows[0] ? toRelease(rows[0]) : null;
      },
      async list() {
        return (await sql`select * from releases order by version_code desc`).map(toRelease);
      },
      async insert(r) {
        // 삽입과 최신 지정을 한 문장으로 묶어, 중간에 실패해도 새 버전만 최신이 아닌 상태가 남지 않게 한다.
        // (데이터 수정 CTE끼리는 서로의 결과를 못 보므로 update는 기존 행만 false로 내리고, 새 행은 is_latest=true로 넣는다.)
        let rows: R[];
        try {
          rows = await sql`
            with ins as (
              insert into releases (version_code, version_name, blob_url, size_bytes, notes, is_latest, uploaded_at)
              values (${r.versionCode}, ${r.versionName}, ${r.blobUrl}, ${r.sizeBytes}, ${r.notes}, true, ${new Date()})
              returning *
            ), demote as (
              update releases set is_latest = false where is_latest = true and id <> (select id from ins)
            )
            select * from ins`;
        } catch (e) {
          if ((e as { code?: string }).code === '23505') {
            throw Object.assign(new Error('duplicate version'), { code: 'DUPLICATE_VERSION' });
          }
          throw e;
        }
        return toRelease(rows[0]);
      },
      async setLatest(id) {
        await sql`update releases set is_latest = (id = ${id})`;
      },
    },
    settings: {
      async all() {
        const rows = await sql`select key, value from settings`;
        return Object.fromEntries(rows.map((r) => [r.key as string, r.value as string]));
      },
      async set(key, value) {
        await sql`insert into settings (key, value) values (${key}, ${value})
          on conflict (key) do update set value = excluded.value`;
      },
    },
  };
}

// Next는 라우트마다 번들이 달라 모듈 변수가 공유되지 않는다. REPO=memory 스모크 테스트용으로 globalThis에 둔다.
const g = globalThis as { __tgetMemoryRepo?: Repo };

export function getRepo(): Repo {
  if (process.env.REPO === 'memory') return (g.__tgetMemoryRepo ??= createMemoryRepo());
  return createNeonRepo();
}

export function resetRepoForTests(): void {
  g.__tgetMemoryRepo = createMemoryRepo();
}
