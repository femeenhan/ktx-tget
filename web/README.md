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
