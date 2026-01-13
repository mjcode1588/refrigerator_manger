// ===========================================
// 레시피 관련 React Query 훅
// ===========================================

import { useMutation, useQuery } from '@tanstack/react-query';
import { recipesApi } from '@/lib/api';

// 쿼리 키
export const recipeKeys = {
    all: ['recipes'] as const,
    suggestions: () => [...recipeKeys.all, 'suggestions'] as const,
    details: () => [...recipeKeys.all, 'detail'] as const,
    detail: (id: string) => [...recipeKeys.details(), id] as const,
};

// 레시피 추천
export function useSuggestRecipes() {
    return useMutation({
        mutationFn: async () => {
            const response = await recipesApi.suggest();
            if (!response.success) {
                throw new Error(response.error?.message || '레시피 추천에 실패했습니다.');
            }
            return response.data!;
        },
    });
}

// 레시피 상세 조회
export function useRecipe(id: string) {
    return useQuery({
        queryKey: recipeKeys.detail(id),
        queryFn: async () => {
            const response = await recipesApi.getRecipe(id);
            if (!response.success) {
                throw new Error(response.error?.message || '레시피 조회에 실패했습니다.');
            }
            return response.data!;
        },
        enabled: !!id,
    });
}
