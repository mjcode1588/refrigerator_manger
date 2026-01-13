// ===========================================
// 유즈케이스 - 레시피 추천
// ===========================================

import type { Recipe } from '@/domain/entities';
import type { IRecipeRepository } from '@/domain/repositories';

export interface SuggestRecipesOutput {
    recipes: Recipe[];
    totalCount: number;
    fullMatchCount: number; // 모든 재료 보유
}

/**
 * 레시피 추천 유즈케이스
 */
export class SuggestRecipesUseCase {
    constructor(private readonly repository: IRecipeRepository) { }

    async execute(): Promise<SuggestRecipesOutput> {
        const recipes = await this.repository.suggestRecipes();

        // 재료 보유율 기준 정렬 (높은 순)
        const sortedRecipes = [...recipes].sort(
            (a, b) => b.getIngredientCoverage() - a.getIngredientCoverage()
        );

        const fullMatchCount = recipes.filter(r => r.hasAllIngredients()).length;

        return {
            recipes: sortedRecipes,
            totalCount: recipes.length,
            fullMatchCount,
        };
    }
}
