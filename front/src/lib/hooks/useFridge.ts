// ===========================================
// 냉장고/가족 공유 관련 React Query 훅
// ===========================================

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fridgesApi } from '@/lib/api';

// 쿼리 키
export const fridgeKeys = {
    all: ['fridges'] as const,
    current: () => [...fridgeKeys.all, 'current'] as const,
    members: (fridgeId: string) => [...fridgeKeys.all, fridgeId, 'members'] as const,
};

// 현재 냉장고 정보
export function useCurrentFridge() {
    return useQuery({
        queryKey: fridgeKeys.current(),
        queryFn: async () => {
            const response = await fridgesApi.getCurrentFridge();
            if (!response.success) {
                throw new Error(response.error?.message || '냉장고 정보 조회에 실패했습니다.');
            }
            return response.data!;
        },
    });
}

// 멤버 목록 조회
export function useFridgeMembers(fridgeId: string) {
    return useQuery({
        queryKey: fridgeKeys.members(fridgeId),
        queryFn: async () => {
            const response = await fridgesApi.getMembers(fridgeId);
            if (!response.success) {
                throw new Error(response.error?.message || '멤버 조회에 실패했습니다.');
            }
            return response.data!;
        },
        enabled: !!fridgeId,
    });
}

// 초대 코드 생성
export function useCreateInvite() {
    return useMutation({
        mutationFn: async () => {
            const response = await fridgesApi.createInvite();
            if (!response.success) {
                throw new Error(response.error?.message || '초대 코드 생성에 실패했습니다.');
            }
            return response.data!;
        },
    });
}

// 냉장고 합류
export function useJoinFridge() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (inviteCode: string) => {
            const response = await fridgesApi.join(inviteCode);
            if (!response.success) {
                throw new Error(response.error?.message || '냉장고 합류에 실패했습니다.');
            }
            return response.data!;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: fridgeKeys.all });
        },
    });
}
