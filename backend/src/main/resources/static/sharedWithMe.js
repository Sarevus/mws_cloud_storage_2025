// sharedWithMe.js
import FileManager from './fileManager.js';

class SharedWithMe {
    constructor() {
        this.baseUrl = 'http://localhost:6969';
        this.params = new URLSearchParams(window.location.search);
        this.userId = this.params.get('id');

        this.fileManager = new FileManager();
        this.sharedFiles = [];
        this.currentFilter = 'all';

        console.log('SharedWithMe инициализирован для пользователя:', this.userId);
    }

    async init() {
        if (!this.userId) {
            alert('Ошибка: не указан пользователь');
            window.location.href = 'index.html';
            return;
        }

        await this.loadSharedFiles();
        this.attachEventHandlers();
    }

    async loadSharedFiles() {
        const loadingEl = document.getElementById('shared-files-list');
        loadingEl.innerHTML = '<div class="loading-files">Загрузка файлов...</div>';

        try {
            const response = await fetch(`${this.baseUrl}/api/permission/shared-with-me`, {
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error('Ошибка загрузки общих файлов');
            }

            const data = await response.json();
            this.sharedFiles = data || [];
            this.filterFiles();
        } catch (error) {
            console.error('Ошибка загрузки общих файлов:', error);
            loadingEl.innerHTML = `
                <div class="empty-shared-files">
                    <p>Ошибка загрузки файлов: ${error.message}</p>
                    <button class="action-btn" onclick="location.reload()">Повторить</button>
                </div>
            `;
        }
    }

    filterFiles(filter = 'all') {
        this.currentFilter = filter;

        let filteredFiles = this.sharedFiles;
        if (filter !== 'all') {
            filteredFiles = this.sharedFiles.filter(f => {
                const role = f.role || 'READER';
                if (filter === 'read') return role === 'READER';
                if (filter === 'edit') return role === 'EDITOR';
                return true;
            });
        }

        this.displayFiles(filteredFiles);

        // Обновляем активный таб
        document.querySelectorAll('.shared-tab').forEach(tab => {
            tab.classList.remove('active');
            if (tab.dataset.filter === filter) {
                tab.classList.add('active');
            }
        });
    }

    displayFiles(files) {
        const listEl = document.getElementById('shared-files-list');

        if (files.length === 0) {
            listEl.innerHTML = `
                <div class="empty-shared-files">
                    <h3>Нет общих файлов</h3>
                    <p>Вам пока не предоставили доступ к файлам</p>
                    <button class="action-btn" onclick="location.href='fileExchange.html?id=${this.userId}'">
                        Перейти к моим файлам
                    </button>
                </div>
            `;
            return;
        }

        listEl.innerHTML = files.map(file => this.createFileCard(file)).join('');

        // Добавляем обработчики для кнопок
        document.querySelectorAll('.shared-file-card').forEach(card => {
            card.addEventListener('click', (e) => {
                if (!e.target.classList.contains('btn-open-file')) {
                    this.openFileModal(card.dataset.fileId);
                }
            });
        });

        document.querySelectorAll('.btn-open-file').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.openFileModal(btn.dataset.fileId);
            });
        });
    }

    createFileCard(file) {
        const fileId = file.fileId || file.id;
        const fileName = file.fileName || file.originalName || 'Без имени';
        const role = file.role || 'READER';

        const fileSize = this.fileManager.formatFileSize(file.fileSize || file.size || 0);
        const fileCategory = file.fileCategory || file.category || 'общее';

        const ownerEmail = file.owner?.email || 'unknown@email.com';
        const ownerInitial = ownerEmail[0].toUpperCase();

        let iconSVG = '';
        switch(file.category) {
            case 'photos':
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>`;
                break;
            case 'videos':
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><polygon points="10 8 16 12 10 16 10 8"/></svg>`;
                break;
            case 'music':
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white"><path d="M9 17H5V9H9V17Z"/><path d="M19 17H15V5H19V17Z"/><circle cx="7" cy="18" r="3"/><circle cx="17" cy="18" r="3"/></svg>`;
                break;
            default:
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white"><path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z"/><path d="M14 2V8H20"/></svg>`;
        }

        // Убираем кнопки, оставляем только информацию
        return `
            <div class="shared-file-card" data-file-id="${fileId}" data-role="${role}" data-file-name="${fileName}">
                <div class="shared-file-icon">${iconSVG}</div>
                <div class="shared-file-info">
                    <h3>${fileName}</h3>
                    <div class="shared-file-meta">
                        <span>📁 ${fileCategory}</span>
                        <span>📦 ${fileSize}</span>
                    </div>
                </div>
                <div class="shared-file-owner">
                    <span class="owner-avatar" title="${ownerEmail}">${ownerInitial}</span>
                    <span class="owner-email">${ownerEmail}</span>
                </div>
                <span class="shared-access-badge badge-${this.getRoleClass(role)}">
                    ${this.getRoleLabel(role)}
                </span>
                <!-- Кнопки полностью убраны -->
            </div>
        `;
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

    async openFileModal(fileId) {
        const file = this.sharedFiles.find(f => (f.fileId || f.id) === fileId);
        if (!file) return;

        const modal = document.createElement('div');
        modal.className = 'modal active';
        modal.id = 'file-modal';

        const fileSize = this.fileManager.formatFileSize(file.fileSize || file.size || 0);
        const fileName = file.fileName || file.originalName || 'Без имени';
        const role = file.role || 'READER';
        const ownerEmail = file.owner?.email || 'unknown@email.com';
        const canEdit = role === 'EDITOR';

        // Создаем кнопки действий
        let actionButtons = '';

        // Кнопка скачивания есть всегда
        actionButtons += `<button class="btn-download-modal" id="modal-download">Скачать</button>`;

        // Кнопка переименования только для EDITOR
        if (canEdit) {
            actionButtons += `<button class="btn-rename-modal" id="modal-rename">Переименовать</button>`;
        }

        modal.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h2>${fileName}</h2>
                    <button class="modal-close" onclick="document.getElementById('file-modal').remove()">×</button>
                </div>
                <div class="modal-body">
                    <div class="file-preview">
                        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5">
                            <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z"/>
                            <path d="M14 2V8H20"/>
                        </svg>
                        <div class="file-details-modal">
                            <h3>${fileName}</h3>
                            <p>Размер: ${fileSize}</p>
                            <p>Владелец: <span class="owner-highlight">${ownerEmail}</span></p>
                            <p>Доступ: ${this.getRoleLabel(role)}</p>
                        </div>
                    </div>
                </div>
                <div class="modal-actions">
                    ${actionButtons}
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // Обработчик для скачивания
        document.getElementById('modal-download').addEventListener('click', async () => {
            await this.fileManager.downloadFile(fileId, fileName);
        });

        // Обработчик для переименования (если есть права)
        if (canEdit) {
            document.getElementById('modal-rename').addEventListener('click', async () => {
                const modal = document.getElementById('file-modal');
                const newName = prompt('Введите новое имя файла:', fileName);

                if (newName && newName !== fileName) {
                    const renameBtn = document.getElementById('modal-rename');
                    renameBtn.textContent = '...';
                    renameBtn.disabled = true;

                    const success = await this.fileManager.renameFile(fileId, newName, fileName);

                    if (success) {
                        // Обновляем название в модальном окне
                        modal.querySelector('h2').textContent = newName;
                        modal.querySelector('.file-details-modal h3').textContent = newName;

                        // Обновляем название в карточке файла
                        const fileCard = document.querySelector(`.shared-file-card[data-file-id="${fileId}"] h3`);
                        if (fileCard) {
                            fileCard.textContent = newName;
                        }

                        // Обновляем данные в массиве
                        const fileIndex = this.sharedFiles.findIndex(f => (f.fileId || f.id) === fileId);
                        if (fileIndex !== -1) {
                            this.sharedFiles[fileIndex].fileName = newName;
                            this.sharedFiles[fileIndex].originalName = newName;
                        }
                    }

                    renameBtn.textContent = 'Переименовать';
                    renameBtn.disabled = false;
                }
            });
        }
    }

    attachEventHandlers() {
        document.querySelectorAll('.shared-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                this.filterFiles(tab.dataset.filter);
            });
        });

        // Обработчик для скачивания
        document.querySelectorAll('.btn-download').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;
                const fileName = btn.dataset.fileName;

                btn.textContent = '...';
                btn.disabled = true;

                await this.fileManager.downloadFile(fileId, fileName);

                btn.textContent = 'Скачать';
                btn.disabled = false;
            });
        });

        // Обработчик для переименования
        document.querySelectorAll('.btn-rename').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;
                const fileCard = btn.closest('.shared-file-card');
                const fileNameElement = fileCard.querySelector('h3');
                const currentName = fileNameElement.textContent;

                const newName = prompt('Введите новое имя файла:', currentName);
                if (newName && newName !== currentName) {
                    btn.textContent = '...';
                    btn.disabled = true;

                    // Здесь нужно добавить метод для переименования в fileManager
                    const success = await this.fileManager.renameFile(fileId, newName, currentName);
                    if (success) {
                        fileNameElement.textContent = newName;
                    }

                    btn.textContent = 'Переименовать';
                    btn.disabled = false;
                }
            });
        });

        // Обработчик для удаления
        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;

                if (confirm('Вы уверены, что хотите удалить этот файл?')) {
                    btn.textContent = '...';
                    btn.disabled = true;

                    // Здесь нужно добавить метод для удаления в fileManager
                    const success = await this.fileManager.deleteFile(fileId);
                    if (success) {
                        btn.closest('.shared-file-card').remove();
                        if (this.sharedFiles.length === 0) {
                            this.filterFiles();
                        }
                    }

                    btn.textContent = 'Удалить';
                    btn.disabled = false;
                }
            });
        });
    }
}

// Инициализация при загрузке
document.addEventListener('DOMContentLoaded', () => {
    window.sharedWithMe = new SharedWithMe();
    window.sharedWithMe.init();
});