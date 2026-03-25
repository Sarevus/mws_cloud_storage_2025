// subscription.js

const API_BASE = 'http://localhost:6969';

class SubscriptionManager {
    constructor() {
        this.params = new URLSearchParams(window.location.search);
        this.userId = this.params.get('id');
        this.plans = [];
        this.currentPlan = null;
        this.currentStorageInfo = null;
    }

    async init() {
        const grid = document.getElementById('plans-grid');
        grid.innerHTML = '<div class="loading-plans">Загрузка тарифов...</div>';

        if (!this.userId) {
            await this.getUserIdFromSession();
        }

        if (!this.userId) {
            alert('Ошибка: необходимо авторизоваться');
            window.location.href = 'loginIndex.html';
            return;
        }

        await Promise.all([
            this.loadCurrentSubscription(),
            this.loadPlans()
        ]);
    }

    async getUserIdFromSession() {
        try {
            const response = await fetch(`${API_BASE}/api/auth/me`, {
                credentials: 'include'
            });
            if (response.ok) {
                const user = await response.json();
                this.userId = user.id;
                const url = new URL(window.location.href);
                url.searchParams.set('id', this.userId);
                window.history.replaceState({}, '', url);
                console.log('✅ Получен userId из сессии:', this.userId);
            }
        } catch (error) {
            console.error('Ошибка получения сессии:', error);
        }
    }

    async loadCurrentSubscription() {
        try {
            const response = await fetch(`${API_BASE}/api/subscriptions/current`, {
                credentials: 'include'
            });

            if (response.ok) {
                const data = await response.json();
                if (data && data.plan) {
                    this.currentPlan = data;
                    console.log('✅ Текущая подписка:', this.currentPlan.plan.name);
                }
            }

            const storageResponse = await fetch(`${API_BASE}/api/storage/info`, {
                credentials: 'include'
            });
            if (storageResponse.ok) {
                this.currentStorageInfo = await storageResponse.json();
            }

            // Обновляем отображение текущей подписки (ДАЖЕ ЕСЛИ НЕТ ПОДПИСКИ)
            this.updateCurrentSubscriptionDisplay();

        } catch (error) {
            console.error('Ошибка загрузки текущей подписки:', error);
            // Показываем FREE по умолчанию
            this.updateCurrentSubscriptionDisplay();
        }
    }

