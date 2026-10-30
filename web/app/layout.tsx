import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "ktx-tget", template: "%s · ktx-tget" },
  description: "코레일톡 취소표를 감지해 예매 직전까지 자동으로 진행하는 안드로이드 앱",
};

export const viewport: Viewport = { themeColor: "#E6EBE2" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        {/* eslint-disable-next-line @next/next/no-page-custom-font */}
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500&family=Space+Grotesk:wght@400;500&display=swap"
        />
      </head>
      <body>{children}</body>
    </html>
  );
}
