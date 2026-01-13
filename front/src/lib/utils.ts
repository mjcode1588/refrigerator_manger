// ===========================================
// 유틸리티 함수
// ===========================================

import { format, differenceInDays, isAfter, isBefore, parseISO } from 'date-fns';
import { ko } from 'date-fns/locale';
import type { FoodItem } from '@/types';

// 날짜 포맷팅
export function formatDate(date: string | Date, formatStr = 'yyyy.MM.dd'): string {
    if (!date) return '-';
    const d = typeof date === 'string' ? parseISO(date) : date;
    if (Number.isNaN(d.getTime())) return '-';
    return format(d, formatStr, { locale: ko });
}

// 상대 날짜 (D-day 형식)
export function getDDay(expiryDate: string): { text: string; status: 'expired' | 'urgent' | 'warning' | 'fresh' } {
    if (!expiryDate) {
        return { text: '미설정', status: 'fresh' };
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expiry = parseISO(expiryDate);
    if (Number.isNaN(expiry.getTime())) {
        return { text: '미설정', status: 'fresh' };
    }
    expiry.setHours(0, 0, 0, 0);

    const diff = differenceInDays(expiry, today);

    if (diff < 0) {
        return { text: `D+${Math.abs(diff)}`, status: 'expired' };
    } else if (diff === 0) {
        return { text: 'D-Day', status: 'urgent' };
    } else if (diff <= 3) {
        return { text: `D-${diff}`, status: 'warning' };
    } else {
        return { text: `D-${diff}`, status: 'fresh' };
    }
}

// 만료 상태 확인
export function getExpiryStatus(expiryDate: string): 'expired' | 'expiring' | 'fresh' {
    if (!expiryDate) {
        return 'fresh';
    }
    const today = new Date();
    const expiry = parseISO(expiryDate);
    if (Number.isNaN(expiry.getTime())) {
        return 'fresh';
    }
    const alertDays = parseInt(process.env.NEXT_PUBLIC_DEFAULT_EXPIRY_DAYS || '3', 10);

    if (isBefore(expiry, today)) {
        return 'expired';
    }

    const daysUntilExpiry = differenceInDays(expiry, today);
    if (daysUntilExpiry <= alertDays) {
        return 'expiring';
    }

    return 'fresh';
}

// 아이템 필터링
export function filterItems(items: FoodItem[], status: 'expired' | 'expiring' | 'fresh' | 'all'): FoodItem[] {
    if (status === 'all') return items;
    return items.filter(item => getExpiryStatus(item.expiryDate) === status);
}

// 카테고리별 그룹화
export function groupByCategory(items: FoodItem[]): Map<string, FoodItem[]> {
    const grouped = new Map<string, FoodItem[]>();
    items.forEach(item => {
        const existing = grouped.get(item.category) || [];
        grouped.set(item.category, [...existing, item]);
    });
    return grouped;
}

// 보관 위치별 그룹화
export function groupByStorage(items: FoodItem[]): Map<string, FoodItem[]> {
    const grouped = new Map<string, FoodItem[]>();
    items.forEach(item => {
        const existing = grouped.get(item.storageLocation) || [];
        grouped.set(item.storageLocation, [...existing, item]);
    });
    return grouped;
}

// 파일 크기 포맷팅
export function formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

// 이미지 파일 검증
export function isValidImageFile(file: File): boolean {
    const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    return validTypes.includes(file.type);
}

// 클립보드 복사
export async function copyToClipboard(text: string): Promise<boolean> {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch {
        return false;
    }
}

// 랜덤 ID 생성 (임시용)
export function generateId(): string {
    return Math.random().toString(36).substring(2, 15);
}

// 디바운스
export function debounce<T extends (...args: unknown[]) => unknown>(
    func: T,
    wait: number
): (...args: Parameters<T>) => void {
    let timeoutId: ReturnType<typeof setTimeout>;
    return (...args: Parameters<T>) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => func(...args), wait);
    };
}

// 클래스명 결합
export function cn(...classes: (string | undefined | null | false)[]): string {
    return classes.filter(Boolean).join(' ');
}
