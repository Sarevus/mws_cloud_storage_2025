/**
 * File Manager для Cloud Storage
 * API взаимодействие с бэкендом для работы с файлами
 */
class FileManager {
    constructor() {
        this.baseUrl = 'http://localhost:6969';
        this.currentUserId = this.getUserIdFromUrl();
        this.currentUserEmail = null;
        this.categories = ['photos', 'videos', 'documents', 'music', 'shared'];

        console.log('FileManager создан для пользователя:', this.currentUserId);
    }

    /**
     * Извлекает параметр ID пользователя из URL
     */
    getUserIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        let userId = params.get('id');
        if (!userId) {
            userId = params.get('userId');
        }
        if (!userId) {
            console.error('User ID not found in URL');
            return null;
        }
        return userId;
    }

    /**
     * Загружает email пользователя из сессии
     */
    async loadUserEmail() {
        let email = sessionStorage.getItem('userEmail');
        if (email) {
            this.currentUserEmail = email;
            console.log('Email из sessionStorage:', email);
            return email;
        }

        try {
            console.log('Получаем email из сессии через API...');
            const response = await fetch(`${this.baseUrl}/api/auth/me`, {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                const userData = await response.json();
                if (userData.email) {
                    this.currentUserEmail = userData.email;
                    sessionStorage.setItem('userEmail', userData.email);
                    console.log('✅ Email получен из сессии:', userData.email);
                    return userData.email;
                }
            } else {
                console.log('Не удалось получить email, статус:', response.status);
            }
        } catch (error) {
            console.error('Ошибка получения email:', error);
        }

        console.warn('⚠️ Email пользователя не найден');
        return null;
    }

    /**
     * Получает список файлов пользователя
     */
    async getFiles() {
        if (!this.currentUserId) {
            console.error('Cannot get files: user ID not found');
            return [];
        }

        console.log('Server is alive, fetching files...');

        try {
            const url = `${this.baseUrl}/api/files?userId=${this.currentUserId}`;
            console.log('Fetching from:', url);

            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            console.log('Response status:', response.status, response.statusText);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }

            const result = await response.json();
            console.log('API response:', result);

            if (!result.success) {
                console.error('API returned error:', result.error);
                alert(`Ошибка API: ${result.error}`);
                return [];
            }

            const files = result.files || [];
            console.log(`✅ Получено ${files.length} файлов`);

            files.forEach((file, index) => {
                console.log(`Файл ${index + 1}: "${file.originalName}"`, {
                    категорияИзБД: file.category,
                    mimeType: file.mimeType,
                    размер: file.size
                });
            });

            return files;

        } catch (error) {
            console.error('❌ Error fetching files:', error);
            alert(`Ошибка загрузки файлов: ${error.message}`);
            return [];
        }
    }

    /**
     * Получает категорию файла ИЗ ДАННЫХ СЕРВЕРА (из БД)
     */
    getFileCategory(file) {
        const categoryFromServer = file.category;
        console.log(`Файл "${file.originalName}": категория "${categoryFromServer}"`);
        return categoryFromServer || "general";
    }

    /**
     * Определяет категорию файла на основе MIME-типа или расширения
     */
    detectCategoryFromFile(file) {
        const mimeType = file.type.toLowerCase();
        const fileName = file.name.toLowerCase();

        if (mimeType.startsWith('image/') ||
            fileName.match(/\.(jpg|jpeg|png|gif|bmp|webp|svg|tiff)$/)) {
            return 'photos';
        }
        if (mimeType.startsWith('video/') ||
            fileName.match(/\.(mp4|avi|mov|wmv|flv|mkv|webm|mpeg|mpg)$/)) {
            return 'videos';
        }
        if (mimeType.startsWith('audio/') ||
            fileName.match(/\.(mp3|wav|flac|aac|ogg|wma|m4a)$/)) {
            return 'music';
        }
        if (mimeType.includes('pdf') ||
            mimeType.includes('msword') ||
            mimeType.includes('excel') ||
            mimeType.includes('presentation') ||
            fileName.match(/\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|csv)$/)) {
            return 'documents';
        }
        console.warn(`Не удалось определить категорию для файла: ${file.name} (${file.type})`);
        return 'shared';
    }

    /**
     * Загружает файл на сервер
     */
    async uploadFile(file, category = null) {
        if (!this.currentUserId) {
            alert('Ошибка: ID пользователя не найден');
            return null;
        }

        const existingFiles = await this.getFiles();
        const existingFile = existingFiles.find(f => f.originalName === file.name);

        if (existingFile) {
            if (!confirm(`Файл с таким именем уже существует. Хотите заменить его?`)) {
                return null;
            }
            await this.deleteFile(existingFile.id, true);
        }

        console.log('Загрузка файла:', {
            имя: file.name,
            категория: category,
            размер: this.formatFileSize(file.size),
            тип: file.type
        });

        const formData = new FormData();
        formData.append('file', file);

        try {
            let finalCategory = category;
            if (!finalCategory || finalCategory === 'shared') {
                finalCategory = this.detectCategoryFromFile(file);
                console.log(`Автоматически определена категория: "${finalCategory}"`);
            }

            let url = `${this.baseUrl}/api/files/upload?userId=${this.currentUserId}`;
            url += `&category=${encodeURIComponent(finalCategory)}`;

            console.log(`Передаём категорию на сервер: "${finalCategory}"`);
            console.log('Upload URL:', url);

            const response = await fetch(url, {
                method: 'POST',
                credentials: 'include',
                body: formData
            });

            console.log('Upload response status:', response.status);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Upload failed: ${response.status} - ${errorText}`);
            }

            const result = await response.json();

            if (!result.success) {
                throw new Error(`Upload failed: ${result.error || 'Unknown error'}`);
            }

            if (!existingFile) {
                console.log('✅ Файл успешно загружен:', result.file);
                alert(`Файл "${file.name}" успешно загружен!`);
                return result.file;
            } else {
                console.log('✅ Файл успешно обновлён:', result.file);
                alert(`Файл "${file.name}" успешно обновлён!`);
                return result.file;
            }

        } catch (error) {
            console.error('❌ Upload error:', error);
            alert(`Ошибка загрузки: ${error.message}`);
            return null;
        }
    }

    formatFileSize(bytes) {
        if (bytes === 0 || !bytes) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    async downloadFile(fileId, fileName) {
        if (!this.currentUserId) {
            alert('Ошибка: ID пользователя не найден');
            return false;
        }

        try {
            const url = `${this.baseUrl}/api/files/${fileId}/download?userId=${this.currentUserId}`;
            console.log('Downloading from:', url);

            const response = await fetch(url);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Download failed: ${response.status} - ${errorText}`);
            }

            const blob = await response.blob();
            const urlObj = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = urlObj;
            a.download = fileName || 'file';
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(urlObj);
            document.body.removeChild(a);

            console.log('✅ Download successful');
            return true;

        } catch (error) {
            console.error('❌ Download error:', error);
            alert(`Ошибка скачивания: ${error.message}`);
            return false;
        }
    }

    async renameFile(fileId, newName, originalFile) {
        if (!this.currentUserId) {
            alert('Ошибка: ID пользователя не найден');
            return null;
        }

        if (!newName || newName.trim() === '') {
            alert('Имя файла не может быть пустым');
            return null;
        }

        const newType = newName.substring(newName.lastIndexOf(".") + 1).toLowerCase();
        const currentType = originalFile.substring(originalFile.lastIndexOf(".") + 1).toLowerCase();

        if (newType !== currentType) {
            alert('Нельзя изменить расширение файла');
            return null;
        }

        try {
            const url = `${this.baseUrl}/api/files/${fileId}?userId=${this.currentUserId}`;
            console.log('Renaming via:', url);

            const response = await fetch(url, {
                method: 'PUT',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ newName: newName.trim() })
            });

            console.log('Rename response status:', response.status);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Rename failed: ${response.status} - ${errorText}`);
            }

            const result = await response.json();

            if (!result.success) {
                throw new Error(`Rename failed: ${result.error || 'Unknown error'}`);
            }

            console.log('✅ File renamed successfully:', result.file);
            alert('Файл успешно переименован!');
            return result.file;

        } catch (error) {
            console.error('❌ Rename error:', error);
            alert(`Ошибка переименования: ${error.message}`);
            return null;
        }
    }

    async updateStorageInfo() {
        if (!this.currentUserId) return;

        try {
            const response = await fetch(`${this.baseUrl}/api/storage/info`, {
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();

            const progressBar = document.getElementById('storage-progress');
            const usedEl = document.getElementById('storage-used');
            const totalEl = document.getElementById('storage-total');
            const percentEl = document.getElementById('storage-percent');

            if (progressBar && usedEl && totalEl && percentEl) {
                progressBar.style.width = data.percent + '%';
                usedEl.textContent = this.formatFileSize(data.used);
                totalEl.textContent = this.formatFileSize(data.total);
                percentEl.textContent = `(${data.percent}%)`;

                console.log('✅ Прогресс-бар обновлён:', {
                    used: this.formatFileSize(data.used),
                    total: this.formatFileSize(data.total),
                    percent: data.percent + '%'
                });
            }
        } catch (error) {
            console.error('❌ Ошибка загрузки информации о хранилище:', error);
        }
    }

    createFileElement(file) {
        const fileId = file.id || file.fileId;
        const fileName = file.originalName || file.name || file.fileName || 'Без имени';
        const fileSize = this.formatFileSize(file.size || file.fileSize || 0);
        const fileCategory = this.getFileCategory(file);

        let uploadDate = 'Недавно';
        const possibleDateFields = ['uploadDate', 'createdAt', 'createdDate', 'uploadTime', 'timestamp'];
        for (const field of possibleDateFields) {
            if (file[field]) {
                try {
                    uploadDate = new Date(file[field]).toLocaleDateString('ru-RU');
                    break;
                } catch (e) {
                    console.log(`Could not parse date field ${field}:`, file[field]);
                }
            }
        }

        const fileElement = document.createElement('div');
        fileElement.className = 'file-card';
        fileElement.dataset.fileId = fileId;
        fileElement.dataset.category = fileCategory;

        let iconSVG = '';
        let fileTypeName = '';

        switch(fileCategory) {
            case 'photos':
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>`;
                fileTypeName = 'Изображение';
                break;
            case 'videos':
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><polygon points="10 8 16 12 10 16 10 8"/></svg>`;
                fileTypeName = 'Видеофайл';
                break;
            case 'music':
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M9 17H5V9H9V17Z"/><path d="M19 17H15V5H19V17Z"/><circle cx="7" cy="18" r="3"/><circle cx="17" cy="18" r="3"/></svg>`;
                fileTypeName = 'Аудиофайл';
                break;
            case 'documents':
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 2V8H20" stroke-linecap="round" stroke-linejoin="round"/><path d="M16 13H8" stroke-linecap="round" stroke-linejoin="round"/><path d="M16 17H8" stroke-linecap="round" stroke-linejoin="round"/><path d="M10 9H9H8" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
                fileTypeName = 'Документ';
                break;
            default:
                iconSVG = `<svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 2V8H20" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
                fileTypeName = 'Файл';
        }

        fileElement.innerHTML = `
            <div class="file-icon">${iconSVG}</div>
            <div class="file-info" style="overflow:hidden;">
                <h4 class="file-name">${this.escapeHtml(fileName)}</h4>
                <p>${fileTypeName}</p>
                <div class="file-meta">
                    <span>${fileSize}</span>
                    <span>${uploadDate}</span>
                </div>
            </div>
            <div class="file-actions">
                <button class="btn-download" data-file-id="${fileId}" data-file-name="${fileName}">Скачать</button>
                <button class="btn-rename" data-file-id="${fileId}">Переименовать</button>
                <button class="btn-share" data-file-id="${fileId}" data-file-name="${fileName}">Поделиться</button>
                <button class="btn-delete" data-file-id="${fileId}">Удалить</button>
            </div>
        `;
        return fileElement;
    }

    escapeHtml(str) {
        if (!str) return '';
        return str.replace(/[&<>]/g, m => {
            if (m === '&') return '&amp;';
            if (m === '<') return '&lt;';
            if (m === '>') return '&gt;';
            return m;
        });
    }

    async updateFileDisplay() {
        if (!this.currentUserId) {
            console.error('Cannot update files: user ID not found');
            return;
        }

        const loadingOverlay = document.getElementById('loading-overlay');
        if (loadingOverlay) loadingOverlay.classList.add('active');

        try {
            const files = await this.getFiles();
            console.log('📁 Всего файлов:', files.length);

            await this.updateStorageInfo();

            const totalFilesEl = document.getElementById('total-files');
            if (totalFilesEl) {
                totalFilesEl.textContent = `${files.length} файл${files.length % 10 === 1 ? '' : files.length % 10 >= 2 && files.length % 10 <= 4 ? 'а' : 'ов'}`;
            }

            for (const categoryName of this.categories) {
                console.log(`🔍 Обновляем категорию: "${categoryName}"`);

                const contentElement = document.getElementById(`${categoryName}-content`);
                if (!contentElement) {
                    console.error(`❌ Элемент #${categoryName}-content не найден!`);
                    continue;
                }

                const filesGrid = contentElement.querySelector('.files-grid');
                if (!filesGrid) {
                    console.error(`❌ .files-grid внутри #${categoryName}-content не найден!`);
                    continue;
                }

                filesGrid.innerHTML = '';

                let categoryFiles = [];
                if (categoryName === 'shared') {
                    categoryFiles = files;
                } else {
                    categoryFiles = files.filter(file => {
                        const fileCategory = this.getFileCategory(file);
                        return fileCategory === categoryName;
                    });
                }

                console.log(`  📊 Файлов в категории "${categoryName}": ${categoryFiles.length}`);

                if (categoryFiles.length === 0) {
                    const emptyState = document.createElement('div');
                    emptyState.className = 'empty-state';
                    emptyState.innerHTML = '<h3>Пока нет файлов</h3><p>Загрузите файлы для хранения</p>';
                    filesGrid.appendChild(emptyState);
                } else {
                    categoryFiles.forEach(file => {
                        filesGrid.appendChild(this.createFileElement(file));
                    });
                }
            }

            this.attachFileEventHandlers();

        } catch (error) {
            console.error('Error updating file display:', error);
        } finally {
            if (loadingOverlay) loadingOverlay.classList.remove('active');
        }
    }

    attachFileEventHandlers() {
        document.querySelectorAll('.btn-download').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;
                const fileName = btn.dataset.fileName || 'file';

                btn.textContent = 'Скачивание...';
                btn.disabled = true;

                await this.downloadFile(fileId, fileName);

                btn.textContent = 'Скачать';
                btn.disabled = false;
            });
        });

        document.querySelectorAll('.btn-rename').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;
                const fileCard = btn.closest('.file-card');
                const fileNameElement = fileCard.querySelector('.file-name');
                const currentName = fileNameElement.textContent;

                const newName = prompt('Введите новое имя файла:', currentName);
                if (newName && newName !== currentName) {
                    btn.textContent = 'Сохранение...';
                    btn.disabled = true;

                    const updatedFile = await this.renameFile(fileId, newName, currentName);
                    if (updatedFile) {
                        fileNameElement.textContent = updatedFile.originalName || newName;

                        const allFiles = document.querySelectorAll(`.file-card[data-file-id="${fileId}"]`);
                        allFiles.forEach(card => {
                            const name = card.querySelector('.file-name');
                            if (name) name.textContent = newName;
                            const downloadBtn = card.querySelector('.btn-download');
                            if (downloadBtn) downloadBtn.dataset.fileName = updatedFile.originalName || newName;
                        });
                    }

                    btn.textContent = 'Переименовать';
                    btn.disabled = false;
                }
            });
        });

        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;
                const fileCard = btn.closest('.file-card');

                btn.textContent = 'Удаление...';
                btn.disabled = true;

                const success = await this.deleteFile(fileId);
                if (success) {
                    fileCard.style.opacity = '0.5';
                    setTimeout(() => {
                        fileCard.remove();
                        this.updateFileDisplay();
                    }, 300);
                } else {
                    btn.textContent = 'Удалить';
                    btn.disabled = false;
                }
            });
        });

        document.querySelectorAll('.btn-share').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;
                window.location.href = `share.html?fileId=${fileId}&userId=${this.currentUserId}`;
            });
        });
    }

    initCategoryUpload(categoryId) {
        const uploadInput = document.getElementById(`${categoryId}-upload`);

        if (!uploadInput) {
            console.error(`❌ Элемент #${categoryId}-upload не найден!`);
            return;
        }

        console.log(`✅ Инициализация загрузчика для категории: "${categoryId}"`);

        const uploadArea = uploadInput.closest('.upload-area');
        if (!uploadArea) {
            console.error(`❌ .upload-area не найден для #${categoryId}-upload!`);
            return;
        }

        document.addEventListener('change', async (e) => {
            if (e.target.id !== `${categoryId}-upload`) return;

            const files = Array.from(e.target.files);
            if (files.length === 0) return;

            console.log(`Загрузка ${files.length} файлов в категорию "${categoryId}"`);

            const originalHTML = uploadArea.innerHTML;
            uploadArea.innerHTML = `<div class="upload-progress"><h3>Загрузка ${files.length} файлов...</h3><p>Пожалуйста, подождите</p></div>`;

            let successCount = 0;

            for (const file of files) {
                const uploadCategory = categoryId === 'shared' ? null : categoryId;
                const uploadedFile = await this.uploadFile(file, uploadCategory);
                if (uploadedFile) successCount++;
            }

            uploadArea.innerHTML = originalHTML;
            await this.updateFileDisplay();

            if (successCount > 0) {
                console.log(`✅ Успешно загружено ${successCount} файлов`);
            }

            e.target.value = '';
        });
    }

    initAllUploaders() {
        console.log('Инициализация загрузчиков для категорий:', this.categories);
        this.categories.forEach(category => {
            this.initCategoryUpload(category);
        });
    }

    async init() {
        if (!this.currentUserId) {
            console.error('FileManager: User ID not found');
            alert('Ошибка: ID пользователя не найден в URL');
            return;
        }

        console.log('Initializing FileManager for user:', this.currentUserId);
        await this.loadUserEmail();
        console.log('User email:', this.currentUserEmail);

        this.initAllUploaders();
        await this.updateStorageInfo();
        await this.updateFileDisplay();
        this.initDeleteAllButtons();

        document.querySelectorAll('.category-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                setTimeout(() => this.attachFileEventHandlers(), 100);
            });
        });

        console.log('FileManager initialized successfully');
    }

    getRecentFiles(limit = 5) {
        return this.getFiles().then(files => files.slice(0, limit));
    }

    initDeleteAllButtons() {
        const deleteAllButtons = [
            'delete-all-photos', 'delete-all-videos', 'delete-all-documents',
            'delete-all-music', 'delete-all-shared'
        ];

        deleteAllButtons.forEach(btnId => {
            const btn = document.getElementById(btnId);
            if (btn) {
                btn.onclick = async (event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    const category = btnId.replace('delete-all-', '');
                    const originalText = btn.textContent;
                    btn.textContent = 'Удаление...';
                    btn.disabled = true;

                    if (category === 'shared') {
                        await this.deleteAllFiles();
                    } else {
                        await this.deleteFilesByCategory(category);
                    }

                    btn.textContent = originalText;
                    btn.disabled = false;
                };
            }
        });
    }

    async deleteAllFiles() {
        if (!this.currentUserId) return false;
        if (!confirm('⚠️ ВНИМАНИЕ! Удалить все файлы?')) return false;
        if (!confirm('Вы точно уверены?')) return false;

        try {
            const url = `${this.baseUrl}/api/files?userId=${this.currentUserId}&category=shared`;
            const response = await fetch(url, { method: 'DELETE', credentials: 'include' });
            const result = await response.json();
            if (result.success) {
                await this.updateFileDisplay();
                alert('✅ Все файлы удалены');
                return true;
            }
            return false;
        } catch (error) {
            console.error('❌ Delete all error:', error);
            return false;
        }
    }

    async deleteFile(fileId, skipConfirm = false) {
        if (!this.currentUserId) return false;
        if (!skipConfirm && !confirm('Вы уверены, что хотите удалить этот файл?')) return false;

        try {
            const url = `${this.baseUrl}/api/files/${fileId}?userId=${this.currentUserId}`;
            const response = await fetch(url, { method: 'DELETE', credentials: 'include' });
            const result = await response.json();
            return result.success;
        } catch (error) {
            console.error('❌ Delete error:', error);
            return false;
        }
    }

    async deleteFilesByCategory(category) {
        if (!this.currentUserId) return false;

        const categories = { 'photos': 'фотографии', 'videos': 'видео', 'documents': 'документы', 'music': 'музыку', 'shared': 'все файлы' };
        if (!confirm(`Вы уверены, что хотите удалить все ${categories[category]}?`)) return false;

        try {
            const url = `${this.baseUrl}/api/files?userId=${this.currentUserId}&category=${category}`;
            const response = await fetch(url, { method: 'DELETE', credentials: 'include' });
            const result = await response.json();
            if (result.success) {
                await this.updateFileDisplay();
                alert('✅ Удалено!');
                return true;
            }
            return false;
        } catch (error) {
            alert('Ошибка: ' + error.message);
            return false;
        }
    }

    async getFileAccessors(fileId) {
        if (!this.currentUserId) return [];
        try {
            const response = await fetch(`${this.baseUrl}/api/permission/${fileId}/accessors`, {
                method: 'GET',
                credentials: 'include'
            });
            return await response.json();
        } catch (error) {
            console.error('❌ Error fetching accessors:', error);
            return [];
        }
    }

    async shareFile(fileId, targetUserEmail, role) {
        if (!this.currentUserId) return false;
        try {
            const response = await fetch(`${this.baseUrl}/api/permission/share`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ fileId, userEmail: targetUserEmail, role })
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            alert('✅ Доступ предоставлен');
            return true;
        } catch (error) {
            console.error('❌ Error sharing file:', error);
            alert(`Ошибка: ${error.message}`);
            return false;
        }
    }

    async revokeAccess(fileId, targetUserId) {
        if (!this.currentUserId) return false;
        try {
            const response = await fetch(`${this.baseUrl}/api/permission/${fileId}/revoke/${targetUserId}`, {
                method: 'DELETE',
                credentials: 'include'
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            alert('✅ Доступ отозван');
            return true;
        } catch (error) {
            console.error('❌ Error revoking access:', error);
            return false;
        }
    }

    async getSharedWithMe() {
        if (!this.currentUserId) return [];
        try {
            const response = await fetch(`${this.baseUrl}/api/permission/shared-with-me`, {
                method: 'GET',
                credentials: 'include'
            });
            return await response.json();
        } catch (error) {
            console.error('❌ Error fetching shared files:', error);
            return [];
        }
    }

    async getUnattachedFiles(userId) {
        try {
            const response = await fetch(`${this.baseUrl}/api/files?userId=${userId}&unattached=true`, {
                credentials: 'include'
            });
            const data = await response.json();
            return data.files || [];
        } catch (error) {
            console.error('❌ Error fetching unattached files:', error);
            return [];
        }
    }

    /**
     * Привязывает существующий файл к папке (БЕЗ ПОВТОРНОЙ ЗАГРУЗКИ)
     */
    async bindFileToFolder(fileId, folderId) {
        console.log('=== bindFileToFolder START ===');
        console.log('File ID:', fileId);
        console.log('Folder ID:', folderId);
        console.log('Current user email:', this.currentUserEmail);

        if (!folderId) {
            console.error('❌ Не указан folderId');
            return false;
        }

        if (!this.currentUserEmail) {
            await this.loadUserEmail();
            if (!this.currentUserEmail) {
                console.error('❌ Не удалось получить email');
                return false;
            }
        }

        try {
            console.log('📁 Привязка файла к папке...');
            const response = await fetch(`${this.baseUrl}/api/folders/${folderId}/files`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fileIds: [fileId],
                    addedBy: this.currentUserEmail
                })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Ошибка привязки: ${response.status} - ${errorText}`);
            }

            console.log(`✅ Файл ${fileId} привязан к папке ${folderId}`);
            return true;

        } catch (error) {
            console.error('❌ Ошибка привязки файла к папке:', error);
            return false;
        }
    }
}

export default FileManager;