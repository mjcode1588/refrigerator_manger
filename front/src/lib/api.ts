// ===========================================
// API 클라이언트
// ===========================================

import type {
    ApiResponse,
    CandidateItem,
    ConfirmRequest,
    FoodItem,
    Fridge,
    FridgeMember,
    IngestResponse,
    ItemFilterOptions,
    PaginatedResponse,
    Recipe,
    UserSettings,
} from '@/types';
import { generateId, getExpiryStatus } from '@/lib/utils';
import { getAccessToken, getCurrentFridge, getCurrentFridgeId, setCurrentFridge } from '@/lib/storage';

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

async function readJson<T>(response: Response): Promise<T | undefined> {
    if (response.status === 204) return undefined;
    const text = await response.text();
    if (!text) return undefined;
    return JSON.parse(text) as T;
}

// 공통 fetch 래퍼
async function fetchApi<T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<ApiResponse<T>> {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers: withAuthHeaders(options.headers, 'application/json'),
        });

        if (!response.ok) {
            const error = await readJson<{ detail?: string; message?: string }>(response).catch(() => ({}));
            return {
                success: false,
                error: {
                    code: `HTTP_${response.status}`,
                    message: error?.detail || error?.message || '요청 처리 중 오류가 발생했습니다.',
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

// multipart/form-data fetch
async function fetchMultipart<T>(
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
            const error = await readJson<{ detail?: string; message?: string }>(response).catch(() => ({}));
            return {
                success: false,
                error: {
                    code: `HTTP_${response.status}`,
                    message: error?.detail || error?.message || '요청 처리 중 오류가 발생했습니다.',
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

// DTO helpers
interface CandidateDTO {
    name: string;
    quantity?: number | null;
    unit?: string | null;
    expiry_date?: string | null;
    storage_location?: string | null;
    confidence?: number | null;
    source?: string;
}

interface ItemDTO {
    id: string;
    name: string;
    category?: string | null;
    quantity?: number | null;
    unit?: string | null;
    purchase_date?: string | null;
    expiry_date?: string | null;
    storage_location?: string | null;
    status?: string;
    notes?: string | null;
    created_at?: string | null;
    updated_at?: string | null;
}

interface RecipeSuggestionDTO {
    title: string;
    steps: string[];
    use_items: string[];
    missing_items: string[];
}

function normalizeStorage(value?: string | null): 'fridge' | 'freezer' | 'room' {
    if (value === 'freezer' || value === 'fridge' || value === 'room') return value;
    return 'fridge';
}

function mapCandidate(dto: CandidateDTO): CandidateItem {
    return {
        id: generateId(),
        name: dto.name,
        category: 'etc',
        quantity: dto.quantity ?? 1,
        unit: dto.unit ?? '',
        expiryDate: dto.expiry_date ?? '',
        storageLocation: normalizeStorage(dto.storage_location),
        confidence: dto.confidence ?? 0.5,
        selected: true,
        memo: undefined,
    };
}

function mapItem(dto: ItemDTO): FoodItem {
    return {
        id: dto.id,
        name: dto.name,
        category: (dto.category as FoodItem['category']) || 'etc',
        quantity: dto.quantity ?? 1,
        unit: dto.unit ?? '',
        purchaseDate: dto.purchase_date ?? '',
        expiryDate: dto.expiry_date ?? '',
        storageLocation: normalizeStorage(dto.storage_location),
        memo: dto.notes ?? undefined,
        imageUrl: undefined,
        createdAt: dto.created_at ?? new Date().toISOString(),
        updatedAt: dto.updated_at ?? dto.created_at ?? new Date().toISOString(),
    };
}

function mapRecipe(dto: RecipeSuggestionDTO): Recipe {
    return {
        id: generateId(),
        name: dto.title,
        description: '',
        imageUrl: undefined,
        cookingTime: 15,
        difficulty: 'easy',
        servings: 1,
        useItems: (dto.use_items || []).map((name) => ({ name, quantity: '' })),
        missingItems: (dto.missing_items || []).map((name) => ({ name, quantity: '' })),
        instructions: dto.steps || [],
    };
}

// ===========================================
// 아이템 API
// ===========================================

export const itemsApi = {
    // 텍스트 + 이미지로 아이템 인식
    async ingest(text?: string, images?: File[]): Promise<ApiResponse<IngestResponse>> {
        const formData = new FormData();
        if (text) {
            formData.append('text', text);
        }
        if (images) {
            images.forEach((image) => {
                formData.append('images', image);
            });
        }
        const response = await fetchMultipart<{ candidates: CandidateDTO[] }>('/items/ingest', formData);
        if (!response.success || !response.data) {
            return response as ApiResponse<IngestResponse>;
        }
        return {
            success: true,
            data: {
                candidates: response.data.candidates.map(mapCandidate),
            },
        };
    },

    // 이미지만으로 아이템 인식
    async ingestImage(images: File[]): Promise<ApiResponse<IngestResponse>> {
        const formData = new FormData();
        const first = images[0];
        if (!first) {
            return {
                success: false,
                error: {
                    code: 'NO_IMAGE',
                    message: '이미지를 선택해주세요.',
                },
            };
        }
        if (first) {
            formData.append('image', first);
        }
        const response = await fetchMultipart<{ candidates: CandidateDTO[] }>('/items/image', formData);
        if (!response.success || !response.data) {
            return response as ApiResponse<IngestResponse>;
        }
        return {
            success: true,
            data: {
                candidates: response.data.candidates.map(mapCandidate),
            },
        };
    },

    // 후보 아이템 확정 저장
    async confirm(request: ConfirmRequest): Promise<ApiResponse<FoodItem[]>> {
        const fridgeId = request.fridgeId || getCurrentFridgeId();
        if (!fridgeId) {
            return {
                success: false,
                error: {
                    code: 'MISSING_FRIDGE',
                    message: '냉장고를 먼저 설정해주세요.',
                },
            };
        }

        const payload = {
            fridge_id: fridgeId,
            items: request.items.map((item) => ({
                name: item.name,
                category: item.category,
                quantity: item.quantity ?? null,
                unit: item.unit || null,
                purchase_date: null,
                expiry_date: item.expiryDate || null,
                storage_location: item.storageLocation || null,
                notes: item.memo || null,
            })),
        };

        const response = await fetchApi<ItemDTO[]>('/items/confirm', {
            method: 'POST',
            body: JSON.stringify(payload),
        });
        if (!response.success || !response.data) {
            return response as ApiResponse<FoodItem[]>;
        }
        return { success: true, data: response.data.map(mapItem) };
    },

    // 아이템 목록 조회
    async getItems(
        filters?: ItemFilterOptions,
        page = 1,
        pageSize = 20
    ): Promise<ApiResponse<PaginatedResponse<FoodItem>>> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            return {
                success: false,
                error: {
                    code: 'MISSING_FRIDGE',
                    message: '냉장고를 먼저 설정해주세요.',
                },
            };
        }

        const response = await fetchApi<ItemDTO[]>(`/items?fridge_id=${fridgeId}`);
        if (!response.success || !response.data) {
            return response as ApiResponse<PaginatedResponse<FoodItem>>;
        }

        let items = response.data.map(mapItem);

        if (filters) {
            if (filters.category) {
                items = items.filter(item => item.category === filters.category);
            }
            if (filters.storageLocation) {
                items = items.filter(item => item.storageLocation === filters.storageLocation);
            }
            if (filters.expiryStatus) {
                items = items.filter(item => getExpiryStatus(item.expiryDate) === filters.expiryStatus);
            }
            if (filters.search) {
                const term = filters.search.toLowerCase();
                items = items.filter(item => item.name.toLowerCase().includes(term));
            }
        }

        if (filters?.sortBy) {
            const direction = filters.sortOrder === 'desc' ? -1 : 1;
            const toTime = (value: string) => {
                const time = Date.parse(value);
                return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time;
            };

            items.sort((a, b) => {
                if (filters.sortBy === 'name') {
                    return a.name.localeCompare(b.name) * direction;
                }
                if (filters.sortBy === 'createdAt') {
                    return (toTime(a.createdAt) - toTime(b.createdAt)) * direction;
                }
                return (toTime(a.expiryDate) - toTime(b.expiryDate)) * direction;
            });
        }

        const total = items.length;
        const start = (page - 1) * pageSize;
        const paged = items.slice(start, start + pageSize);

        return {
            success: true,
            data: {
                items: paged,
                total,
                page,
                pageSize,
                hasNext: start + pageSize < total,
            },
        };
    },

    // 단일 아이템 조회
    async getItem(id: string): Promise<ApiResponse<FoodItem>> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            return {
                success: false,
                error: {
                    code: 'MISSING_FRIDGE',
                    message: '냉장고를 먼저 설정해주세요.',
                },
            };
        }

        const response = await fetchApi<ItemDTO[]>(`/items?fridge_id=${fridgeId}`);
        if (!response.success || !response.data) {
            return response as ApiResponse<FoodItem>;
        }

        const item = response.data.map(mapItem).find((entry) => entry.id === id);
        if (!item) {
            return {
                success: false,
                error: {
                    code: 'HTTP_404',
                    message: '아이템을 찾을 수 없습니다.',
                },
            };
        }
        return { success: true, data: item };
    },

    // 아이템 수정
    async updateItem(id: string, item: Partial<FoodItem>): Promise<ApiResponse<FoodItem>> {
        const payload = {
            name: item.name,
            category: item.category,
            quantity: item.quantity ?? null,
            unit: item.unit || null,
            purchase_date: item.purchaseDate || null,
            expiry_date: item.expiryDate || null,
            storage_location: item.storageLocation || null,
            status: item.expiryDate ? getExpiryStatus(item.expiryDate) : undefined,
            notes: item.memo || null,
        };
        const response = await fetchApi<ItemDTO>(`/items/${id}`, {
            method: 'PUT',
            body: JSON.stringify(payload),
        });
        if (!response.success || !response.data) {
            return response as ApiResponse<FoodItem>;
        }
        return { success: true, data: mapItem(response.data) };
    },

    // 아이템 삭제
    async deleteItem(id: string): Promise<ApiResponse<void>> {
        return fetchApi<void>(`/items/${id}`, {
            method: 'DELETE',
        });
    },

    // 임박/만료 아이템 조회
    async getExpiringItems(days?: number): Promise<ApiResponse<FoodItem[]>> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            return {
                success: false,
                error: {
                    code: 'MISSING_FRIDGE',
                    message: '냉장고를 먼저 설정해주세요.',
                },
            };
        }

        const params = new URLSearchParams();
        params.append('fridge_id', fridgeId);
        if (days) {
            params.append('days', days.toString());
        }

        const response = await fetchApi<ItemDTO[]>(`/items/expiring?${params.toString()}`);
        if (!response.success || !response.data) {
            return response as ApiResponse<FoodItem[]>;
        }
        return { success: true, data: response.data.map(mapItem) };
    },
};

