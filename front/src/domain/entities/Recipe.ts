// ===========================================
// 도메인 엔티티 - Recipe
// ===========================================

export type RecipeDifficulty = 'easy' | 'medium' | 'hard';

export interface RecipeIngredient {
    name: string;
    quantity: string;
}

export interface RecipeProps {
    id: string;
    name: string;
    description: string;
    imageUrl?: string;
    cookingTime: number; // 분 단위
    difficulty: RecipeDifficulty;
    servings: number;
    useItems: RecipeIngredient[];
    missingItems: RecipeIngredient[];
    instructions: string[];
}

/**
 * Recipe 도메인 엔티티
 */
export class Recipe {
    private constructor(private readonly props: RecipeProps) { }

    static create(props: RecipeProps): Recipe {
        Recipe.validate(props);
        return new Recipe(props);
    }

    private static validate(props: RecipeProps): void {
        if (!props.name || props.name.trim() === '') {
            throw new Error('레시피명은 필수입니다.');
        }
        if (props.cookingTime < 0) {
            throw new Error('조리 시간은 0 이상이어야 합니다.');
        }
        if (props.servings < 1) {
            throw new Error('인분 수는 1 이상이어야 합니다.');
        }
    }

    // Getters
    get id(): string { return this.props.id; }
    get name(): string { return this.props.name; }
    get description(): string { return this.props.description; }
    get imageUrl(): string | undefined { return this.props.imageUrl; }
    get cookingTime(): number { return this.props.cookingTime; }
    get difficulty(): RecipeDifficulty { return this.props.difficulty; }
    get servings(): number { return this.props.servings; }
    get useItems(): RecipeIngredient[] { return [...this.props.useItems]; }
    get missingItems(): RecipeIngredient[] { return [...this.props.missingItems]; }
    get instructions(): string[] { return [...this.props.instructions]; }

    /**
     * 보유 재료 비율 (0-100)
     */
    getIngredientCoverage(): number {
        const total = this.props.useItems.length + this.props.missingItems.length;
        if (total === 0) return 100;
        return Math.round((this.props.useItems.length / total) * 100);
    }

    /**
     * 모든 재료 보유 여부
     */
    hasAllIngredients(): boolean {
        return this.props.missingItems.length === 0;
    }

    /**
     * 난이도 텍스트
     */
    getDifficultyText(): string {
        const labels: Record<RecipeDifficulty, string> = {
            easy: '쉬움',
            medium: '보통',
            hard: '어려움',
        };
        return labels[this.props.difficulty];
    }

    /**
     * 조리 시간 포맷팅
     */
    getFormattedCookingTime(): string {
        if (this.props.cookingTime < 60) {
            return `${this.props.cookingTime}분`;
        }
        const hours = Math.floor(this.props.cookingTime / 60);
        const minutes = this.props.cookingTime % 60;
        return minutes > 0 ? `${hours}시간 ${minutes}분` : `${hours}시간`;
    }

    toDTO(): RecipeProps {
        return { ...this.props };
    }
}