    async loadPlans() {
        const grid = document.getElementById('plans-grid');

        try {
            const response = await fetch(`${API_BASE}/api/subscriptions/plans`, {
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error('Ошибка загрузки тарифов');
            }

            this.plans = await response.json();
            console.log('✅ Загружено тарифов:', this.plans.length);
            this.renderPlans();
        } catch (error) {
            console.error('Ошибка:', error);
            grid.innerHTML = `
                <div class="loading-plans">
                    <p>❌ Ошибка загрузки тарифов</p>
                    <button onclick="location.reload()" class="btn-retry">Повторить</button>
                </div>
            `;
        }
    }

    renderPlans() {
        const grid = document.getElementById('plans-grid');

        if (!this.plans.length) {
            grid.innerHTML = '<div class="loading-plans">Нет доступных тарифов</div>';
            return;
        }

        // Обновляем карточку текущей подписки сверху
        this.updateCurrentSubscriptionDisplay();

        // Отображаем все тарифы
        grid.innerHTML = this.plans.map(plan => {
            const isCurrent = this.currentPlan?.plan?.id === plan.id;
            const isPopular = plan.name === 'PREMIUM';
            const features = this.getPlanFeatures(plan);

            const priceMonth = plan.pricePerMonth === 0 ? 'Бесплатно' : `${plan.pricePerMonth} ₽`;
            const priceYear = plan.pricePerYear === 0 ? '' : `${plan.pricePerYear} ₽/год`;

            return `
                <div class="plan-card ${isPopular ? 'popular' : ''} ${isCurrent ? 'current-plan-card' : ''}">
                    ${isPopular ? '<div class="popular-badge">Популярный</div>' : ''}
                    ${isCurrent ? '<div class="current-badge">Текущий</div>' : ''}
                    <div class="plan-name-large">${plan.name}</div>
                    <div class="plan-price">
                        <div class="price-month">${priceMonth} <small>/мес</small></div>
                        ${priceYear ? `<div class="price-year">${priceYear}</div>` : ''}
                    </div>
                    <ul class="plan-features">
                        ${features.map(f => `<li>${f}</li>`).join('')}
                    </ul>
                    <button
                        class="btn-subscribe ${isCurrent ? 'btn-current' : ''}"
                        data-plan-id="${plan.id}"
                        data-plan-name="${plan.name}"
                        ${isCurrent ? 'disabled' : ''}
                        onclick="window.subscriptionManager.subscribe('${plan.id}', '${plan.name}')"
                    >
                        ${isCurrent ? '✅ Текущий тариф' : 'Оформить подписку'}
                    </button>
                </div>
            `;
        }).join('');
    }

    getPlanFeatures(plan) {
        const features = [];
        const storageGB = (plan.storageLimitBytes / (1024 * 1024 * 1024)).toFixed(0);
        features.push(`${storageGB} GB хранилища`);

        if (plan.maxFileSizeBytes) {
            const maxSizeMB = (plan.maxFileSizeBytes / (1024 * 1024)).toFixed(0);
            features.push(`Файлы до ${maxSizeMB} MB`);
        }

        if (plan.maxFilesCount) {
            features.push(`До ${plan.maxFilesCount} файлов`);
        }

        if (plan.canShareFiles) {
            features.push(`✓ Общий доступ к файлам`);
        }

        if (plan.name === 'PREMIUM' || plan.name === 'BUSINESS') {
            features.push(`⭐ Приоритетная поддержка 24/7`);
        }

        if (plan.name === 'BUSINESS') {
            features.push(`👥 Управление командой`);
        }

        return features;
    }

    updateCurrentSubscriptionDisplay() {
        const container = document.getElementById('current-subscription');

        // Если контейнера нет, выходим
        if (!container) return;

        const planNameSpan = document.querySelector('#current-plan .plan-name');
        const storageInfoSpan = document.getElementById('storage-info');

        // Если нет подписки или не загружена, показываем FREE
        if (!this.currentPlan?.plan) {
            container.style.display = 'block';
            if (planNameSpan) planNameSpan.textContent = 'FREE';
            if (storageInfoSpan) {
                const usedFormatted = this.currentStorageInfo ? this.formatBytes(this.currentStorageInfo.used) : '0 B';
                const totalFormatted = this.formatBytes(1073741824); // 1 GB для FREE
                const usedPercent = this.currentStorageInfo?.percent || 0;
                storageInfoSpan.textContent = `${usedFormatted} из ${totalFormatted} (${usedPercent}%)`;
            }
            return;
        }

        // Есть подписка
        container.style.display = 'block';

        const usedPercent = this.currentStorageInfo?.percent || 0;
        const usedFormatted = this.currentStorageInfo ? this.formatBytes(this.currentStorageInfo.used) : '0 B';
        const totalFormatted = this.formatBytes(this.currentPlan.plan.storageLimitBytes);

        if (planNameSpan) {
            planNameSpan.textContent = this.currentPlan.plan.name;
        }
        if (storageInfoSpan) {
            storageInfoSpan.textContent = `${usedFormatted} из ${totalFormatted} (${usedPercent}%)`;
        }
    }

    formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    async subscribe(planId, planName) {
        const loadingOverlay = document.getElementById('loading-overlay');
        loadingOverlay.classList.add('active');

        try {
            const response = await fetch(`${API_BASE}/api/subscriptions/subscribe?planId=${planId}&period=YEARLY`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' }
            });

            if (!response.ok) {
                if (response.status === 402) {
                    alert('💳 Требуется оплата. Перенаправление на страницу оплаты...');
                }
                const error = await response.text();
                throw new Error(error || `Ошибка ${response.status}`);
            }

            const result = await response.json();
            alert(`✅ Подписка на тариф "${planName}" успешно оформлена!`);

            // Перезагружаем данные
            await this.loadCurrentSubscription();
            await this.loadPlans();

            // Обновляем прогресс-бар и подписку в профиле
            if (window.opener && !window.opener.closed) {
                if (window.opener.refreshStorage) {
                    window.opener.refreshStorage();
                }
                if (window.opener.loadCurrentSubscription) {
                    window.opener.loadCurrentSubscription();
                }
                if (window.opener.fileManager?.updateStorageInfo) {
                    window.opener.fileManager.updateStorageInfo();
                }
            }

            // Обновляем прогресс-бар на текущей странице
            await this.updateStorageInfoOnPage();

            setTimeout(() => {
                location.reload();
            }, 1000);

        } catch (error) {
            console.error('Ошибка оформления подписки:', error);
            alert('❌ Ошибка оформления подписки: ' + error.message);
        } finally {
            loadingOverlay.classList.remove('active');
        }
    }

    async updateStorageInfoOnPage() {
        try {
            const response = await fetch(`${API_BASE}/api/storage/info`, {
                credentials: 'include'
            });
            if (response.ok) {
                const data = await response.json();
                this.currentStorageInfo = data;
                this.updateCurrentSubscriptionDisplay();

                const progressBar = document.getElementById('storage-progress');
                const usedEl = document.getElementById('storage-used');
                const totalEl = document.getElementById('storage-total');
                const percentEl = document.getElementById('storage-percent');

                if (progressBar && usedEl && totalEl && percentEl) {
                    const usedFormatted = this.formatBytes(data.used);
                    const totalFormatted = this.formatBytes(data.total);
                    progressBar.style.width = data.percent + '%';
                    usedEl.textContent = usedFormatted;
                    totalEl.textContent = totalFormatted;
                    percentEl.textContent = `(${data.percent}%)`;
                }
            }
        } catch (error) {
            console.error('Ошибка обновления прогресс-бара:', error);
        }
    }
}

let subscriptionManager;

document.addEventListener('DOMContentLoaded', () => {
    subscriptionManager = new SubscriptionManager();
    subscriptionManager.init();
    window.subscriptionManager = subscriptionManager;
});