// ===========================================
// 레시피 API
// ===========================================

export const recipesApi = {
    // 냉장고 기반 레시피 추천
    async suggest(): Promise<ApiResponse<Recipe[]>> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            return {
                success: false,
                error: {
                    code: 'MISSING_FRIDGE',
                    message: '냉장고를 먼저 설정해주세요.',
                },
            };
        }

        const response = await fetchApi<{ recipes: RecipeSuggestionDTO[] }>('/recipes/suggest', {
            method: 'POST',
            body: JSON.stringify({ fridge_id: fridgeId, prefer_expiring_first: true }),
        });

        if (!response.success || !response.data) {
            return response as ApiResponse<Recipe[]>;
        }

        return { success: true, data: response.data.recipes.map(mapRecipe) };
    },

    // 레시피 상세 조회 (백엔드 미지원)
    async getRecipe(id: string): Promise<ApiResponse<Recipe>> {
        return {
            success: false,
            error: {
                code: 'NOT_IMPLEMENTED',
                message: '레시피 상세 조회는 아직 지원되지 않습니다.',
            },
        };
    },
};

// ===========================================
// 냉장고/가족 공유 API
// ===========================================

export const fridgesApi = {
    async getCurrentFridge(): Promise<ApiResponse<Fridge>> {
        const stored = getCurrentFridge();
        if (!stored) {
            return { success: true, data: undefined } as ApiResponse<Fridge>;
        }
        return {
            success: true,
            data: {
                id: stored.id,
                name: stored.name || '내 냉장고',
                inviteCode: undefined,
                members: [],
                createdAt: '',
            },
        };
    },

    async createFridge(name: string): Promise<ApiResponse<Fridge>> {
        const response = await fetchApi<{ id: string; name: string; owner_user_id: string; created_at: string }>(
            '/fridges',
            {
                method: 'POST',
                body: JSON.stringify({ name }),
            }
        );

        if (!response.success || !response.data) {
            return response as ApiResponse<Fridge>;
        }

        const fridge = {
            id: response.data.id,
            name: response.data.name,
            inviteCode: undefined,
            members: [],
            createdAt: response.data.created_at,
        } satisfies Fridge;

        setCurrentFridge({ id: fridge.id, name: fridge.name, role: 'owner' });

        return { success: true, data: fridge };
    },

    // 초대 코드 생성
    async createInvite(fridgeId?: string): Promise<ApiResponse<{ inviteCode: string }>> {
        const targetId = fridgeId || getCurrentFridgeId();
        if (!targetId) {
            return {
                success: false,
                error: {
                    code: 'MISSING_FRIDGE',
                    message: '냉장고를 먼저 설정해주세요.',
                },
            };
        }

        const response = await fetchApi<{ invite_code: string; expires_at: string }>('/fridges/invite', {
            method: 'POST',
            body: JSON.stringify({ fridge_id: targetId }),
        });

        if (!response.success || !response.data) {
            return response as ApiResponse<{ inviteCode: string }>;
        }

        return { success: true, data: { inviteCode: response.data.invite_code } };
    },

    // 냉장고 합류
    async join(inviteCode: string): Promise<ApiResponse<Fridge>> {
        const response = await fetchApi<{ fridge_id: string; role: string }>('/fridges/join', {
            method: 'POST',
            body: JSON.stringify({ invite_code: inviteCode }),
        });

        if (!response.success || !response.data) {
            return response as ApiResponse<Fridge>;
        }

        setCurrentFridge({ id: response.data.fridge_id, name: '공유 냉장고', role: response.data.role });

        return {
            success: true,
            data: {
                id: response.data.fridge_id,
                name: '공유 냉장고',
                inviteCode: undefined,
                members: [],
                createdAt: '',
            },
        };
    },

    // 멤버 목록 조회
    async getMembers(fridgeId: string): Promise<ApiResponse<FridgeMember[]>> {
        const response = await fetchApi<{ user_id: string; role: string; joined_at: string }[]>(
            `/fridges/${fridgeId}/members`
        );

        if (!response.success || !response.data) {
            return response as ApiResponse<FridgeMember[]>;
        }

        return {
            success: true,
            data: response.data.map((member) => ({
                id: member.user_id,
                name: member.user_id,
                email: '',
                role: member.role as FridgeMember['role'],
                joinedAt: member.joined_at,
            })),
        };
    },
};

