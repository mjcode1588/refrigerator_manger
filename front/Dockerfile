# ================================
# Stage 1: Dependencies
# ================================
FROM node:20-alpine AS deps
WORKDIR /app

# 패키지 매니저 설정
RUN corepack enable && corepack prepare pnpm@latest --activate

# 의존성 파일 복사
COPY package.json package-lock.json* ./

# 의존성 설치
RUN npm ci --legacy-peer-deps

# ================================
# Stage 2: Build
# ================================
FROM node:20-alpine AS builder
WORKDIR /app

# 의존성 복사
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# 환경변수 설정 (빌드 시점)
ARG NEXT_PUBLIC_API_BASE_URL
ARG NEXT_PUBLIC_DEFAULT_FRIDGE_ID
ARG NEXT_PUBLIC_ACCESS_TOKEN
ARG NEXT_PUBLIC_APP_NAME
ARG NEXT_PUBLIC_DEFAULT_EXPIRY_DAYS

ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_PUBLIC_DEFAULT_FRIDGE_ID=${NEXT_PUBLIC_DEFAULT_FRIDGE_ID}
ENV NEXT_PUBLIC_ACCESS_TOKEN=${NEXT_PUBLIC_ACCESS_TOKEN}
ENV NEXT_PUBLIC_APP_NAME=${NEXT_PUBLIC_APP_NAME}
ENV NEXT_PUBLIC_DEFAULT_EXPIRY_DAYS=${NEXT_PUBLIC_DEFAULT_EXPIRY_DAYS}

# Next.js 텔레메트리 비활성화
ENV NEXT_TELEMETRY_DISABLED=1

# 빌드
RUN npm run build

# ================================
# Stage 3: Production Runner
# ================================
FROM node:20-alpine AS runner
WORKDIR /app

# 프로덕션 환경 설정
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# 보안을 위한 non-root 사용자 생성
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# public 폴더 복사
COPY --from=builder /app/public ./public

# standalone 출력 디렉토리 구조 설정
# .next/standalone에는 서버 실행에 필요한 최소한의 파일만 포함됨
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# 사용자 전환
USER nextjs

# 포트 노출
EXPOSE 3000

# 환경변수 설정
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# standalone 서버 시작
CMD ["node", "server.js"]
