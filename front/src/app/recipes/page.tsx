'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import styles from './page.module.css';
import { useMutation } from '@tanstack/react-query';
import { Button, Card, Badge, LoadingState, ErrorState, EmptyState } from '@/components/common';
import { recipeRepository } from '@/infrastructure';
import { SuggestRecipesUseCase } from '@/application';
import type { Recipe } from '@/domain';
import { FiArrowLeft, FiClock, FiUsers, FiChevronDown, FiChevronUp, FiShoppingCart, FiCheckCircle, FiAlertCircle } from 'react-icons/fi';

const suggestRecipesUseCase = new SuggestRecipesUseCase(recipeRepository);

export default function RecipesPage() {
    const router = useRouter();
    const [expandedRecipe, setExpandedRecipe] = useState<string | null>(null);
    const [shoppingList, setShoppingList] = useState<{ name: string; quantity: string }[]>([]);

    // 레시피 추천
    const {
        mutate: suggest,
        data,
        isPending,
        error,
        reset
    } = useMutation({
        mutationFn: () => suggestRecipesUseCase.execute(),
    });

    // 페이지 로드시 자동 추천
    useEffect(() => {
        suggest();
    }, []);

    // 장보기 리스트에 추가
    const addToShoppingList = (items: { name: string; quantity: string }[]) => {
        const existingNames = new Set(shoppingList.map(item => item.name));
        const newItems = items.filter(item => !existingNames.has(item.name));
        setShoppingList(prev => [...prev, ...newItems]);
    };

    // 장보기 리스트에서 제거
    const removeFromShoppingList = (name: string) => {
        setShoppingList(prev => prev.filter(item => item.name !== name));
    };

    const toggleRecipe = (id: string) => {
        setExpandedRecipe(prev => prev === id ? null : id);
    };

    const getDifficultyColor = (difficulty: string) => {
        switch (difficulty) {
            case 'easy': return 'success';
            case 'medium': return 'warning';
            case 'hard': return 'danger';
            default: return 'default';
        }
    };

    return (
        <main className={styles.main}>
            {/* 헤더 */}
            <header className={styles.header}>
                <button onClick={() => router.back()} className={styles.backButton}>
                    <FiArrowLeft size={24} />
                </button>
                <h1 className={styles.title}>레시피 추천</h1>
                <div className={styles.spacer} />
            </header>

            <div className={styles.content}>
                {/* 통계 */}
                {data && (
                    <div className={styles.stats}>
                        <div className={styles.statItem}>
                            <span className={styles.statValue}>{data.totalCount}</span>
                            <span className={styles.statLabel}>추천 레시피</span>
                        </div>
                        <div className={styles.statDivider} />
                        <div className={styles.statItem}>
                            <span className={styles.statValue}>{data.fullMatchCount}</span>
                            <span className={styles.statLabel}>바로 조리 가능</span>
                        </div>
                    </div>
                )}

                {/* 로딩/에러/빈 상태 */}
                {isPending && <LoadingState message="냉장고 재료로 레시피를 찾고 있어요..." />}

                {error && (
                    <ErrorState
                        message={(error as Error).message}
                        onRetry={() => suggest()}
                    />
                )}

                {data?.recipes.length === 0 && (
                    <EmptyState
                        icon="🍳"
                        title="추천할 레시피가 없어요"
                        description="냉장고에 재료를 더 추가해보세요!"
                    />
                )}

                {/* 레시피 리스트 */}
                {data?.recipes && data.recipes.length > 0 && (
                    <div className={styles.recipeList}>
                        {data.recipes.map(recipe => (
                            <RecipeCard
                                key={recipe.id}
                                recipe={recipe}
                                isExpanded={expandedRecipe === recipe.id}
                                onToggle={() => toggleRecipe(recipe.id)}
                                onAddToShoppingList={addToShoppingList}
                                getDifficultyColor={getDifficultyColor}
                            />
                        ))}
                    </div>
                )}

                {/* 장보기 리스트 */}
                {shoppingList.length > 0 && (
                    <div className={styles.shoppingListSection}>
                        <div className={styles.shoppingListHeader}>
                            <FiShoppingCart size={20} />
                            <h3>장보기 리스트</h3>
                            <Badge variant="primary">{shoppingList.length}개</Badge>
                        </div>
                        <div className={styles.shoppingListItems}>
                            {shoppingList.map((item, index) => (
                                <div key={index} className={styles.shoppingItem}>
                                    <span>{item.name}</span>
                                    <span className={styles.shoppingItemQty}>{item.quantity}</span>
                                    <button
                                        onClick={() => removeFromShoppingList(item.name)}
                                        className={styles.removeBtn}
                                    >
                                        ✕
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </main>
    );
}

// 레시피 카드 컴포넌트
interface RecipeCardProps {
    recipe: Recipe;
    isExpanded: boolean;
    onToggle: () => void;
    onAddToShoppingList: (items: { name: string; quantity: string }[]) => void;
    getDifficultyColor: (difficulty: string) => 'success' | 'warning' | 'danger' | 'default';
}

function RecipeCard({
    recipe,
    isExpanded,
    onToggle,
    onAddToShoppingList,
    getDifficultyColor
}: RecipeCardProps) {
    const coverage = recipe.getIngredientCoverage();

    return (
        <div className={styles.recipeCard}>
            <div className={styles.recipeCardHeader} onClick={onToggle}>
                <div className={styles.recipeImage}>
                    {recipe.imageUrl ? (
                        <img src={recipe.imageUrl} alt={recipe.name} />
                    ) : (
                        <span className={styles.recipePlaceholder}>🍳</span>
                    )}
                    <div className={styles.coverageBadge}>
                        <span>{coverage}%</span>
                    </div>
                </div>

                <div className={styles.recipeInfo}>
                    <h3 className={styles.recipeName}>{recipe.name}</h3>
                    <p className={styles.recipeDesc}>{recipe.description}</p>
                    <div className={styles.recipeMeta}>
                        <span><FiClock size={14} /> {recipe.getFormattedCookingTime()}</span>
                        <span><FiUsers size={14} /> {recipe.servings}인분</span>
                        <Badge variant={getDifficultyColor(recipe.difficulty)} size="sm">
                            {recipe.getDifficultyText()}
                        </Badge>
                    </div>
                </div>

                <button className={styles.expandBtn}>
                    {isExpanded ? <FiChevronUp size={24} /> : <FiChevronDown size={24} />}
                </button>
            </div>

            {isExpanded && (
                <div className={styles.recipeCardBody}>
                    {/* 재료 섹션 */}
                    <div className={styles.ingredientsSection}>
                        <div className={styles.ingredientColumn}>
                            <h4 className={styles.ingredientTitle}>
                                <FiCheckCircle className={styles.iconSuccess} />
                                보유 재료 ({recipe.useItems.length})
                            </h4>
                            <ul className={styles.ingredientList}>
                                {recipe.useItems.map((item, idx) => (
                                    <li key={idx} className={styles.ingredientItem}>
                                        <span>{item.name}</span>
                                        <span className={styles.ingredientQty}>{item.quantity}</span>
                                    </li>
                                ))}
                            </ul>
                        </div>

                        {recipe.missingItems.length > 0 && (
                            <div className={styles.ingredientColumn}>
                                <h4 className={styles.ingredientTitle}>
                                    <FiAlertCircle className={styles.iconWarning} />
                                    부족 재료 ({recipe.missingItems.length})
                                </h4>
                                <ul className={styles.ingredientList}>
                                    {recipe.missingItems.map((item, idx) => (
                                        <li key={idx} className={`${styles.ingredientItem} ${styles.missing}`}>
                                            <span>{item.name}</span>
                                            <span className={styles.ingredientQty}>{item.quantity}</span>
                                        </li>
                                    ))}
                                </ul>
                                <Button
                                    variant="secondary"
                                    size="sm"
                                    onClick={() => onAddToShoppingList(recipe.missingItems)}
                                    leftIcon={<FiShoppingCart />}
                                >
                                    장보기 리스트에 추가
                                </Button>
                            </div>
                        )}
                    </div>

                    {/* 조리 순서 */}
                    <div className={styles.instructionsSection}>
                        <h4 className={styles.sectionTitle}>조리 순서</h4>
                        <ol className={styles.instructions}>
                            {recipe.instructions.map((step, idx) => (
                                <li key={idx} className={styles.instructionStep}>
                                    <span className={styles.stepNumber}>{idx + 1}</span>
                                    <span>{step}</span>
                                </li>
                            ))}
                        </ol>
                    </div>
                </div>
            )}
        </div>
    );
}