// ===========================================
// 알림 API
// ===========================================

export const notificationsApi = {
    async getNotifications(): Promise<ApiResponse<unknown>> {
        return {
            success: false,
            error: {
                code: 'NOT_IMPLEMENTED',
                message: '알림 조회는 아직 지원되지 않습니다.',
            },
        };
    },
    async markAsRead(): Promise<ApiResponse<void>> {
        return {
            success: false,
            error: {
                code: 'NOT_IMPLEMENTED',
                message: '알림 읽음 처리는 아직 지원되지 않습니다.',
            },
        };
    },
    async markAllAsRead(): Promise<ApiResponse<void>> {
        return {
            success: false,
            error: {
                code: 'NOT_IMPLEMENTED',
                message: '알림 읽음 처리는 아직 지원되지 않습니다.',
            },
        };
    },
};

// ===========================================
// 설정 API
// ===========================================

export const settingsApi = {
    async getSettings(): Promise<ApiResponse<UserSettings>> {
        return {
            success: false,
            error: {
                code: 'NOT_IMPLEMENTED',
                message: '설정 조회는 아직 지원되지 않습니다.',
            },
        };
    },
    async updateSettings(): Promise<ApiResponse<UserSettings>> {
        return {
            success: false,
            error: {
                code: 'NOT_IMPLEMENTED',
                message: '설정 업데이트는 아직 지원되지 않습니다.',
            },
        };
    },
};
