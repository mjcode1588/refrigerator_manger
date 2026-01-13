// ===========================================
// 리포지토리 인터페이스 - Recipe
// ===========================================

import type { Recipe } from '../entities';

/**
 * Recipe 리포지토리 인터페이스
 */
export interface IRecipeRepository {
    /**
     * 냉장고 기반 레시피 추천
     */
    suggestRecipes(): Promise<Recipe[]>;

    /**
     * 레시피 상세 조회
     */
    getRecipeById(id: string): Promise<Recipe | null>;
}
