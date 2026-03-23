// shareManager.js
import FileManager from './fileManager.js';

class ShareManager {
    constructor() {
        this.baseUrl = 'http://localhost:6969';
        this.params = new URLSearchParams(window.location.search);
        this.fileId = this.params.get('fileId');
        this.userId = this.params.get('userId');

        this.fileManager = new FileManager();
        this.currentFile = null;
        this.accessors = [];

        console.log('ShareManager инициализирован для файла:', this.fileId);
        console.log('ShareManager userId из URL:', this.userId);
    }

    async init() {
        if (!this.fileId) {
            alert('Ошибка: не указан файл');
            window.location.href = 'fileExchange.html';
            return;
        }

        // Если userId не передан в URL, пробуем получить из сессии
        if (!this.userId) {
            await this.getUserIdFromSession();
        }

        await this.loadFileInfo();
        await this.loadAccessors();
        this.attachEventHandlers();
    }

    async getUserIdFromSession() {
        try {
            const response = await fetch(`${this.baseUrl}/api/auth/me`, {
                credentials: 'include'
            });

            if (response.ok) {
                const user = await response.json();
                this.userId = user.id;
                console.log('Получен userId из сессии:', this.userId);

                // Обновляем URL с правильным userId
                const url = new URL(window.location.href);
                url.searchParams.set('userId', this.userId);
                window.history.replaceState({}, '', url);
            } else {
                console.error('Не удалось получить сессию');
                alert('Ошибка: необходимо авторизоваться');
                window.location.href = 'loginIndex.html';
            }
        } catch (error) {
            console.error('Ошибка получения сессии:', error);
        }
    }

    async loadFileInfo() {
        try {
            // Получаем файлы через FileManager
            const files = await this.fileManager.getFiles();
            console.log('Все файлы пользователя:', files);

            this.currentFile = files.find(f => f.id === this.fileId || f.fileId === this.fileId);

            if (this.currentFile) {
                console.log('✅ Файл найден в ваших файлах! Вы владелец');
                this.displayFileInfo();
            } else {
                // Если файл не найден в своих файлах, возможно это чужой файл
                console.log('Файл не найден в ваших файлах, проверяем общие...');
                const sharedFiles = await this.fileManager.getSharedWithMe();
                this.currentFile = sharedFiles.find(f => (f.fileId || f.id) === this.fileId);

                if (this.currentFile) {
                    console.log('Файл найден в общих файлах');
                    this.displayFileInfo();
                } else {
                    throw new Error('Файл не найден');
                }
            }
        } catch (error) {
            console.error('Ошибка загрузки информации о файле:', error);
            // Создаем заглушку для отображения
            this.currentFile = {
                id: this.fileId,
                originalName: 'Файл',
                size: 0,
                category: 'general'
            };
            this.displayFileInfo();
        }
    }

