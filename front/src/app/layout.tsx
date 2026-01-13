import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "냉장고 관리 | 스마트한 식품 관리의 시작",
  description: "AI 기반 식품 인식, 유통기한 관리, 레시피 추천까지. 더 스마트한 냉장고 관리를 경험하세요.",
  keywords: ["냉장고", "식품관리", "유통기한", "레시피", "AI"],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          {children}
        </Providers>
      </body>
    </html>
  );
}
