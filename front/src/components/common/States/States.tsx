'use client';

import React from 'react';
import styles from './States.module.css';
import { cn } from '@/lib/utils';
import { Button } from '../Button';

// 로딩 스피너
interface SpinnerProps {
    size?: 'sm' | 'md' | 'lg';
    className?: string;
}

export function Spinner({ size = 'md', className }: SpinnerProps) {
    return <div className={cn(styles.spinner, styles[`spinner-${size}`], className)} />;
}

// 로딩 상태
interface LoadingStateProps {
    message?: string;
    className?: string;
}

export function LoadingState({ message = '로딩 중...', className }: LoadingStateProps) {
    return (
        <div className={cn(styles.stateContainer, className)}>
            <Spinner size="lg" />
            <p className={styles.stateText}>{message}</p>
        </div>
    );
}

// 에러 상태
interface ErrorStateProps {
    title?: string;
    message?: string;
    onRetry?: () => void;
    className?: string;
}

export function ErrorState({
    title = '오류가 발생했습니다',
    message = '잠시 후 다시 시도해 주세요.',
    onRetry,
    className,
}: ErrorStateProps) {
    return (
        <div className={cn(styles.stateContainer, styles.errorState, className)}>
            <span className={styles.stateIcon}>⚠️</span>
            <h3 className={styles.stateTitle}>{title}</h3>
            <p className={styles.stateText}>{message}</p>
            {onRetry && (
                <Button variant="secondary" onClick={onRetry}>
                    다시 시도
                </Button>
            )}
        </div>
    );
}

// 빈 상태
interface EmptyStateProps {
    icon?: string;
    title: string;
    description?: string;
    action?: {
        label: string;
        onClick: () => void;
    };
    className?: string;
}

export function EmptyState({
    icon = '📭',
    title,
    description,
    action,
    className,
}: EmptyStateProps) {
    return (
        <div className={cn(styles.stateContainer, className)}>
            <span className={styles.stateIcon}>{icon}</span>
            <h3 className={styles.stateTitle}>{title}</h3>
            {description && <p className={styles.stateText}>{description}</p>}
            {action && (
                <Button variant="primary" onClick={action.onClick}>
                    {action.label}
                </Button>
            )}
        </div>
    );
}

// 스켈레톤
interface SkeletonProps {
    width?: string | number;
    height?: string | number;
    borderRadius?: string;
    className?: string;
}

export function Skeleton({
    width = '100%',
    height = '20px',
    borderRadius = 'var(--radius-sm)',
    className,
}: SkeletonProps) {
    return (
        <div
            className={cn(styles.skeleton, className)}
            style={{
                width: typeof width === 'number' ? `${width}px` : width,
                height: typeof height === 'number' ? `${height}px` : height,
                borderRadius,
            }}
        />
    );
}

// 카드 스켈레톤
export function CardSkeleton({ className }: { className?: string }) {
    return (
        <div className={cn(styles.cardSkeleton, className)}>
            <Skeleton height={48} width={48} borderRadius="var(--radius-md)" />
            <div className={styles.cardSkeletonContent}>
                <Skeleton height={20} width="60%" />
                <Skeleton height={16} width="40%" />
            </div>
        </div>
    );
}