    displayFileInfo() {
        const fileInfoEl = document.getElementById('file-info');
        const fileName = this.currentFile.originalName || this.currentFile.name || 'Файл';
        const fileSize = this.fileManager.formatFileSize(this.currentFile.size || this.currentFile.fileSize || 0);

        let iconSVG = '';
        const category = this.currentFile.category || 'general';

        switch(category) {
            case 'photos':
                iconSVG = `<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>`;
                break;
            case 'videos':
                iconSVG = `<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><polygon points="10 8 16 12 10 16 10 8"/></svg>`;
                break;
            case 'music':
                iconSVG = `<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M9 17H5V9H9V17Z"/><path d="M19 17H15V5H19V17Z"/><circle cx="7" cy="18" r="3"/><circle cx="17" cy="18" r="3"/></svg>`;
                break;
            default:
                iconSVG = `<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 2V8H20" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
        }

        fileInfoEl.innerHTML = `
            <div class="file-icon-large">${iconSVG}</div>
            <div class="file-details">
                <h3>${fileName}</h3>
                <p>Размер: ${fileSize}</p>
            </div>
        `;
    }

    async loadAccessors() {
        try {
            const url = `${this.baseUrl}/api/permission/${this.fileId}/accessors`;
            console.log('Fetching accessors from:', url);

            const response = await fetch(url, {
                credentials: 'include'
            });

            console.log('Accessors response status:', response.status);

            if (response.status === 403) {
                // Если нет прав, показываем сообщение
                this.accessors = [];
                this.displayAccessors();
                return;
            }

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Error response:', errorText);
                throw new Error(`Ошибка ${response.status}: ${errorText}`);
            }

            const data = await response.json();
            console.log('Accessors data:', data);
            this.accessors = data || [];
            this.displayAccessors();
        } catch (error) {
            console.error('Ошибка загрузки прав доступа:', error);
            this.accessors = [];
            document.getElementById('permissions-list').innerHTML = `
                <div class="empty-permissions">
                    <p>Ошибка загрузки списка доступа</p>
                    <p class="error-detail">${error.message}</p>
                    <button onclick="location.reload()" class="btn-retry">Повторить</button>
                </div>
            `;
        }
    }

    displayAccessors() {
        const listEl = document.getElementById('permissions-list');

        if (this.accessors.length === 0) {
            listEl.innerHTML = `
                <div class="empty-permissions">
                    <p>Никому не предоставлен доступ к этому файлу</p>
                </div>
            `;
            return;
        }

        listEl.innerHTML = this.accessors.map(accessor => {
            const userEmail = accessor.user?.email || accessor.userEmail || 'email@example.com';
            const userInitial = userEmail[0].toUpperCase();
            const targetUserId = accessor.user?.id || accessor.userId;

            console.log('Accessor user ID:', targetUserId);

            return `
                <div class="permission-item" data-target-user-id="${targetUserId}">
                    <div class="permission-avatar" title="${userEmail}">
                        ${userInitial}
                    </div>
                    <div class="permission-info">
                        <h4>${userEmail}</h4>
                        <p>Роль: ${this.getRoleLabel(accessor.role)}</p>
                    </div>
                    <span class="permission-badge badge-${this.getRoleClass(accessor.role)}">
                        ${this.getRoleLabel(accessor.role)}
                    </span>
                    <div class="permission-actions">
                        <button class="btn-danger" onclick="shareManager.revokeAccess('${targetUserId}')">
                            Отозвать
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }

    getRoleLabel(role) {
        const labels = {
            'READER': 'Чтение',
            'EDITOR': 'Редактирование'
        };
        return labels[role] || role;
    }

    getRoleClass(role) {
        const classes = {
            'READER': 'read',
            'EDITOR': 'edit'
        };
        return classes[role] || 'read';
    }

    async grantAccess() {
        const email = document.getElementById('user-email').value.trim();
        const accessLevel = document.getElementById('access-level').value;

        let role = 'READER';
        if (accessLevel === 'edit') role = 'EDITOR';

        if (!email) {
            alert('Введите email пользователя');
            return;
        }

        const btn = document.getElementById('grant-access-btn');
        btn.disabled = true;
        btn.innerHTML = 'Предоставление...';

        try {
            const url = `${this.baseUrl}/api/permission/share`;
            console.log('Отправка запроса на:', url);
            console.log('Данные:', { fileId: this.fileId, userEmail: email, role });

            const response = await fetch(url, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fileId: this.fileId,
                    userEmail: email,
                    role: role
                })
            });

            console.log('Статус ответа:', response.status);

            if (response.status === 403) {
                const errorText = await response.text();
                console.error('Ошибка 403:', errorText);

                if (errorText.includes('Only owner can share files')) {
                    alert('❌ Вы не являетесь владельцем этого файла. Только владелец может предоставлять доступ.');
                } else {
                    alert('❌ Ошибка доступа: ' + errorText);
                }
                return;
            }

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const result = await response.json();
            console.log('Результат:', result);

            await this.loadAccessors();
            document.getElementById('user-email').value = '';
            alert('✅ Доступ успешно предоставлен');
        } catch (error) {
            console.error('Ошибка предоставления доступа:', error);
            alert('❌ Ошибка: ' + error.message);
        } finally {
            btn.disabled = false;
            btn.innerHTML = `
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="8.5" cy="7" r="4"/>
                    <line x1="20" y1="8" x2="20" y2="14"/>
                    <line x1="23" y1="11" x2="17" y2="11"/>
                </svg>
                Предоставить доступ
            `;
        }
    }

    async revokeAccess(targetUserId) {
        if (!confirm('Вы уверены, что хотите отозвать доступ?')) {
            return;
        }

        try {
            const url = `${this.baseUrl}/api/permission/${this.fileId}/revoke/${targetUserId}`;
            console.log('Отзыв доступа:', url);

            const response = await fetch(url, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            await this.loadAccessors();
            alert('✅ Доступ отозван');
        } catch (error) {
            console.error('Ошибка отзыва доступа:', error);
            alert('❌ Ошибка: ' + error.message);
        }
    }

    attachEventHandlers() {
        document.getElementById('grant-access-btn').addEventListener('click', () => this.grantAccess());
    }
}

window.shareManager = new ShareManager();

document.addEventListener('DOMContentLoaded', () => {
    window.shareManager.init();
});