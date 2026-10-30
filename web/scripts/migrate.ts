import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { getSql } from '../lib/db';

async function main() {
  const text = readFileSync(join(__dirname, '..', 'db', 'schema.sql'), 'utf8');
  // neon 서버리스 드라이버는 다중 구문을 받지 않으므로 줄 끝의 ;로 나눈다
  const statements = text
    .split(/;\s*$/m)
    .map((s) => s.trim())
    .filter(Boolean);
  const sql = getSql();
  for (const stmt of statements) {
    await sql.query(stmt);
  }
  console.log(`applied ${statements.length} statements`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
