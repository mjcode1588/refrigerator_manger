// ===========================================
// 아이템 관련 React Query 훅
// ===========================================

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { itemsApi } from '@/lib/api';
import type { ConfirmRequest, FoodItem, ItemFilterOptions } from '@/types';

// 쿼리 키
export const itemKeys = {
    all: ['items'] as const,
    lists: () => [...itemKeys.all, 'list'] as const,
    list: (filters?: ItemFilterOptions) => [...itemKeys.lists(), filters] as const,
    details: () => [...itemKeys.all, 'detail'] as const,
    detail: (id: string) => [...itemKeys.details(), id] as const,
    expiring: (days?: number) => [...itemKeys.all, 'expiring', days] as const,
};

// 아이템 목록 조회
export function useItems(filters?: ItemFilterOptions, page = 1, pageSize = 20) {
    return useQuery({
        queryKey: itemKeys.list(filters),
        queryFn: async () => {
            const response = await itemsApi.getItems(filters, page, pageSize);
            if (!response.success) {
                throw new Error(response.error?.message || '아이템 조회에 실패했습니다.');
            }
            return response.data!;
        },
    });
}

// 단일 아이템 조회
export function useItem(id: string) {
    return useQuery({
        queryKey: itemKeys.detail(id),
        queryFn: async () => {
            const response = await itemsApi.getItem(id);
            if (!response.success) {
                throw new Error(response.error?.message || '아이템 조회에 실패했습니다.');
            }
            return response.data!;
        },
        enabled: !!id,
    });
}

// 임박/만료 아이템 조회
export function useExpiringItems(days?: number) {
    return useQuery({
        queryKey: itemKeys.expiring(days),
        queryFn: async () => {
            const response = await itemsApi.getExpiringItems(days);
            if (!response.success) {
                throw new Error(response.error?.message || '임박 아이템 조회에 실패했습니다.');
            }
            return response.data!;
        },
    });
}

// 아이템 인식 (텍스트 + 이미지)
export function useIngestItems() {
    return useMutation({
        mutationFn: async ({ text, images }: { text?: string; images?: File[] }) => {
            const response = await itemsApi.ingest(text, images);
            if (!response.success) {
                throw new Error(response.error?.message || '아이템 인식에 실패했습니다.');
            }
            return response.data!;
        },
    });
}

// 이미지만으로 아이템 인식
export function useIngestImage() {
    return useMutation({
        mutationFn: async (images: File[]) => {
            const response = await itemsApi.ingestImage(images);
            if (!response.success) {
                throw new Error(response.error?.message || '이미지 인식에 실패했습니다.');
            }
            return response.data!;
        },
    });
}

// 후보 아이템 확정
export function useConfirmItems() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (request: ConfirmRequest) => {
            const response = await itemsApi.confirm(request);
            if (!response.success) {
                throw new Error(response.error?.message || '아이템 저장에 실패했습니다.');
            }
            return response.data!;
        },
        onSuccess: () => {
            // 아이템 목록 캐시 무효화
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
        },
    });
}

// 아이템 수정
export function useUpdateItem() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ id, item }: { id: string; item: Partial<FoodItem> }) => {
            const response = await itemsApi.updateItem(id, item);
            if (!response.success) {
                throw new Error(response.error?.message || '아이템 수정에 실패했습니다.');
            }
            return response.data!;
        },
        onSuccess: (data, { id }) => {
            queryClient.invalidateQueries({ queryKey: itemKeys.lists() });
            queryClient.setQueryData(itemKeys.detail(id), data);
        },
    });
}

// 아이템 삭제
export function useDeleteItem() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (id: string) => {
            const response = await itemsApi.deleteItem(id);
            if (!response.success) {
                throw new Error(response.error?.message || '아이템 삭제에 실패했습니다.');
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
        },
    });
}
