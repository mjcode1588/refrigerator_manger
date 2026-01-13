// ===========================================
// 도메인 엔티티 - CandidateItem
// AI 인식 결과 후보 아이템
// ===========================================

import type { FoodCategory, StorageLocation } from './FoodItem';

export interface CandidateItemProps {
    id: string;
    name: string;
    category: FoodCategory;
    quantity: number;
    unit: string;
    expiryDate: string;
    storageLocation: StorageLocation;
    confidence: number; // 인식 신뢰도 0-1
    selected: boolean;
    memo?: string;
}

/**
 * CandidateItem 도메인 엔티티
 * 휴먼 인 더 루프 확인용 후보 아이템
 */
export class CandidateItem {
    private constructor(private props: CandidateItemProps) { }

    static create(props: CandidateItemProps): CandidateItem {
        CandidateItem.validate(props);
        return new CandidateItem({ ...props });
    }

    private static validate(props: CandidateItemProps): void {
        if (!props.name || props.name.trim() === '') {
            throw new Error('식품명은 필수입니다.');
        }
        if (props.confidence < 0 || props.confidence > 1) {
            throw new Error('신뢰도는 0과 1 사이여야 합니다.');
        }
    }

    // Getters
    get id(): string { return this.props.id; }
    get name(): string { return this.props.name; }
    get category(): FoodCategory { return this.props.category; }
    get quantity(): number { return this.props.quantity; }
    get unit(): string { return this.props.unit; }
    get expiryDate(): string { return this.props.expiryDate; }
    get storageLocation(): StorageLocation { return this.props.storageLocation; }
    get confidence(): number { return this.props.confidence; }
    get selected(): boolean { return this.props.selected; }
    get memo(): string | undefined { return this.props.memo; }

    /**
     * 신뢰도 퍼센트 (0-100)
     */
    getConfidencePercent(): number {
        return Math.round(this.props.confidence * 100);
    }

    /**
     * 신뢰도 레벨
     */
    getConfidenceLevel(): 'high' | 'medium' | 'low' {
        if (this.props.confidence >= 0.8) return 'high';
        if (this.props.confidence >= 0.5) return 'medium';
        return 'low';
    }

    /**
     * 선택 토글
     */
    toggleSelection(): CandidateItem {
        return new CandidateItem({
            ...this.props,
            selected: !this.props.selected,
        });
    }

    /**
     * 선택 상태 설정
     */
    setSelected(selected: boolean): CandidateItem {
        return new CandidateItem({
            ...this.props,
            selected,
        });
    }

    /**
     * 속성 업데이트 (휴먼 인 더 루프에서 수정)
     */
    update(updates: Partial<Omit<CandidateItemProps, 'id' | 'confidence'>>): CandidateItem {
        const newProps = { ...this.props, ...updates };
        return CandidateItem.create(newProps);
    }

    /**
     * 확정 저장용 DTO (id, confidence, selected 제외)
     */
    toConfirmDTO(): Omit<CandidateItemProps, 'id' | 'confidence' | 'selected'> {
        return {
            name: this.props.name,
            category: this.props.category,
            quantity: this.props.quantity,
            unit: this.props.unit,
            expiryDate: this.props.expiryDate,
            storageLocation: this.props.storageLocation,
            memo: this.props.memo,
        };
    }

    toDTO(): CandidateItemProps {
        return { ...this.props };
    }
}
