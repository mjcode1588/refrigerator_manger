// ===========================================
// 유즈케이스 - 아이템 목록 조회
// 애플리케이션 계층: 비즈니스 로직 조합
// ===========================================

import type { FoodItem } from '@/domain/entities';
import type { IFoodItemRepository, ItemFilterOptions, PaginatedResult } from '@/domain/repositories';

export interface GetItemsInput {
    filters?: ItemFilterOptions;
    page?: number;
    pageSize?: number;
}

export interface GetItemsOutput {
    items: FoodItem[];
    total: number;
    page: number;
    pageSize: number;
    hasNext: boolean;
}

/**
 * 아이템 목록 조회 유즈케이스
 */
export class GetItemsUseCase {
    constructor(private readonly repository: IFoodItemRepository) { }

    async execute(input: GetItemsInput = {}): Promise<GetItemsOutput> {
        const { filters, page = 1, pageSize = 20 } = input;

        const result = await this.repository.getItems(filters, page, pageSize);

        return {
            items: result.items,
            total: result.total,
            page: result.page,
            pageSize: result.pageSize,
            hasNext: result.hasNext,
        };
    }
}
