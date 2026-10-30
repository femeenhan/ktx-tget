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

// 첫 사용 시점에 클라이언트를 만드는 지연 태그 함수
export const sql: Sql = ((...args: Parameters<Sql>) =>
  (getSql() as (...a: Parameters<Sql>) => ReturnType<Sql>)(...args)) as Sql;
