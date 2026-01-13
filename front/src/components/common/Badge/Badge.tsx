'use client';

import React from 'react';
import styles from './Badge.module.css';
import { cn } from '@/lib/utils';
import { getDDay } from '@/lib/utils';

interface BadgeProps {
    children: React.ReactNode;
    variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'primary';
    size?: 'sm' | 'md';
    className?: string;
}

export function Badge({
    children,
    variant = 'default',
    size = 'md',
    className,
}: BadgeProps) {
    return (
        <span className={cn(styles.badge, styles[variant], styles[size], className)}>
            {children}
        </span>
    );
}

interface DDayBadgeProps {
    expiryDate: string;
    className?: string;
}

export function DDayBadge({ expiryDate, className }: DDayBadgeProps) {
    const { text, status } = getDDay(expiryDate);

    return (
        <span className={cn(styles.ddayBadge, styles[`dday-${status}`], className)}>
            {text}
        </span>
    );
}
