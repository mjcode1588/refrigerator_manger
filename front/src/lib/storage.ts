const TOKEN_KEY = 'access_token';
const FRIDGE_KEY = 'current_fridge';

export interface StoredFridge {
    id: string;
    name?: string;
    role?: string;
}

function safeJsonParse<T>(value: string | null): T | null {
    if (!value) return null;
    try {
        return JSON.parse(value) as T;
    } catch {
        return null;
    }
}

export function getAccessToken(): string | null {
    if (typeof window === 'undefined') {
        return process.env.NEXT_PUBLIC_ACCESS_TOKEN || null;
    }
    return window.localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): void {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken(): void {
    if (typeof window === 'undefined') return;
    window.localStorage.removeItem(TOKEN_KEY);
}

export function getCurrentFridge(): StoredFridge | null {
    if (typeof window === 'undefined') {
        const envId = process.env.NEXT_PUBLIC_DEFAULT_FRIDGE_ID;
        return envId ? { id: envId, name: '기본 냉장고' } : null;
    }
    return safeJsonParse<StoredFridge>(window.localStorage.getItem(FRIDGE_KEY));
}

export function setCurrentFridge(fridge: StoredFridge): void {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(FRIDGE_KEY, JSON.stringify(fridge));
}

export function clearCurrentFridge(): void {
    if (typeof window === 'undefined') return;
    window.localStorage.removeItem(FRIDGE_KEY);
}

export function getCurrentFridgeId(): string | null {
    const stored = getCurrentFridge();
    return stored?.id || process.env.NEXT_PUBLIC_DEFAULT_FRIDGE_ID || null;
}
