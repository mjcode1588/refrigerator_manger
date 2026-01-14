import { Suspense } from 'react';
import ItemsPageClient from './ItemsPageClient';

export default function ItemsPage() {
    return (
        <Suspense fallback={<div>로딩 중...</div>}>
            <ItemsPageClient />
        </Suspense>
    );
}
