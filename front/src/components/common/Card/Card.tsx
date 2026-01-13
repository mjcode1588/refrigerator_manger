'use client';

import React from 'react';
import styles from './Card.module.css';
import { cn } from '@/lib/utils';

interface CardProps {
    children: React.ReactNode;
    className?: string;
    variant?: 'default' | 'glass' | 'outlined';
    hover?: boolean;
    onClick?: () => void;
}

export function Card({
    children,
    className,
    variant = 'default',
    hover = true,
    onClick,
}: CardProps) {
    return (
        <div
            className={cn(
                styles.card,
                styles[variant],
                hover && styles.hover,
                onClick && styles.clickable,
                className
            )}
            onClick={onClick}
        >
            {children}
        </div>
    );
}

interface CardHeaderProps {
    children: React.ReactNode;
    className?: string;
}

export function CardHeader({ children, className }: CardHeaderProps) {
    return <div className={cn(styles.header, className)}>{children}</div>;
}

interface CardTitleProps {
    children: React.ReactNode;
    className?: string;
}

export function CardTitle({ children, className }: CardTitleProps) {
    return <h3 className={cn(styles.title, className)}>{children}</h3>;
}

interface CardContentProps {
    children: React.ReactNode;
    className?: string;
}

export function CardContent({ children, className }: CardContentProps) {
    return <div className={cn(styles.content, className)}>{children}</div>;
}

interface CardFooterProps {
    children: React.ReactNode;
    className?: string;
}

export function CardFooter({ children, className }: CardFooterProps) {
    return <div className={cn(styles.footer, className)}>{children}</div>;
}
