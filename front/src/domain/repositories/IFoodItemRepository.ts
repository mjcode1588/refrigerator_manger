// ===========================================
// 리포지토리 인터페이스 - FoodItem
// 클린 아키텍처: 도메인 계층에서 인터페이스 정의
// ===========================================

import type { FoodItem, FoodItemProps, FoodCategory, StorageLocation, ExpiryStatus } from '../entities';

export interface ItemFilterOptions {
    category?: FoodCategory;
    storageLocation?: StorageLocation;
    expiryStatus?: ExpiryStatus;
    search?: string;
    sortBy?: 'name' | 'expiryDate' | 'createdAt';
    sortOrder?: 'asc' | 'desc';
}

export interface PaginatedResult<T> {
    items: T[];
    total: number;
    page: number;
    pageSize: number;
    hasNext: boolean;
}

/**
 * FoodItem 리포지토리 인터페이스
 * 인프라 계층에서 구현
 */
export interface IFoodItemRepository {
    /**
     * 아이템 목록 조회
     */
    getItems(
        filters?: ItemFilterOptions,
        page?: number,
        pageSize?: number
    ): Promise<PaginatedResult<FoodItem>>;

    /**
     * 단일 아이템 조회
     */
    getItemById(id: string): Promise<FoodItem | null>;

    /**
     * 아이템 저장 (생성/수정)
     */
    saveItem(item: FoodItem): Promise<FoodItem>;

    /**
     * 여러 아이템 일괄 저장
     */
    saveItems(items: FoodItem[]): Promise<FoodItem[]>;

    /**
     * 아이템 삭제
     */
    deleteItem(id: string): Promise<void>;

    /**
     * 임박/만료 아이템 조회
     */
    getExpiringItems(days?: number): Promise<FoodItem[]>;
}
