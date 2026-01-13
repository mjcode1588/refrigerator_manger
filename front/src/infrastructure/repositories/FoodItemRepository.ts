// ===========================================
// FoodItem 리포지토리 구현체
// 인프라 계층: API 통신 담당
// ===========================================

import { FoodItem, FoodItemProps } from '@/domain/entities';
import type { IFoodItemRepository, ItemFilterOptions, PaginatedResult } from '@/domain/repositories';
import { fetchApi } from '../api/apiClient';
import { getExpiryStatus } from '@/lib/utils';
import { getCurrentFridgeId } from '@/lib/storage';

interface FoodItemDTO {
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

function normalizeStorageLocation(value?: string | null): FoodItemProps['storageLocation'] {
    if (value === 'freezer' || value === 'fridge' || value === 'room') return value;
    return 'fridge';
}

/**
 * DTO를 도메인 엔티티로 변환
 */
function toDomainEntity(dto: FoodItemDTO): FoodItem {
    return FoodItem.create({
        id: dto.id,
        name: dto.name,
        category: (dto.category as FoodItemProps['category']) || 'etc',
        quantity: dto.quantity ?? 1,
        unit: dto.unit ?? '',
        purchaseDate: dto.purchase_date ?? '',
        expiryDate: dto.expiry_date ?? '',
        storageLocation: normalizeStorageLocation(dto.storage_location),
        memo: dto.notes ?? undefined,
        imageUrl: undefined,
        createdAt: dto.created_at ?? new Date().toISOString(),
        updatedAt: dto.updated_at ?? dto.created_at ?? new Date().toISOString(),
    } as FoodItemProps);
}

function applyFilters(items: FoodItem[], filters?: ItemFilterOptions): FoodItem[] {
    if (!filters) return items;

    let filtered = items;

    if (filters.category) {
        filtered = filtered.filter(item => item.category === filters.category);
    }
    if (filters.storageLocation) {
        filtered = filtered.filter(item => item.storageLocation === filters.storageLocation);
    }
    if (filters.expiryStatus) {
        filtered = filtered.filter(item => getExpiryStatus(item.expiryDate) === filters.expiryStatus);
    }
    if (filters.search) {
        const term = filters.search.toLowerCase();
        filtered = filtered.filter(item => item.name.toLowerCase().includes(term));
    }

    if (filters.sortBy) {
        const direction = filters.sortOrder === 'desc' ? -1 : 1;
        const toTime = (value: string) => {
            const time = Date.parse(value);
            return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time;
        };

        filtered = [...filtered].sort((a, b) => {
            if (filters.sortBy === 'name') {
                return a.name.localeCompare(b.name) * direction;
            }
            if (filters.sortBy === 'createdAt') {
                return (toTime(a.createdAt) - toTime(b.createdAt)) * direction;
            }
            return (toTime(a.expiryDate) - toTime(b.expiryDate)) * direction;
        });
    }

    return filtered;
}

/**
 * FoodItem 리포지토리 API 구현체
 */
export class FoodItemRepository implements IFoodItemRepository {
    async getItems(
        filters?: ItemFilterOptions,
        page = 1,
        pageSize = 20
    ): Promise<PaginatedResult<FoodItem>> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            throw new Error('냉장고를 먼저 설정해주세요.');
        }

        const response = await fetchApi<FoodItemDTO[]>(`/items?fridge_id=${fridgeId}`);

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '아이템 조회에 실패했습니다.');
        }

        const allItems = response.data.map(toDomainEntity);
        const filtered = applyFilters(allItems, filters);

        const total = filtered.length;
        const start = (page - 1) * pageSize;
        const items = filtered.slice(start, start + pageSize);

        return {
            items,
            total,
            page,
            pageSize,
            hasNext: start + pageSize < total,
        };
    }

    async getItemById(id: string): Promise<FoodItem | null> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            throw new Error('냉장고를 먼저 설정해주세요.');
        }

        const response = await fetchApi<FoodItemDTO[]>(`/items?fridge_id=${fridgeId}`);

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '아이템 조회에 실패했습니다.');
        }

        const items = response.data.map(toDomainEntity);
        return items.find(item => item.id === id) || null;
    }

    async saveItem(item: FoodItem): Promise<FoodItem> {
        const dto = item.toDTO();
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            throw new Error('냉장고를 먼저 설정해주세요.');
        }

        if (!dto.id) {
            const response = await fetchApi<FoodItemDTO[]>('/items/confirm', {
                method: 'POST',
                body: JSON.stringify({
                    fridge_id: fridgeId,
                    items: [
                        {
                            name: dto.name,
                            category: dto.category,
                            quantity: dto.quantity,
                            unit: dto.unit,
                            purchase_date: dto.purchaseDate || null,
                            expiry_date: dto.expiryDate || null,
                            storage_location: dto.storageLocation || null,
                            notes: dto.memo || null,
                        },
                    ],
                }),
            });

            if (!response.success || !response.data) {
                throw new Error(response.error?.message || '아이템 저장에 실패했습니다.');
            }

            return toDomainEntity(response.data[0]);
        }

        const response = await fetchApi<FoodItemDTO>(`/items/${dto.id}`, {
            method: 'PUT',
            body: JSON.stringify({
                name: dto.name,
                category: dto.category,
                quantity: dto.quantity,
                unit: dto.unit,
                purchase_date: dto.purchaseDate || null,
                expiry_date: dto.expiryDate || null,
                storage_location: dto.storageLocation || null,
                notes: dto.memo || null,
            }),
        });

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '아이템 저장에 실패했습니다.');
        }

        return toDomainEntity(response.data);
    }

    async saveItems(items: FoodItem[]): Promise<FoodItem[]> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            throw new Error('냉장고를 먼저 설정해주세요.');
        }

        const payload = {
            fridge_id: fridgeId,
            items: items.map(item => {
                const dto = item.toDTO();
                return {
                    name: dto.name,
                    category: dto.category,
                    quantity: dto.quantity,
                    unit: dto.unit,
                    purchase_date: dto.purchaseDate || null,
                    expiry_date: dto.expiryDate || null,
                    storage_location: dto.storageLocation || null,
                    notes: dto.memo || null,
                };
            }),
        };

        const response = await fetchApi<FoodItemDTO[]>('/items/confirm', {
            method: 'POST',
            body: JSON.stringify(payload),
        });

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '아이템 일괄 저장에 실패했습니다.');
        }

        return response.data.map(toDomainEntity);
    }

    async deleteItem(id: string): Promise<void> {
        const response = await fetchApi<void>(`/items/${id}`, {
            method: 'DELETE',
        });

        if (!response.success) {
            throw new Error(response.error?.message || '아이템 삭제에 실패했습니다.');
        }
    }

    async getExpiringItems(days = 3): Promise<FoodItem[]> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            throw new Error('냉장고를 먼저 설정해주세요.');
        }

        const response = await fetchApi<FoodItemDTO[]>(`/items/expiring?fridge_id=${fridgeId}&days=${days}`);

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '임박 아이템 조회에 실패했습니다.');
        }

        return response.data.map(toDomainEntity);
    }
}

// 싱글톤 인스턴스
export const foodItemRepository = new FoodItemRepository();
