// ===========================================
// 유즈케이스 - 임박/만료 아이템 조회
// ===========================================

import type { FoodItem, ExpiryStatus } from '@/domain/entities';
import type { IFoodItemRepository } from '@/domain/repositories';

export interface GetExpiringItemsInput {
    alertDays?: number;
}

export interface ExpiringItemGroup {
    status: ExpiryStatus;
    label: string;
    items: FoodItem[];
    count: number;
}

export interface GetExpiringItemsOutput {
    groups: ExpiringItemGroup[];
    totalCount: number;
    expiredCount: number;
    expiringCount: number;
}

/**
 * 임박/만료 아이템 조회 유즈케이스
 */
export class GetExpiringItemsUseCase {
    constructor(private readonly repository: IFoodItemRepository) { }

    async execute(input: GetExpiringItemsInput = {}): Promise<GetExpiringItemsOutput> {
        const { alertDays = 3 } = input;

        const items = await this.repository.getExpiringItems(alertDays);

        // 상태별 그룹화
        const expired: FoodItem[] = [];
        const expiring: FoodItem[] = [];

        items.forEach(item => {
            const status = item.getExpiryStatus(alertDays);
            if (status === 'expired') {
                expired.push(item);
            } else if (status === 'expiring') {
                expiring.push(item);
            }
        });

        // 만료일 기준 정렬 (가까운 순)
        const sortByExpiry = (a: FoodItem, b: FoodItem) =>
            a.getDaysUntilExpiry() - b.getDaysUntilExpiry();

        expired.sort(sortByExpiry);
        expiring.sort(sortByExpiry);

        const groups: ExpiringItemGroup[] = [];

        if (expired.length > 0) {
            groups.push({
                status: 'expired',
                label: '유통기한 지남',
                items: expired,
                count: expired.length,
            });
        }

        if (expiring.length > 0) {
            groups.push({
                status: 'expiring',
                label: '임박',
                items: expiring,
                count: expiring.length,
            });
        }

        return {
            groups,
            totalCount: items.length,
            expiredCount: expired.length,
            expiringCount: expiring.length,
        };
    }
}
