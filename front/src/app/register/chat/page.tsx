'use client';

import React, { useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import styles from './page.module.css';
import { useMutation } from '@tanstack/react-query';
import { Button, Input, Card, Badge, LoadingState, ErrorState, EmptyState } from '@/components/common';
import { ingestRepository } from '@/infrastructure';
import { IngestItemsUseCase, ConfirmItemsUseCase } from '@/application';
import { CandidateItem } from '@/domain';
import type { FoodCategory, StorageLocation } from '@/domain';
import { CATEGORY_LABELS, CATEGORY_ICONS, STORAGE_LABELS } from '@/types';
import { FiArrowLeft, FiSend, FiImage, FiCheck, FiX, FiEdit2, FiPlus, FiTrash2 } from 'react-icons/fi';

// 유즈케이스 인스턴스
const ingestItemsUseCase = new IngestItemsUseCase(ingestRepository);
const confirmItemsUseCase = new ConfirmItemsUseCase(ingestRepository);

type Step = 'input' | 'review' | 'complete';

export default function ChatRegisterPage() {
    const router = useRouter();
    const fileInputRef = useRef<HTMLInputElement>(null);

    // 상태
    const [step, setStep] = useState<Step>('input');
    const [text, setText] = useState('');
    const [images, setImages] = useState<File[]>([]);
    const [candidates, setCandidates] = useState<CandidateItem[]>([]);
    const [editingId, setEditingId] = useState<string | null>(null);

    // 인식 mutation
    const { mutate: ingest, isPending: isIngesting, error: ingestError } = useMutation({
        mutationFn: () => ingestItemsUseCase.execute({ text, images }),
        onSuccess: (data) => {
            setCandidates(data.candidates);
            setStep('review');
        },
    });

    // 확정 mutation
    const { mutate: confirm, isPending: isConfirming } = useMutation({
        mutationFn: () => confirmItemsUseCase.execute({ candidates }),
        onSuccess: (data) => {
            setStep('complete');
        },
    });

    // 이미지 선택 핸들러
    const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (files) {
            setImages(prev => [...prev, ...Array.from(files)]);
        }
    };

    // 이미지 제거
    const removeImage = (index: number) => {
        setImages(prev => prev.filter((_, i) => i !== index));
    };

    // 후보 토글
    const toggleCandidate = (id: string) => {
        setCandidates(prev =>
            prev.map(c => c.id === id ? c.toggleSelection() : c)
        );
    };

    // 후보 업데이트
    const updateCandidate = (id: string, updates: Partial<CandidateItem>) => {
        setCandidates(prev =>
            prev.map(c => c.id === id ? c.update(updates) : c)
        );
        setEditingId(null);
    };

    // 후보 삭제
    const removeCandidate = (id: string) => {
        setCandidates(prev => prev.filter(c => c.id !== id));
    };

    // 후보 추가
    const addCandidate = () => {
        const newCandidate = CandidateItem.create({
            id: `new-${Date.now()}`,
            name: '',
            category: 'etc',
            quantity: 1,
            unit: '개',
            expiryDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
            storageLocation: 'fridge',
            confidence: 1,
            selected: true,
        });
        setCandidates(prev => [...prev, newCandidate]);
        setEditingId(newCandidate.id);
    };

    // 전체 선택/해제
    const toggleAll = (selected: boolean) => {
        setCandidates(prev => prev.map(c => c.setSelected(selected)));
    };

    const selectedCount = candidates.filter(c => c.selected).length;

    return (
        <main className={styles.main}>
            {/* 헤더 */}
            <header className={styles.header}>
                <button onClick={() => router.back()} className={styles.backButton}>
                    <FiArrowLeft size={24} />
                </button>
                <h1 className={styles.title}>
                    {step === 'input' && '채팅으로 등록'}
                    {step === 'review' && '확인 및 수정'}
                    {step === 'complete' && '등록 완료'}
                </h1>
                <div className={styles.spacer} />
            </header>

            {/* 단계 표시 */}
            <div className={styles.steps}>
                <div className={`${styles.step} ${step === 'input' ? styles.active : ''} ${step !== 'input' ? styles.completed : ''}`}>
                    <span className={styles.stepNumber}>1</span>
                    <span className={styles.stepLabel}>입력</span>
                </div>
                <div className={styles.stepLine} />
                <div className={`${styles.step} ${step === 'review' ? styles.active : ''} ${step === 'complete' ? styles.completed : ''}`}>
                    <span className={styles.stepNumber}>2</span>
                    <span className={styles.stepLabel}>확인</span>
                </div>
                <div className={styles.stepLine} />
                <div className={`${styles.step} ${step === 'complete' ? styles.active : ''}`}>
                    <span className={styles.stepNumber}>3</span>
                    <span className={styles.stepLabel}>완료</span>
                </div>
            </div>

            {/* 입력 단계 */}
            {step === 'input' && (
                <div className={styles.content}>
                    <div className={styles.inputSection}>
                        <p className={styles.hint}>
                            💡 "우유 2개, 계란 1판, 돼지고기 500g" 처럼 자유롭게 입력하세요.
                            영수증이나 식품 사진을 첨부하면 더 정확해요!
                        </p>

                        {/* 텍스트 입력 */}
                        <div className={styles.textInputWrapper}>
                            <textarea
                                value={text}
                                onChange={(e) => setText(e.target.value)}
                                placeholder="식품명, 수량 등을 입력하세요..."
                                className={styles.textInput}
                                rows={4}
                            />
                        </div>

                        {/* 이미지 첨부 */}
                        <div className={styles.imageSection}>
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept="image/*"
                                multiple
                                onChange={handleImageSelect}
                                className={styles.hiddenInput}
                            />

                            <div className={styles.imageGrid}>
                                {images.map((image, index) => (
                                    <div key={index} className={styles.imagePreview}>
                                        <img src={URL.createObjectURL(image)} alt={`첨부 ${index + 1}`} />
                                        <button
                                            onClick={() => removeImage(index)}
                                            className={styles.removeImageBtn}
                                        >
                                            <FiX size={16} />
                                        </button>
                                    </div>
                                ))}

                                <button
                                    onClick={() => fileInputRef.current?.click()}
                                    className={styles.addImageBtn}
                                >
                                    <FiImage size={24} />
                                    <span>사진 추가</span>
                                </button>
                            </div>
                        </div>

                        {ingestError && (
                            <div className={styles.errorMessage}>
                                {(ingestError as Error).message}
                            </div>
                        )}
                    </div>

                    <div className={styles.footer}>
                        <Button
                            onClick={() => ingest()}
                            isLoading={isIngesting}
                            disabled={!text && images.length === 0}
                            size="lg"
                            fullWidth
                            rightIcon={<FiSend />}
                        >
                            {isIngesting ? 'AI가 분석 중...' : '식품 인식하기'}
                        </Button>
                    </div>
                </div>
            )}

            {/* 확인/수정 단계 (휴먼 인 더 루프) */}
            {step === 'review' && (
                <div className={styles.content}>
                    <div className={styles.reviewHeader}>
                        <p className={styles.reviewHint}>
                            🔍 AI가 인식한 결과입니다. 확인 후 수정하거나 삭제할 수 있어요.
                        </p>

                        <div className={styles.reviewActions}>
                            <span className={styles.selectedCount}>
                                {selectedCount}개 선택됨
                            </span>
                            <button
                                onClick={() => toggleAll(true)}
                                className={styles.textButton}
                            >
                                전체 선택
                            </button>
                            <button
                                onClick={() => toggleAll(false)}
                                className={styles.textButton}
                            >
                                전체 해제
                            </button>
                        </div>
                    </div>

                    <div className={styles.candidateList}>
                        {candidates.length === 0 ? (
                            <EmptyState
                                icon="🤔"
                                title="인식된 식품이 없습니다"
                                description="텍스트나 이미지에서 식품을 찾지 못했어요."
                                action={{
                                    label: '다시 시도',
                                    onClick: () => setStep('input'),
                                }}
                            />
                        ) : (
                            candidates.map((candidate) => (
                                <CandidateCard
                                    key={candidate.id}
                                    candidate={candidate}
                                    isEditing={editingId === candidate.id}
                                    onToggle={() => toggleCandidate(candidate.id)}
                                    onEdit={() => setEditingId(candidate.id)}
                                    onUpdate={(updates) => updateCandidate(candidate.id, updates)}
                                    onDelete={() => removeCandidate(candidate.id)}
                                    onCancelEdit={() => setEditingId(null)}
                                />
                            ))
                        )}

                        {/* 수동 추가 버튼 */}
                        <button onClick={addCandidate} className={styles.addCandidateBtn}>
                            <FiPlus size={20} />
                            <span>항목 직접 추가</span>
                        </button>
                    </div>

                    <div className={styles.footer}>
                        <Button
                            variant="secondary"
                            onClick={() => setStep('input')}
                            size="lg"
                        >
                            이전
                        </Button>
                        <Button
                            onClick={() => confirm()}
                            isLoading={isConfirming}
                            disabled={selectedCount === 0}
                            size="lg"
                            rightIcon={<FiCheck />}
                        >
                            {selectedCount}개 저장하기
                        </Button>
                    </div>
                </div>
            )}

            {/* 완료 단계 */}
            {step === 'complete' && (
                <div className={styles.content}>
                    <div className={styles.completeSection}>
                        <div className={styles.completeIcon}>✅</div>
                        <h2 className={styles.completeTitle}>등록 완료!</h2>
                        <p className={styles.completeMessage}>
                            {selectedCount}개 식품이 냉장고에 추가되었습니다.
                        </p>

                        <div className={styles.completeActions}>
                            <Button variant="secondary" onClick={() => {
                                setStep('input');
                                setText('');
                                setImages([]);
                                setCandidates([]);
                            }}>
                                더 등록하기
                            </Button>
                            <Link href="/items">
                                <Button>목록 보기</Button>
                            </Link>
                        </div>
                    </div>
                </div>
            )}
        </main>
    );
}

