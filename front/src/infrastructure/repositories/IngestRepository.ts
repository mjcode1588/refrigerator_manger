// ===========================================
// Ingest 리포지토리 구현체
// AI 인식 및 확정 저장 처리
// ===========================================

import { CandidateItem, CandidateItemProps, FoodItem, FoodItemProps } from '@/domain/entities';
import type { IIngestRepository, IngestResult } from '@/domain/repositories';
import { fetchApi, fetchMultipart } from '../api/apiClient';
import { generateId } from '@/lib/utils';
import { getCurrentFridgeId } from '@/lib/storage';

interface CandidateDTO {
    name: string;
    quantity?: number | null;
    unit?: string | null;
    expiry_date?: string | null;
    storage_location?: string | null;
    confidence?: number | null;
}

interface IngestResponseDTO {
    candidates: CandidateDTO[];
}

interface FoodItemDTO {
    id: string;
    name: string;
    category?: string | null;
    quantity?: number | null;
    unit?: string | null;
    purchase_date?: string | null;
    expiry_date?: string | null;
    storage_location?: string | null;
    notes?: string | null;
    created_at?: string | null;
    updated_at?: string | null;
}

function normalizeStorageLocation(value?: string | null): CandidateItemProps['storageLocation'] {
    if (value === 'freezer' || value === 'fridge' || value === 'room') return value;
    return 'fridge';
}

function candidateToDomain(dto: CandidateDTO): CandidateItem {
    return CandidateItem.create({
        id: generateId(),
        name: dto.name,
        category: 'etc',
        quantity: dto.quantity ?? 1,
        unit: dto.unit ?? '',
        expiryDate: dto.expiry_date ?? '',
        storageLocation: normalizeStorageLocation(dto.storage_location),
        confidence: dto.confidence ?? 0.5,
        selected: true,
    } as CandidateItemProps);
}

function foodItemToDomain(dto: FoodItemDTO): FoodItem {
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

/**
 * Ingest 리포지토리 API 구현체
 */
export class IngestRepository implements IIngestRepository {
    async ingest(text?: string, images?: File[]): Promise<IngestResult> {
        const formData = new FormData();
        if (text) {
            formData.append('text', text);
        }
        if (images) {
            images.forEach((image) => {
                formData.append('images', image);
            });
        }

        const response = await fetchMultipart<IngestResponseDTO>('/items/ingest', formData);

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '아이템 인식에 실패했습니다.');
        }

        return {
            candidates: response.data.candidates.map(candidateToDomain),
        };
    }

    async ingestImage(images: File[]): Promise<IngestResult> {
        const formData = new FormData();
        const first = images[0];
        if (first) {
            formData.append('image', first);
        }

        const response = await fetchMultipart<IngestResponseDTO>('/items/image', formData);

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '이미지 인식에 실패했습니다.');
        }

        return {
            candidates: response.data.candidates.map(candidateToDomain),
        };
    }

    async confirm(items: CandidateItem[]): Promise<FoodItem[]> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            throw new Error('냉장고를 먼저 설정해주세요.');
        }

        const payload = {
            fridge_id: fridgeId,
            items: items
                .filter(item => item.selected)
                .map(item => ({
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

        const response = await fetchApi<FoodItemDTO[]>('/items/confirm', {
            method: 'POST',
            body: JSON.stringify(payload),
        });

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '아이템 저장에 실패했습니다.');
        }

        return response.data.map(foodItemToDomain);
    }
}

// 싱글톤 인스턴스
export const ingestRepository = new IngestRepository();
