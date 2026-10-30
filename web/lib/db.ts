import { neon } from '@neondatabase/serverless';

type Sql = ReturnType<typeof neon>;

let client: Sql | undefined;

export function getSql(): Sql {
  if (!client) {
    const url = process.env.DATABASE_URL;
    if (!url) throw new Error('DATABASE_URL is not set');
    client = neon(url);
  }
  return client;
}

// 첫 사용 시점에 클라이언트를 만드는 지연 태그 함수.
// 태그드 템플릿 호출만 지원한다. sql.query / sql.transaction 등은 getSql()을 쓸 것.
export type TaggedSql = (strings: TemplateStringsArray, ...params: unknown[]) => Promise<Record<string, unknown>[]>;

export const sql: TaggedSql = (strings, ...params) =>
  (getSql() as unknown as TaggedSql)(strings, ...params);
