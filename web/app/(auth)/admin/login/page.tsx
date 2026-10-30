import { Suspense } from 'react';
import { LoginForm } from './login-form';

export default function LoginPage() {
  return (
    <main style={{ maxWidth: 320, margin: '80px auto', fontFamily: 'sans-serif' }}>
      <h1>관리자 로그인</h1>
      <Suspense>
        <LoginForm />
      </Suspense>
    </main>
  );
}
