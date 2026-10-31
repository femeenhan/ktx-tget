# ktx-tget web

tget 랜딩·APK 배포·라이선스 API·관리자 페이지 (Next.js, Vercel).

```
pnpm install
cp .env.example .env.local   # 값 채우기
REPO=memory pnpm dev          # DB 없이 로컬 개발
pnpm test                     # vitest
pnpm migrate                  # DATABASE_URL에 db/schema.sql 적용 (멱등)
```

릴리스(APK) 업로드는 Vercel Blob을 쓰므로 `BLOB_READ_WRITE_TOKEN`이 필요하다 (로컬 `REPO=memory`만으로는 업로드 불가, 메타 등록 API는 동작).

## 배포 (Vercel CLI, 프로젝트 `tget-web`)

```
vercel link --yes --project tget-web --scope femeenhans-projects   # 처음 한 번 (.vercel/은 git 제외)
vercel deploy --prod                                              # 비상용 수동 운영 배포. 평소엔 tget-web 브랜치 푸시로 자동 배포
vercel deploy                                                     # Preview 배포 (지금은 운영과 같은 DB를 씀)
```

환경변수는 Vercel에 있다. Neon(`DATABASE_URL` 등)과 Blob(`BLOB_READ_WRITE_TOKEN`)은 연동이 주입하고, `ADMIN_PASSWORD`·`SESSION_SECRET`·`CRON_SECRET`은 직접 넣었다(값: `~/keystores/tget-web-secrets.txt`).

```
vercel env pull .env.production.local --environment=production --yes   # git 제외 파일
# 민감 변수 3개는 pull하면 빈 값이므로 ~/keystores/tget-web-secrets.txt에서 채운다
set -a; . ./.env.production.local; set +a
pnpm migrate                                                            # 운영 DB에 스키마 적용 (멱등)
```

DB 스키마를 바꿨으면 `pnpm migrate`를 배포 전에 돌린다. 자세한 운영 절차(가비아 DNS, 릴리스 업로드, 운영 전 할 일)는 루트 `README.md`.
