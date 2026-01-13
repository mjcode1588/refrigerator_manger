// ===========================================
// 유즈케이스 - 아이템 인식 및 확정
// 휴먼 인 더 루프 플로우 처리
// ===========================================

import type { CandidateItem, FoodItem } from '@/domain/entities';
import type { IIngestRepository } from '@/domain/repositories';

export interface IngestItemsInput {
    text?: string;
    images?: File[];
}

export interface IngestItemsOutput {
    candidates: CandidateItem[];
}

/**
 * 아이템 인식 유즈케이스
 * 텍스트 + 이미지를 분석하여 후보 아이템 생성
 */
export class IngestItemsUseCase {
    constructor(private readonly repository: IIngestRepository) { }

    async execute(input: IngestItemsInput): Promise<IngestItemsOutput> {
        const { text, images } = input;

        // 최소 하나의 입력 필요
        if (!text && (!images || images.length === 0)) {
            throw new Error('텍스트 또는 이미지를 입력해주세요.');
        }

        const result = await this.repository.ingest(text, images);

        return {
            candidates: result.candidates,
        };
    }
}

export interface IngestImageInput {
    images: File[];
}

/**
 * 이미지 인식 유즈케이스
 */
export class IngestImageUseCase {
    constructor(private readonly repository: IIngestRepository) { }

    async execute(input: IngestImageInput): Promise<IngestItemsOutput> {
        const { images } = input;

        if (!images || images.length === 0) {
            throw new Error('이미지를 선택해주세요.');
        }

        const result = await this.repository.ingestImage(images);

        return {
            candidates: result.candidates,
        };
    }
}

export interface ConfirmItemsInput {
    candidates: CandidateItem[];
}

export interface ConfirmItemsOutput {
    savedItems: FoodItem[];
    savedCount: number;
}

/**
 * 아이템 확정 저장 유즈케이스
 * 사용자가 확인/수정한 후보를 최종 저장
 */
export class ConfirmItemsUseCase {
    constructor(private readonly repository: IIngestRepository) { }

    async execute(input: ConfirmItemsInput): Promise<ConfirmItemsOutput> {
        const { candidates } = input;

        // 선택된 아이템만 필터링
        const selectedCandidates = candidates.filter(c => c.selected);

        if (selectedCandidates.length === 0) {
            throw new Error('저장할 아이템을 선택해주세요.');
        }

        const savedItems = await this.repository.confirm(selectedCandidates);

        return {
            savedItems,
            savedCount: savedItems.length,
        };
    }
}
