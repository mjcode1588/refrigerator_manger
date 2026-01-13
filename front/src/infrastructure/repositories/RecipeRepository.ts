// ===========================================
// Recipe 리포지토리 구현체
// ===========================================

import { Recipe, RecipeProps } from '@/domain/entities';
import type { IRecipeRepository } from '@/domain/repositories';
import { fetchApi } from '../api/apiClient';
import { generateId } from '@/lib/utils';
import { getCurrentFridgeId } from '@/lib/storage';

interface RecipeSuggestionDTO {
    title: string;
    steps: string[];
    use_items: string[];
    missing_items: string[];
}

function toDomainEntity(dto: RecipeSuggestionDTO): Recipe {
    return Recipe.create({
        id: generateId(),
        name: dto.title,
        description: '',
        imageUrl: undefined,
        cookingTime: 15,
        difficulty: 'easy',
        servings: 1,
        useItems: (dto.use_items || []).map((name) => ({ name, quantity: '' })),
        missingItems: (dto.missing_items || []).map((name) => ({ name, quantity: '' })),
        instructions: dto.steps || [],
    } as RecipeProps);
}

/**
 * Recipe 리포지토리 API 구현체
 */
export class RecipeRepository implements IRecipeRepository {
    async suggestRecipes(): Promise<Recipe[]> {
        const fridgeId = getCurrentFridgeId();
        if (!fridgeId) {
            throw new Error('냉장고를 먼저 설정해주세요.');
        }

        const response = await fetchApi<{ recipes: RecipeSuggestionDTO[] }>('/recipes/suggest', {
            method: 'POST',
            body: JSON.stringify({ fridge_id: fridgeId, prefer_expiring_first: true }),
        });

        if (!response.success || !response.data) {
            throw new Error(response.error?.message || '레시피 추천에 실패했습니다.');
        }

        return response.data.recipes.map(toDomainEntity);
    }

    async getRecipeById(_id: string): Promise<Recipe | null> {
        return null;
    }
}

// 싱글톤 인스턴스
export const recipeRepository = new RecipeRepository();
