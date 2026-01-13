'use client';

import React, { useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import styles from './page.module.css';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Input, Card, Badge, DDayBadge, LoadingState, ErrorState, EmptyState } from '@/components/common';
import { foodItemRepository } from '@/infrastructure';
import { GetItemsUseCase } from '@/application';
import { CATEGORY_LABELS, CATEGORY_ICONS, STORAGE_LABELS } from '@/types';
import type { FoodItem, FoodCategory, StorageLocation, ExpiryStatus } from '@/domain';
import { FiArrowLeft, FiSearch, FiFilter, FiPlus, FiGrid, FiList, FiTrash2, FiEdit2 } from 'react-icons/fi';

const getItemsUseCase = new GetItemsUseCase(foodItemRepository);

export default function ItemsPage() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const queryClient = useQueryClient();

    // 필터 상태
    const [search, setSearch] = useState('');
    const [category, setCategory] = useState<FoodCategory | 'all'>(
        (searchParams.get('category') as FoodCategory) || 'all'
    );
    const [storage, setStorage] = useState<StorageLocation | 'all'>('all');
    const [expiryStatus, setExpiryStatus] = useState<ExpiryStatus | 'all'>(
        searchParams.get('filter') === 'expiring' ? 'expiring' : 'all'
    );
    const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
    const [showFilters, setShowFilters] = useState(false);

    // 아이템 조회
    const { data, isLoading, error, refetch } = useQuery({
        queryKey: ['items', { search, category, storage, expiryStatus }],
        queryFn: () => getItemsUseCase.execute({
            filters: {
                search: search || undefined,
                category: category !== 'all' ? category : undefined,
                storageLocation: storage !== 'all' ? storage : undefined,
                expiryStatus: expiryStatus !== 'all' ? expiryStatus : undefined,
                sortBy: 'expiryDate',
                sortOrder: 'asc',
            },
        }),
    });

    // 삭제
    const { mutate: deleteItem, isPending: isDeleting } = useMutation({
        mutationFn: (id: string) => foodItemRepository.deleteItem(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['items'] });
        },
    });

    const handleDelete = (id: string, name: string) => {
        if (confirm(`'${name}'을(를) 삭제하시겠습니까?`)) {
            deleteItem(id);
        }
    };

    const clearFilters = () => {
        setSearch('');
        setCategory('all');
        setStorage('all');
        setExpiryStatus('all');
    };

    const hasActiveFilters = search || category !== 'all' || storage !== 'all' || expiryStatus !== 'all';

    return (
        <main className={styles.main}>
            {/* 헤더 */}
            <header className={styles.header}>
                <button onClick={() => router.back()} className={styles.backButton}>
                    <FiArrowLeft size={24} />
                </button>
                <h1 className={styles.title}>내 냉장고</h1>
                <Link href="/register/chat" className={styles.addButton}>
                    <FiPlus size={24} />
                </Link>
            </header>

            {/* 검색 & 필터 */}
            <div className={styles.searchSection}>
                <div className={styles.searchBox}>
                    <FiSearch className={styles.searchIcon} />
                    <input
                        type="text"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="식품 검색..."
                        className={styles.searchInput}
                    />
                </div>
                <button
                    onClick={() => setShowFilters(!showFilters)}
                    className={`${styles.filterButton} ${hasActiveFilters ? styles.active : ''}`}
                >
                    <FiFilter size={20} />
                </button>
                <div className={styles.viewToggle}>
                    <button
                        onClick={() => setViewMode('grid')}
                        className={viewMode === 'grid' ? styles.active : ''}
                    >
                        <FiGrid size={18} />
                    </button>
                    <button
                        onClick={() => setViewMode('list')}
                        className={viewMode === 'list' ? styles.active : ''}
                    >
                        <FiList size={18} />
                    </button>
                </div>
            </div>

            {/* 필터 패널 */}
            {showFilters && (
                <div className={styles.filterPanel}>
                    <div className={styles.filterGroup}>
                        <label className={styles.filterLabel}>카테고리</label>
                        <div className={styles.filterOptions}>
                            <button
                                onClick={() => setCategory('all')}
                                className={`${styles.filterChip} ${category === 'all' ? styles.active : ''}`}
                            >
                                전체
                            </button>
                            {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
                                <button
                                    key={key}
                                    onClick={() => setCategory(key as FoodCategory)}
                                    className={`${styles.filterChip} ${category === key ? styles.active : ''}`}
                                >
                                    {CATEGORY_ICONS[key as FoodCategory]} {label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className={styles.filterGroup}>
                        <label className={styles.filterLabel}>보관 위치</label>
                        <div className={styles.filterOptions}>
                            <button
                                onClick={() => setStorage('all')}
                                className={`${styles.filterChip} ${storage === 'all' ? styles.active : ''}`}
                            >
                                전체
                            </button>
                            {Object.entries(STORAGE_LABELS).map(([key, label]) => (
                                <button
                                    key={key}
                                    onClick={() => setStorage(key as StorageLocation)}
                                    className={`${styles.filterChip} ${storage === key ? styles.active : ''}`}
                                >
                                    {label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className={styles.filterGroup}>
                        <label className={styles.filterLabel}>유통기한 상태</label>
                        <div className={styles.filterOptions}>
                            <button
                                onClick={() => setExpiryStatus('all')}
                                className={`${styles.filterChip} ${expiryStatus === 'all' ? styles.active : ''}`}
                            >
                                전체
                            </button>
                            <button
                                onClick={() => setExpiryStatus('expired')}
                                className={`${styles.filterChip} ${styles.danger} ${expiryStatus === 'expired' ? styles.active : ''}`}
                            >
                                만료됨
                            </button>
                            <button
                                onClick={() => setExpiryStatus('expiring')}
                                className={`${styles.filterChip} ${styles.warning} ${expiryStatus === 'expiring' ? styles.active : ''}`}
                            >
                                임박
                            </button>
                            <button
                                onClick={() => setExpiryStatus('fresh')}
                                className={`${styles.filterChip} ${styles.success} ${expiryStatus === 'fresh' ? styles.active : ''}`}
                            >
                                신선
                            </button>
                        </div>
                    </div>

                    {hasActiveFilters && (
                        <Button variant="ghost" size="sm" onClick={clearFilters}>
                            필터 초기화
                        </Button>
                    )}
                </div>
            )}

            {/* 결과 */}
            <div className={styles.content}>
                {isLoading ? (
                    <LoadingState message="식품 목록을 불러오는 중..." />
                ) : error ? (
                    <ErrorState
                        message={(error as Error).message}
                        onRetry={() => refetch()}
                    />
                ) : data?.items.length === 0 ? (
                    <EmptyState
                        icon="🧊"
                        title="냉장고가 비어있어요"
                        description="식품을 등록해보세요!"
                        action={{
                            label: '식품 등록하기',
                            onClick: () => router.push('/register/chat'),
                        }}
                    />
                ) : (
                    <>
                        <div className={styles.resultInfo}>
                            <span>총 {data?.total || 0}개</span>
                        </div>

                        <div className={viewMode === 'grid' ? styles.itemsGrid : styles.itemsList}>
                            {data?.items.map(item => (
                                <ItemCard
                                    key={item.id}
                                    item={item}
                                    viewMode={viewMode}
                                    onDelete={() => handleDelete(item.id, item.name)}
                                />
                            ))}
                        </div>
                    </>
                )}
            </div>
        </main>
    );
}

// 아이템 카드 컴포넌트
interface ItemCardProps {
    item: FoodItem;
    viewMode: 'grid' | 'list';
    onDelete: () => void;
}

function ItemCard({ item, viewMode, onDelete }: ItemCardProps) {
    if (viewMode === 'list') {
        return (
            <div className={styles.itemListCard}>
                <div className={styles.itemIcon}>
                    {CATEGORY_ICONS[item.category]}
                </div>
                <div className={styles.itemInfo}>
                    <h3 className={styles.itemName}>{item.name}</h3>
                    <p className={styles.itemMeta}>
                        {item.quantity} {item.unit} · {STORAGE_LABELS[item.storageLocation]}
                    </p>
                </div>
                <DDayBadge expiryDate={item.expiryDate} />
                <div className={styles.itemActions}>
                    <Link href={`/items/${item.id}/edit`} className={styles.actionBtn}>
                        <FiEdit2 size={18} />
                    </Link>
                    <button onClick={onDelete} className={styles.actionBtn}>
                        <FiTrash2 size={18} />
                    </button>
                </div>
            </div>
        );
    }

    return (
        <Link href={`/items/${item.id}`} className={styles.itemGridCard}>
            <div className={styles.gridCardHeader}>
                <span className={styles.gridIcon}>{CATEGORY_ICONS[item.category]}</span>
                <DDayBadge expiryDate={item.expiryDate} />
            </div>
            <h3 className={styles.gridName}>{item.name}</h3>
            <p className={styles.gridMeta}>
                {item.quantity} {item.unit}
            </p>
            <Badge variant="default" size="sm">
                {STORAGE_LABELS[item.storageLocation]}
            </Badge>
        </Link>
    );
}
