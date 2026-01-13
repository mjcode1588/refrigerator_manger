// ===========================================
// 리포지토리 인터페이스 - Ingest (AI 인식)
// ===========================================

import type { CandidateItem, FoodItem } from '../entities';

export interface IngestResult {
    candidates: CandidateItem[];
}

/**
 * Ingest 리포지토리 인터페이스
 * AI 인식 및 확정 저장 처리
 */
export interface IIngestRepository {
    /**
     * 텍스트 + 이미지로 아이템 인식
     */
    ingest(text?: string, images?: File[]): Promise<IngestResult>;

    /**
     * 이미지만으로 아이템 인식
     */
    ingestImage(images: File[]): Promise<IngestResult>;

    /**
     * 후보 아이템 확정 저장
     */
    confirm(items: CandidateItem[]): Promise<FoodItem[]>;
}
