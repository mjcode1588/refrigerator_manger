// ===========================================
// FoodItem 도메인 엔티티 테스트
// TDD: 비즈니스 로직 검증
// ===========================================

import { FoodItem, FoodItemProps } from '../FoodItem';

describe('FoodItem Entity', () => {
    const mockDate = '2026-01-13';

    const createValidProps = (overrides?: Partial<FoodItemProps>): FoodItemProps => ({
        id: 'test-id-1',
        name: '우유',
        category: 'dairy',
        quantity: 2,
        unit: '개',
        purchaseDate: '2026-01-10',
        expiryDate: '2026-01-16', // 3일 후
        storageLocation: 'fridge',
        createdAt: '2026-01-10T10:00:00Z',
        updatedAt: '2026-01-10T10:00:00Z',
        ...overrides,
    });

    beforeAll(() => {
        // 테스트용 날짜 고정
        jest.useFakeTimers();
        jest.setSystemTime(new Date('2026-01-13'));
    });

    afterAll(() => {
        jest.useRealTimers();
    });

    describe('create', () => {
        it('유효한 props로 FoodItem을 생성해야 한다', () => {
            const props = createValidProps();
            const item = FoodItem.create(props);

            expect(item.id).toBe('test-id-1');
            expect(item.name).toBe('우유');
            expect(item.category).toBe('dairy');
            expect(item.quantity).toBe(2);
        });

        it('이름이 비어있으면 에러를 던져야 한다', () => {
            const props = createValidProps({ name: '' });

            expect(() => FoodItem.create(props)).toThrow('식품명은 필수입니다.');
        });

        it('수량이 음수면 에러를 던져야 한다', () => {
            const props = createValidProps({ quantity: -1 });

            expect(() => FoodItem.create(props)).toThrow('수량은 0 이상이어야 합니다.');
        });

        it('유통기한이 없으면 에러를 던져야 한다', () => {
            const props = createValidProps({ expiryDate: '' });

            expect(() => FoodItem.create(props)).toThrow('유통기한은 필수입니다.');
        });
    });

    describe('getDaysUntilExpiry', () => {
        it('만료까지 남은 일수를 정확히 계산해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-16' }));

            expect(item.getDaysUntilExpiry()).toBe(3);
        });

        it('만료된 경우 음수를 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-10' }));

            expect(item.getDaysUntilExpiry()).toBe(-3);
        });

        it('당일인 경우 0을 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-13' }));

            expect(item.getDaysUntilExpiry()).toBe(0);
        });
    });

    describe('getExpiryStatus', () => {
        it('만료된 경우 "expired"를 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-10' }));

            expect(item.getExpiryStatus()).toBe('expired');
        });

        it('3일 이내면 "expiring"을 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-15' }));

            expect(item.getExpiryStatus(3)).toBe('expiring');
        });

        it('3일 초과면 "fresh"를 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-20' }));

            expect(item.getExpiryStatus(3)).toBe('fresh');
        });

        it('커스텀 alertDays를 적용해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-18' }));

            expect(item.getExpiryStatus(5)).toBe('expiring');
            expect(item.getExpiryStatus(3)).toBe('fresh');
        });
    });

    describe('getDDayText', () => {
        it('만료 전 D-N 형식을 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-16' }));

            expect(item.getDDayText()).toBe('D-3');
        });

        it('당일은 D-Day를 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-13' }));

            expect(item.getDDayText()).toBe('D-Day');
        });

        it('만료 후 D+N 형식을 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ expiryDate: '2026-01-10' }));

            expect(item.getDDayText()).toBe('D+3');
        });
    });

    describe('isExpired / isExpiring', () => {
        it('isExpired는 만료된 경우 true를 반환해야 한다', () => {
            const expired = FoodItem.create(createValidProps({ expiryDate: '2026-01-10' }));
            const fresh = FoodItem.create(createValidProps({ expiryDate: '2026-01-20' }));

            expect(expired.isExpired()).toBe(true);
            expect(fresh.isExpired()).toBe(false);
        });

        it('isExpiring은 임박한 경우 true를 반환해야 한다', () => {
            const expiring = FoodItem.create(createValidProps({ expiryDate: '2026-01-15' }));
            const fresh = FoodItem.create(createValidProps({ expiryDate: '2026-01-20' }));

            expect(expiring.isExpiring(3)).toBe(true);
            expect(fresh.isExpiring(3)).toBe(false);
        });
    });

    describe('updateQuantity', () => {
        it('새로운 수량으로 업데이트된 FoodItem을 반환해야 한다', () => {
            const item = FoodItem.create(createValidProps({ quantity: 2 }));
            const updated = item.updateQuantity(5);

            expect(updated.quantity).toBe(5);
            expect(item.quantity).toBe(2); // 불변성 확인
        });

        it('음수 수량은 에러를 던져야 한다', () => {
            const item = FoodItem.create(createValidProps());

            expect(() => item.updateQuantity(-1)).toThrow('수량은 0 이상이어야 합니다.');
        });
    });

    describe('toDTO', () => {
        it('FoodItemProps 형태로 변환되어야 한다', () => {
            const props = createValidProps();
            const item = FoodItem.create(props);
            const dto = item.toDTO();

            expect(dto).toEqual(props);
        });
    });
});
