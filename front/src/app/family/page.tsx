'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import styles from './page.module.css';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Button, Input, Card, Badge, LoadingState, ErrorState, EmptyState, Spinner } from '@/components/common';
import { fridgesApi } from '@/lib/api';
import { copyToClipboard } from '@/lib/utils';
import { FiArrowLeft, FiCopy, FiCheck, FiUserPlus, FiUsers, FiRefreshCw, FiLogIn, FiStar, FiUser } from 'react-icons/fi';

export default function FamilyPage() {
    const router = useRouter();
    const [inviteCode, setInviteCode] = useState('');
    const [joinCode, setJoinCode] = useState('');
    const [copied, setCopied] = useState(false);
    const [activeTab, setActiveTab] = useState<'members' | 'invite' | 'join'>('members');
    const [newFridgeName, setNewFridgeName] = useState('내 냉장고');

    // 현재 냉장고 정보
    const {
        data: fridge,
        isLoading: fridgeLoading,
        error: fridgeError,
        refetch: refetchFridge
    } = useQuery({
        queryKey: ['current-fridge'],
        queryFn: async () => {
            const response = await fridgesApi.getCurrentFridge();
            if (!response.success) throw new Error(response.error?.message);
            return response.data!;
        },
    });

    // 멤버 목록
    const {
        data: members,
        isLoading: membersLoading,
        refetch: refetchMembers
    } = useQuery({
        queryKey: ['fridge-members', fridge?.id],
        queryFn: async () => {
            if (!fridge?.id) return [];
            const response = await fridgesApi.getMembers(fridge.id);
            if (!response.success) throw new Error(response.error?.message);
            return response.data!;
        },
        enabled: !!fridge?.id,
    });

    // 초대 코드 생성
    const {
        mutate: createInvite,
        isPending: inviteLoading
    } = useMutation({
        mutationFn: async () => {
            const response = await fridgesApi.createInvite();
            if (!response.success) throw new Error(response.error?.message);
            return response.data!;
        },
        onSuccess: (data) => {
            setInviteCode(data.inviteCode);
        },
    });

    // 냉장고 합류
    const {
        mutate: joinFridge,
        isPending: joinLoading,
        error: joinError
    } = useMutation({
        mutationFn: async () => {
            const response = await fridgesApi.join(joinCode);
            if (!response.success) throw new Error(response.error?.message);
            return response.data!;
        },
        onSuccess: () => {
            refetchFridge();
            refetchMembers();
            setJoinCode('');
            setActiveTab('members');
        },
    });

    // 냉장고 생성
    const {
        mutate: createFridge,
        isPending: createFridgeLoading,
        error: createFridgeError,
    } = useMutation({
        mutationFn: async () => {
            const response = await fridgesApi.createFridge(newFridgeName.trim() || '내 냉장고');
            if (!response.success) throw new Error(response.error?.message);
            return response.data!;
        },
        onSuccess: () => {
            refetchFridge();
            refetchMembers();
            setActiveTab('members');
        },
    });

    // 복사 핸들러
    const handleCopy = async () => {
        const success = await copyToClipboard(inviteCode);
        if (success) {
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        }
    };

    return (
        <main className={styles.main}>
            {/* 헤더 */}
            <header className={styles.header}>
                <button onClick={() => router.back()} className={styles.backButton}>
                    <FiArrowLeft size={24} />
                </button>
                <h1 className={styles.title}>가족 공유</h1>
                <div className={styles.spacer} />
            </header>

            {/* 탭 */}
            <div className={styles.tabs}>
                <button
                    className={`${styles.tab} ${activeTab === 'members' ? styles.active : ''}`}
                    onClick={() => setActiveTab('members')}
                >
                    <FiUsers size={18} />
                    <span>멤버</span>
                </button>
                <button
                    className={`${styles.tab} ${activeTab === 'invite' ? styles.active : ''}`}
                    onClick={() => setActiveTab('invite')}
                >
                    <FiUserPlus size={18} />
                    <span>초대</span>
                </button>
                <button
                    className={`${styles.tab} ${activeTab === 'join' ? styles.active : ''}`}
                    onClick={() => setActiveTab('join')}
                >
                    <FiLogIn size={18} />
                    <span>합류</span>
                </button>
            </div>

            <div className={styles.content}>
                {/* 멤버 탭 */}
                {activeTab === 'members' && (
                    <div className={styles.membersSection}>
                        {fridgeLoading || membersLoading ? (
                            <LoadingState message="멤버 정보를 불러오는 중..." />
                        ) : fridgeError ? (
                            <ErrorState
                                message="멤버 정보를 불러올 수 없습니다."
                                onRetry={() => refetchFridge()}
                            />
                        ) : !fridge ? (
                            <>
                                <EmptyState
                                    icon="🏠"
                                    title="냉장고가 없습니다"
                                    description="초대 코드로 냉장고에 합류하거나 새로 만들어보세요."
                                    action={{
                                        label: '냉장고 합류하기',
                                        onClick: () => setActiveTab('join'),
                                    }}
                                />
                                <div className={styles.inviteSection}>
                                    <div className={styles.inviteCard}>
                                        <div className={styles.inviteHeader}>
                                            <FiUserPlus size={48} className={styles.inviteIcon} />
                                            <h2>냉장고 만들기</h2>
                                            <p>새 냉장고를 만들어 가족을 초대할 수 있어요.</p>
                                        </div>
                                        <div className={styles.inviteBody}>
                                            <Input
                                                value={newFridgeName}
                                                onChange={(e) => setNewFridgeName(e.target.value)}
                                                placeholder="냉장고 이름"
                                            />
                                            {createFridgeError && (
                                                <p className={styles.errorMessage}>
                                                    {(createFridgeError as Error).message}
                                                </p>
                                            )}
                                            <Button
                                                onClick={() => createFridge()}
                                                isLoading={createFridgeLoading}
                                                fullWidth
                                            >
                                                냉장고 만들기
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            </>
                        ) : (
                            <>
                                <div className={styles.fridgeInfo}>
                                    <span className={styles.fridgeIcon}>🧊</span>
                                    <div className={styles.fridgeDetails}>
                                        <h2 className={styles.fridgeName}>{fridge.name}</h2>
                                        <p className={styles.fridgeMeta}>
                                            {members?.length || 0}명의 멤버
                                        </p>
                                    </div>
                                </div>

                                <div className={styles.memberList}>
                                    {members?.map(member => (
                                        <div key={member.id} className={styles.memberCard}>
                                            <div className={styles.memberAvatar}>
                                                {member.avatarUrl ? (
                                                    <img src={member.avatarUrl} alt={member.name} />
                                                ) : (
                                                    <span>{(member.name || member.id)[0]}</span>
                                                )}
                                            </div>
                                            <div className={styles.memberInfo}>
                                                <div className={styles.memberName}>
                                                    {member.name || member.id}
                                                    {member.role === 'owner' && (
                                                        <Badge variant="primary" size="sm">
                                                            <FiStar size={12} /> 소유자
                                                        </Badge>
                                                    )}
                                                </div>
                                                {member.email && <p className={styles.memberEmail}>{member.email}</p>}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </>
                        )}
                    </div>
                )}

                {/* 초대 탭 */}
                {activeTab === 'invite' && (
                    <div className={styles.inviteSection}>
                        <div className={styles.inviteCard}>
                            <div className={styles.inviteHeader}>
                                <FiUserPlus size={48} className={styles.inviteIcon} />
                                <h2>가족 초대하기</h2>
                                <p>초대 코드를 공유하여 가족을 냉장고에 초대하세요.</p>
                            </div>

                            {inviteCode ? (
                                <div className={styles.inviteCodeBox}>
                                    <span className={styles.inviteCodeLabel}>초대 코드</span>
                                    <div className={styles.inviteCodeValue}>
                                        <span>{inviteCode}</span>
                                        <button onClick={handleCopy} className={styles.copyBtn}>
                                            {copied ? <FiCheck /> : <FiCopy />}
                                        </button>
                                    </div>
                                    <p className={styles.inviteCodeHint}>
                                        이 코드를 가족에게 공유하세요. 24시간 동안 유효합니다.
                                    </p>

                                    <Button
                                        variant="secondary"
                                        onClick={() => createInvite()}
                                        isLoading={inviteLoading}
                                        leftIcon={<FiRefreshCw />}
                                    >
                                        새 코드 생성
                                    </Button>
                                </div>
                            ) : (
                                <Button
                                    onClick={() => createInvite()}
                                    isLoading={inviteLoading}
                                    size="lg"
                                    leftIcon={<FiUserPlus />}
                                >
                                    초대 코드 생성
                                </Button>
                            )}
                        </div>
                    </div>
                )}

                {/* 합류 탭 */}
                {activeTab === 'join' && (
                    <div className={styles.joinSection}>
                        <div className={styles.joinCard}>
                            <div className={styles.joinHeader}>
                                <FiLogIn size={48} className={styles.joinIcon} />
                                <h2>냉장고 합류하기</h2>
                                <p>가족에게 받은 초대 코드를 입력하세요.</p>
                            </div>

                            <div className={styles.joinForm}>
                                <Input
                                    value={joinCode}
                                    onChange={(e) => setJoinCode(e.target.value)}
                                    placeholder="초대 코드 입력"
                                    error={joinError ? (joinError as Error).message : undefined}
                                />

                                <Button
                                    onClick={() => joinFridge()}
                                    isLoading={joinLoading}
                                    disabled={!joinCode.trim()}
                                    size="lg"
                                    fullWidth
                                    leftIcon={<FiLogIn />}
                                >
                                    냉장고 합류
                                </Button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </main>
    );
}
