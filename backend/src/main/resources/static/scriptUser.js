console.log("scriptUser.js выполняется");

document.addEventListener("DOMContentLoaded", () => {
    const editBtn = document.getElementById("edit-profile-btn");

    const params = new URLSearchParams(window.location.search);
    const userId = params.get("id");

    console.log("userId =", userId);

    if (!userId) {
        console.error("ID пользователя не найден в URL");
        window.location.href = '/loginIndex.html';
        return;
    }

    // Загружаем данные пользователя
    fetch("/api/user/" + encodeURIComponent(userId), {
        credentials: 'include'
    })
    .then(res => {
        if (!res.ok) throw new Error('Ошибка загрузки пользователя');
        return res.json();
    })
    .then(user => {
        document.getElementById("user-name").textContent = user.name || 'Не указано';
        document.getElementById("user-email").textContent = user.email || 'Не указано';
        document.getElementById("user-number").textContent = user.phoneNumber || 'Не указано';

        // Загружаем информацию о хранилище и подписке
        loadStorageInfo();
        loadCurrentSubscription();
    })
    .catch(err => {
        console.error('Ошибка:', err);
    });

    // Кнопка редактирования профиля
    if (editBtn) {
        editBtn.onclick = function () {
            console.log("Редактирование профиля");
            window.location.href = `/editProfile.html?id=${encodeURIComponent(userId)}`;
        };
    }
});

// Функция загрузки текущей подписки
async function loadCurrentSubscription() {
    try {
        const response = await fetch('/api/subscriptions/current', {
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            if (data && data.plan) {
                const subscriptionSpan = document.getElementById('user-subscription');
                if (subscriptionSpan) {
                    subscriptionSpan.textContent = data.plan.name;
                }
                console.log('✅ Текущая подписка:', data.plan.name);
            }
        }
    } catch (error) {
        console.error('❌ Ошибка загрузки подписки:', error);
        const subscriptionSpan = document.getElementById('user-subscription');
        if (subscriptionSpan) {
            subscriptionSpan.textContent = 'Ошибка';
        }
    }
}

// Функция загрузки информации о хранилище
async function loadStorageInfo() {
    try {
        console.log('Загрузка информации о хранилище...');

        const response = await fetch('/api/storage/info', {
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        const data = await response.json();
        console.log('Информация о хранилище (сырые данные):', data);

        updateStorageBar(data);

    } catch (error) {
        console.error('❌ Ошибка загрузки информации о хранилище:', error);

        const progressBar = document.getElementById('storage-progress');
        const usedEl = document.getElementById('storage-used');

        if (progressBar && usedEl) {
            progressBar.style.width = '0%';
            usedEl.textContent = 'Ошибка';
        }
    }
}

// Функция обновления прогресс-бара
function updateStorageBar(data) {
    const progressBar = document.getElementById('storage-progress');
    const usedEl = document.getElementById('storage-used');
    const totalEl = document.getElementById('storage-total');
    const percentEl = document.getElementById('storage-percent');

    if (!progressBar || !usedEl || !totalEl || !percentEl) {
        console.error('❌ Элементы прогресс-бара не найдены в DOM');
        return;
    }

    progressBar.style.width = data.percent + '%';
    usedEl.textContent = formatBytes(data.used);
    totalEl.textContent = formatBytes(data.total);
    percentEl.textContent = `(${data.percent}%)`;

    console.log('✅ Прогресс-бар обновлён:', {
        used: formatBytes(data.used),
        total: formatBytes(data.total),
        percent: data.percent + '%'
    });
}

// Функция форматирования байтов
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    if (!bytes || isNaN(bytes)) return '0 B';

    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Функция для ручного обновления (можно вызвать из консоли)
window.refreshStorage = function() {
    loadStorageInfo();
    loadCurrentSubscription();
};

// Проверка сессии каждую минуту
setInterval(() => {
    fetch("/api/auth/me", { credentials: 'include' })
        .then(res => {
            if (!res.ok) {
                console.log("Сессия истекла, редирект...");
                window.location.href = "/loginIndex.html";
            }
        })
        .catch(() => {
            window.location.href = "/loginIndex.html";
        });
}, 60000);

// Делаем функцию глобальной для вызова из других окон
window.loadCurrentSubscription = loadCurrentSubscription;