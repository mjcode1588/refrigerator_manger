'use client';

import React from 'react';
import Link from 'next/link';
import styles from './page.module.css';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Button, Card, CardContent, DDayBadge, Badge, LoadingState, ErrorState, EmptyState } from '@/components/common';
import { foodItemRepository, ingestRepository, recipeRepository } from '@/infrastructure';
import { GetExpiringItemsUseCase, SuggestRecipesUseCase } from '@/application';
import { CATEGORY_ICONS, CATEGORY_LABELS } from '@/types';
import type { FoodItem, Recipe, FoodCategory } from '@/domain';
import { FiPlus, FiCamera, FiMessageSquare, FiChevronRight, FiClock, FiUsers, FiBell } from 'react-icons/fi';

// 유즈케이스 인스턴스
const getExpiringItemsUseCase = new GetExpiringItemsUseCase(foodItemRepository);
const suggestRecipesUseCase = new SuggestRecipesUseCase(recipeRepository);

export default function HomePage() {
  // 임박/만료 아이템 조회
  const {
    data: expiringData,
    isLoading: expiringLoading,
    error: expiringError,
    refetch: refetchExpiring
  } = useQuery({
    queryKey: ['expiring-items'],
    queryFn: () => getExpiringItemsUseCase.execute({ alertDays: 3 }),
  });

  // 레시피 추천
  const {
    mutate: suggestRecipes,
    data: recipesData,
    isPending: recipesLoading
  } = useMutation({
    mutationFn: () => suggestRecipesUseCase.execute(),
  });

  return (
    <main className={styles.main}>
      {/* 헤더 */}
      <header className={styles.header}>
        <div className={styles.headerContent}>
          <div className={styles.logo}>
            <span className={styles.logoIcon}>🧊</span>
            <h1 className={styles.logoText}>냉장고 관리</h1>
          </div>
          <div className={styles.headerActions}>
            <Link href="/notifications" className={styles.iconButton}>
              <FiBell size={22} />
              <span className={styles.notificationBadge}>2</span>
            </Link>
            <Link href="/family" className={styles.iconButton}>
              <FiUsers size={22} />
            </Link>
          </div>
        </div>
      </header>

      {/* 히어로 섹션 */}
      <section className={styles.hero}>
        <div className={styles.heroBackground} />
        <div className={styles.heroContent}>
          <h2 className={styles.heroTitle}>
            오늘의 <span className={styles.highlight}>냉장고</span> 상태
          </h2>
          <p className={styles.heroSubtitle}>
            AI가 관리하는 스마트한 식품 관리
          </p>

          {/* 통계 카드 */}
          <div className={styles.statsGrid}>
            <div className={styles.statCard}>
              <span className={styles.statIcon}>⚠️</span>
              <div className={styles.statInfo}>
                <span className={styles.statValue}>
                  {expiringData?.expiredCount || 0}
                </span>
                <span className={styles.statLabel}>만료됨</span>
              </div>
            </div>
            <div className={styles.statCard}>
              <span className={styles.statIcon}>⏰</span>
              <div className={styles.statInfo}>
                <span className={styles.statValue}>
                  {expiringData?.expiringCount || 0}
                </span>
                <span className={styles.statLabel}>임박</span>
              </div>
            </div>
            <div className={styles.statCard}>
              <span className={styles.statIcon}>🍳</span>
              <div className={styles.statInfo}>
                <span className={styles.statValue}>
                  {recipesData?.fullMatchCount || '-'}
                </span>
                <span className={styles.statLabel}>추천 레시피</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 빠른 등록 섹션 */}
      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>빠른 등록</h3>
        <div className={styles.quickActions}>
          <Link href="/register/chat" className={styles.quickActionCard}>
            <div className={styles.quickActionIcon}>
              <FiMessageSquare size={28} />
            </div>
            <div className={styles.quickActionInfo}>
              <h4>채팅 등록</h4>
              <p>텍스트로 간편하게</p>
            </div>
            <FiChevronRight className={styles.quickActionArrow} />
          </Link>

          <Link href="/register/image" className={styles.quickActionCard}>
            <div className={styles.quickActionIcon}>
              <FiCamera size={28} />
            </div>
            <div className={styles.quickActionInfo}>
              <h4>이미지 등록</h4>
              <p>영수증/식품 사진으로</p>
            </div>
            <FiChevronRight className={styles.quickActionArrow} />
          </Link>
        </div>
      </section>

      {/* 임박/만료 아이템 섹션 */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>주의가 필요한 식품</h3>
          <Link href="/items?filter=expiring" className={styles.seeAllLink}>
            모두 보기 <FiChevronRight size={16} />
          </Link>
        </div>

        {expiringLoading ? (
          <LoadingState message="불러오는 중..." />
        ) : expiringError ? (
          <ErrorState
            message="데이터를 불러올 수 없습니다."
            onRetry={() => refetchExpiring()}
          />
        ) : expiringData?.totalCount === 0 ? (
          <EmptyState
            icon="✨"
            title="모든 식품이 신선해요!"
            description="유통기한이 임박한 식품이 없습니다."
          />
        ) : (
          <div className={styles.expiringList}>
            {expiringData?.groups.map(group => (
              <div key={group.status} className={styles.expiringGroup}>
                <Badge
                  variant={group.status === 'expired' ? 'danger' : 'warning'}
                  className={styles.groupBadge}
                >
                  {group.label} ({group.count})
                </Badge>
                <div className={styles.itemsGrid}>
                  {group.items.slice(0, 4).map(item => (
                    <ExpiringItemCard key={item.id} item={item} />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 레시피 추천 섹션 */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>오늘의 추천 레시피</h3>
          <Link href="/recipes" className={styles.seeAllLink}>
            모두 보기 <FiChevronRight size={16} />
          </Link>
        </div>

        {!recipesData && !recipesLoading ? (
          <div className={styles.recipeCTA}>
            <p>냉장고 재료로 만들 수 있는 레시피를 추천받아보세요!</p>
            <Button onClick={() => suggestRecipes()} isLoading={recipesLoading}>
              레시피 추천받기
            </Button>
          </div>
        ) : recipesLoading ? (
          <LoadingState message="레시피 추천 중..." />
        ) : (
          <div className={styles.recipeGrid}>
            {recipesData?.recipes.slice(0, 3).map(recipe => (
              <RecipeCard key={recipe.id} recipe={recipe} />
            ))}
          </div>
        )}
      </section>

      {/* 카테고리 바로가기 */}
      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>카테고리별 보기</h3>
        <div className={styles.categoryGrid}>
          {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
            <Link
              key={key}
              href={`/items?category=${key}`}
              className={styles.categoryItem}
            >
              <span className={styles.categoryIcon}>
                {CATEGORY_ICONS[key as FoodCategory]}
              </span>
              <span className={styles.categoryLabel}>{label}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* 플로팅 추가 버튼 */}
      <Link href="/register/chat" className={styles.fab}>
        <FiPlus size={28} />
      </Link>
    </main>
  );
}

// 임박 아이템 카드 컴포넌트
function ExpiringItemCard({ item }: { item: FoodItem }) {
  return (
    <Link href={`/items/${item.id}`} className={styles.expiringCard}>
      <div className={styles.expiringCardIcon}>
        {CATEGORY_ICONS[item.category]}
      </div>
      <div className={styles.expiringCardInfo}>
        <h4 className={styles.expiringCardName}>{item.name}</h4>
        <p className={styles.expiringCardMeta}>
          {item.quantity} {item.unit}
        </p>
      </div>
      <DDayBadge expiryDate={item.expiryDate} />
    </Link>
  );
}

// 레시피 카드 컴포넌트
function RecipeCard({ recipe }: { recipe: Recipe }) {
  const coverage = recipe.getIngredientCoverage();

  return (
    <Link href={`/recipes/${recipe.id}`} className={styles.recipeCard}>
      <div className={styles.recipeCardImage}>
        {recipe.imageUrl ? (
          <img src={recipe.imageUrl} alt={recipe.name} />
        ) : (
          <span className={styles.recipeCardPlaceholder}>🍳</span>
        )}
        <div className={styles.recipeCardCoverage}>
          <span>{coverage}%</span>
        </div>
      </div>
      <div className={styles.recipeCardContent}>
        <h4 className={styles.recipeCardTitle}>{recipe.name}</h4>
        <div className={styles.recipeCardMeta}>
          <span><FiClock size={14} /> {recipe.getFormattedCookingTime()}</span>
          <Badge variant={recipe.hasAllIngredients() ? 'success' : 'warning'} size="sm">
            {recipe.hasAllIngredients() ? '재료 완비' : `${recipe.missingItems.length}개 부족`}
          </Badge>
        </div>
      </div>
    </Link>
  );
}
