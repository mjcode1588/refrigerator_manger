// ===========================================
// API 클라이언트 - 인프라 계층
// HTTP 통신 추상화
// ===========================================

import { getAccessToken } from '@/lib/storage';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8000/api/v1';

function normalizeHeaders(headers?: HeadersInit): Record<string, string> {
    if (!headers) return {};
    if (headers instanceof Headers) {
        return Object.fromEntries(headers.entries());
    }
    if (Array.isArray(headers)) {
        return Object.fromEntries(headers);
    }
    return headers;
}

function withAuthHeaders(headers?: HeadersInit, contentType?: string): Record<string, string> {
    const base = normalizeHeaders(headers);
    const token = getAccessToken();
    return {
        ...(contentType ? { 'Content-Type': contentType } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...base,
    };
}

export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: {
        code: string;
        message: string;
    };
}

type ErrorPayload = {
    detail?: string;
    message?: string;
};

async function readJson<T>(response: Response): Promise<T | undefined> {
    if (response.status === 204) return undefined;
    const text = await response.text();
    if (!text) return undefined;
    return JSON.parse(text) as T;
}

function getErrorMessage(error: unknown): string | undefined {
    if (!error || typeof error !== 'object') return undefined;
    const payload = error as ErrorPayload;
    return payload.detail || payload.message;
}

/**
 * 공통 fetch 래퍼
 */
export async function fetchApi<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<ApiResponse<T>> {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers: withAuthHeaders(options.headers, 'application/json'),
        });

        if (!response.ok) {
            const error = await readJson<ErrorPayload>(response).catch(() => undefined);
            return {
                success: false,
                error: {
                    code: `HTTP_${response.status}`,
                    message: getErrorMessage(error) || '요청 처리 중 오류가 발생했습니다.',
                },
            };
        }

        const data = await readJson<T>(response);
        return { success: true, data };
    } catch (error) {
        return {
            success: false,
            error: {
                code: 'NETWORK_ERROR',
                message: error instanceof Error ? error.message : '네트워크 오류가 발생했습니다.',
            },
        };
    }
}

/**
 * multipart/form-data fetch
 */
export async function fetchMultipart<T>(
    endpoint: string,
    formData: FormData
): Promise<ApiResponse<T>> {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'POST',
            body: formData,
            headers: withAuthHeaders(),
        });

        if (!response.ok) {
            const error = await readJson<ErrorPayload>(response).catch(() => undefined);
            return {
                success: false,
                error: {
                    code: `HTTP_${response.status}`,
                    message: getErrorMessage(error) || '요청 처리 중 오류가 발생했습니다.',
                },
            };
        }

        const data = await readJson<T>(response);
        return { success: true, data };
    } catch (error) {
        return {
            success: false,
            error: {
                code: 'NETWORK_ERROR',
                message: error instanceof Error ? error.message : '네트워크 오류가 발생했습니다.',
            },
        };
    }
}
