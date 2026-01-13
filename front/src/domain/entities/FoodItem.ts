// ===========================================
// 도메인 엔티티 - FoodItem
// 비즈니스 로직의 핵심, 외부 의존성 없음
// ===========================================

import { differenceInDays, parseISO } from 'date-fns';

export type FoodCategory =
    | 'vegetables'
    | 'fruits'
    | 'meat'
    | 'seafood'
    | 'dairy'
    | 'beverages'
    | 'condiments'
    | 'frozen'
    | 'etc';

export type StorageLocation = 'freezer' | 'fridge' | 'room';

export type ExpiryStatus = 'expired' | 'expiring' | 'fresh';

export interface FoodItemProps {
    id: string;
    name: string;
    category: FoodCategory;
    quantity: number;
    unit: string;
    purchaseDate: string;
    expiryDate: string;
    storageLocation: StorageLocation;
    memo?: string;
    imageUrl?: string;
    createdAt: string;
    updatedAt: string;
}

/**
 * FoodItem 도메인 엔티티
 * 비즈니스 규칙을 캡슐화
 */
export class FoodItem {
    private constructor(private readonly props: FoodItemProps) { }

    // Factory method
    static create(props: FoodItemProps): FoodItem {
        FoodItem.validate(props);
        return new FoodItem(props);
    }

    // Validation
    private static validate(props: FoodItemProps): void {
        if (!props.name || props.name.trim() === '') {
            throw new Error('식품명은 필수입니다.');
        }
        if (props.quantity < 0) {
            throw new Error('수량은 0 이상이어야 합니다.');
        }
    }

    // Getters
    get id(): string { return this.props.id; }
    get name(): string { return this.props.name; }
    get category(): FoodCategory { return this.props.category; }
    get quantity(): number { return this.props.quantity; }
    get unit(): string { return this.props.unit; }
    get purchaseDate(): string { return this.props.purchaseDate; }
    get expiryDate(): string { return this.props.expiryDate; }
    get storageLocation(): StorageLocation { return this.props.storageLocation; }
    get memo(): string | undefined { return this.props.memo; }
    get imageUrl(): string | undefined { return this.props.imageUrl; }
    get createdAt(): string { return this.props.createdAt; }
    get updatedAt(): string { return this.props.updatedAt; }

    /**
     * 만료까지 남은 일수 계산
     */
    getDaysUntilExpiry(): number {
        if (!this.props.expiryDate) {
            return Number.POSITIVE_INFINITY;
        }
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const expiry = parseISO(this.props.expiryDate);
        if (Number.isNaN(expiry.getTime())) {
            return Number.POSITIVE_INFINITY;
        }
        expiry.setHours(0, 0, 0, 0);
        return differenceInDays(expiry, today);
    }

    /**
     * 만료 상태 확인
     */
    getExpiryStatus(alertDays: number = 3): ExpiryStatus {
        const daysUntilExpiry = this.getDaysUntilExpiry();

        if (daysUntilExpiry < 0) {
            return 'expired';
        }
        if (daysUntilExpiry <= alertDays) {
            return 'expiring';
        }
        return 'fresh';
    }

    /**
     * D-Day 텍스트 반환
     */
    getDDayText(): string {
        if (!this.props.expiryDate) {
            return '미설정';
        }
        const days = this.getDaysUntilExpiry();
        if (!Number.isFinite(days)) {
            return '미설정';
        }

        if (days < 0) {
            return `D+${Math.abs(days)}`;
        }
        if (days === 0) {
            return 'D-Day';
        }
        return `D-${days}`;
    }

    /**
     * 만료 여부
     */
    isExpired(): boolean {
        return this.getDaysUntilExpiry() < 0;
    }

    /**
     * 임박 여부 (기본 3일 이내)
     */
    isExpiring(alertDays: number = 3): boolean {
        const days = this.getDaysUntilExpiry();
        return days >= 0 && days <= alertDays;
    }

    /**
     * DTO로 변환 (API 응답용)
     */
    toDTO(): FoodItemProps {
        return { ...this.props };
    }

    /**
     * 수량 업데이트
     */
    updateQuantity(quantity: number): FoodItem {
        if (quantity < 0) {
            throw new Error('수량은 0 이상이어야 합니다.');
        }
        return new FoodItem({
            ...this.props,
            quantity,
            updatedAt: new Date().toISOString(),
        });
    }
}
