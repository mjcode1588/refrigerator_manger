// ===========================================
// 냉장고 관리 앱 타입 정의
// ===========================================

// 식품 카테고리
export type FoodCategory = 
  | 'vegetables' 
  | 'fruits' 
  | 'meat' 
  | 'seafood' 
  | 'dairy' 
  | 'beverages' 
  | 'condiments' 
  | 'frozen' 
  | 'etc';

export const CATEGORY_LABELS: Record<FoodCategory, string> = {
  vegetables: '채소',
  fruits: '과일',
  meat: '육류',
  seafood: '해산물',
  dairy: '유제품',
  beverages: '음료',
  condiments: '조미료',
  frozen: '냉동식품',
  etc: '기타',
};

export const CATEGORY_ICONS: Record<FoodCategory, string> = {
  vegetables: '🥬',
  fruits: '🍎',
  meat: '🥩',
  seafood: '🐟',
  dairy: '🧀',
  beverages: '🥤',
  condiments: '🧂',
  frozen: '🧊',
  etc: '📦',
};

// 보관 위치
export type StorageLocation = 'freezer' | 'fridge' | 'room';

export const STORAGE_LABELS: Record<StorageLocation, string> = {
  freezer: '냉동실',
  fridge: '냉장실',
  room: '실온',
};

// 식품 아이템
export interface FoodItem {
  id: string;
  name: string;
  category: FoodCategory;
  quantity: number;
  unit: string;
  purchaseDate: string;
  expiryDate: string;
  storageLocation: StorageLocation;
  memo?: string;
  imageUrl?: string;
  createdAt: string;
  updatedAt: string;
}

// 후보 아이템 (인식 결과)
export interface CandidateItem {
  id: string;
  name: string;
  category: FoodCategory;
  quantity: number;
  unit: string;
  expiryDate: string;
  storageLocation: StorageLocation;
  confidence: number; // 인식 신뢰도 0-1
  selected: boolean; // 사용자 선택 여부
  memo?: string;
}

// 인식 요청
export interface IngestRequest {
  text?: string;
  images?: File[];
}

// 인식 응답
export interface IngestResponse {
  candidates: CandidateItem[];
}

// 확정 요청
export interface ConfirmRequest {
  fridgeId?: string;
  items: Omit<CandidateItem, 'confidence' | 'selected' | 'id'>[];
}

// 레시피
export interface Recipe {
  id: string;
  name: string;
  description: string;
  imageUrl?: string;
  cookingTime: number; // 분 단위
  difficulty: 'easy' | 'medium' | 'hard';
  servings: number;
  useItems: { name: string; quantity: string }[];
  missingItems: { name: string; quantity: string }[];
  instructions: string[];
}

// 레시피 추천 응답
export interface RecipeSuggestResponse {
  recipes: Recipe[];
}

// 냉장고 멤버
export interface FridgeMember {
  id: string;
  name: string;
  email: string;
  role: 'owner' | 'member';
  joinedAt: string;
  avatarUrl?: string;
}

// 냉장고 정보
export interface Fridge {
  id: string;
  name: string;
  inviteCode?: string;
  members: FridgeMember[];
  createdAt: string;
}

// 알림
export interface Notification {
  id: string;
  type: 'expiry' | 'family' | 'system';
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  itemId?: string;
}

// 사용자 설정
export interface UserSettings {
  expiryAlertDays: number;
  pushEnabled: boolean;
  emailEnabled: boolean;
}

// API 응답 래퍼
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
}

// 페이지네이션
export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  hasNext: boolean;
}

// 필터 옵션
export interface ItemFilterOptions {
  category?: FoodCategory;
  storageLocation?: StorageLocation;
  expiryStatus?: 'expired' | 'expiring' | 'fresh';
  search?: string;
  sortBy?: 'name' | 'expiryDate' | 'createdAt';
  sortOrder?: 'asc' | 'desc';
}
