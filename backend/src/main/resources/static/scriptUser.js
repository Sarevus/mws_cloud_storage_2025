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

        // После загрузки пользователя загружаем информацию о хранилище
        loadStorageInfo();
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

// Функция загрузки информации о хранилище
async function loadStorageInfo() {
    try {
        console.log('Загрузка информации о хранилище...');

        // Правильный URL для StorageController
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

        // Обновляем прогресс-бар
        updateStorageBar(data);

    } catch (error) {
        console.error('❌ Ошибка загрузки информации о хранилище:', error);

        // Показываем ошибку в интерфейсе
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

    // Устанавливаем ширину прогресс-бара
    progressBar.style.width = data.percent + '%';

    // Форматируем байты в читаемый вид
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