// 후보 아이템 카드 컴포넌트
interface CandidateCardProps {
    candidate: CandidateItem;
    isEditing: boolean;
    onToggle: () => void;
    onEdit: () => void;
    onUpdate: (updates: Partial<CandidateItem>) => void;
    onDelete: () => void;
    onCancelEdit: () => void;
}

function CandidateCard({
    candidate,
    isEditing,
    onToggle,
    onEdit,
    onUpdate,
    onDelete,
    onCancelEdit,
}: CandidateCardProps) {
    const [editForm, setEditForm] = useState({
        name: candidate.name,
        category: candidate.category,
        quantity: candidate.quantity,
        unit: candidate.unit,
        expiryDate: candidate.expiryDate,
        storageLocation: candidate.storageLocation,
    });

    const handleSave = () => {
        if (!editForm.name.trim()) return;
        onUpdate(editForm);
    };

    const confidenceLevel = candidate.getConfidenceLevel();

    if (isEditing) {
        return (
            <div className={`${styles.candidateCard} ${styles.editing}`}>
                <div className={styles.editForm}>
                    <div className={styles.editRow}>
                        <Input
                            label="식품명"
                            value={editForm.name}
                            onChange={(e) => setEditForm(prev => ({ ...prev, name: e.target.value }))}
                            placeholder="식품명 입력"
                        />
                    </div>

                    <div className={styles.editRow}>
                        <div className={styles.editField}>
                            <label className={styles.editLabel}>카테고리</label>
                            <select
                                value={editForm.category}
                                onChange={(e) => setEditForm(prev => ({ ...prev, category: e.target.value as FoodCategory }))}
                                className={styles.editSelect}
                            >
                                {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
                                    <option key={key} value={key}>
                                        {CATEGORY_ICONS[key as FoodCategory]} {label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className={styles.editField}>
                            <label className={styles.editLabel}>보관 위치</label>
                            <select
                                value={editForm.storageLocation}
                                onChange={(e) => setEditForm(prev => ({ ...prev, storageLocation: e.target.value as StorageLocation }))}
                                className={styles.editSelect}
                            >
                                {Object.entries(STORAGE_LABELS).map(([key, label]) => (
                                    <option key={key} value={key}>{label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className={styles.editRow}>
                        <div className={styles.editField}>
                            <label className={styles.editLabel}>수량</label>
                            <div className={styles.quantityInput}>
                                <input
                                    type="number"
                                    min="1"
                                    value={editForm.quantity}
                                    onChange={(e) => setEditForm(prev => ({ ...prev, quantity: parseInt(e.target.value) || 1 }))}
                                    className={styles.editInput}
                                />
                                <input
                                    type="text"
                                    value={editForm.unit}
                                    onChange={(e) => setEditForm(prev => ({ ...prev, unit: e.target.value }))}
                                    placeholder="단위"
                                    className={styles.editInput}
                                />
                            </div>
                        </div>

                        <div className={styles.editField}>
                            <label className={styles.editLabel}>유통기한</label>
                            <input
                                type="date"
                                value={editForm.expiryDate}
                                onChange={(e) => setEditForm(prev => ({ ...prev, expiryDate: e.target.value }))}
                                className={styles.editInput}
                            />
                        </div>
                    </div>

                    <div className={styles.editActions}>
                        <Button variant="ghost" onClick={onCancelEdit}>
                            취소
                        </Button>
                        <Button onClick={handleSave}>
                            저장
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className={`${styles.candidateCard} ${!candidate.selected ? styles.unselected : ''}`}>
            <button
                className={`${styles.checkbox} ${candidate.selected ? styles.checked : ''}`}
                onClick={onToggle}
            >
                {candidate.selected && <FiCheck size={14} />}
            </button>

            <div className={styles.candidateIcon}>
                {CATEGORY_ICONS[candidate.category]}
            </div>

            <div className={styles.candidateInfo}>
                <div className={styles.candidateName}>
                    {candidate.name}
                    <Badge
                        variant={confidenceLevel === 'high' ? 'success' : confidenceLevel === 'medium' ? 'warning' : 'danger'}
                        size="sm"
                        className={styles.confidenceBadge}
                    >
                        {candidate.getConfidencePercent()}%
                    </Badge>
                </div>
                <div className={styles.candidateMeta}>
                    <span>{candidate.quantity} {candidate.unit}</span>
                    <span>•</span>
                    <span>{STORAGE_LABELS[candidate.storageLocation]}</span>
                    <span>•</span>
                    <span>{candidate.expiryDate}</span>
                </div>
            </div>

            <div className={styles.candidateActions}>
                <button onClick={onEdit} className={styles.actionBtn}>
                    <FiEdit2 size={18} />
                </button>
                <button onClick={onDelete} className={styles.actionBtn}>
                    <FiTrash2 size={18} />
                </button>
            </div>
        </div>
    );
}
