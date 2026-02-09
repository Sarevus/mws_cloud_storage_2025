/**
 * File Manager для Cloud Storage
 * API взаимодействие с бэкендом для работы с файлами
 */
class FileManager {
    constructor() {
        this.baseUrl = 'http://localhost:6969';
        this.currentUserId = this.getUserIdFromUrl();
        // Категории из fileExchange.html
        this.categories = ['photos', 'videos', 'documents', 'music', 'shared'];

        console.log('FileManager создан для пользователя:', this.currentUserId);
    }

    /**
     * Извлекает параметр ID пользователя из URL
     */
    getUserIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const userId = params.get('id');

        if (!userId) {
            console.error('User ID not found in URL');
            return null;
        }

        return userId;
    }

    /**
     * Проверяет доступность сервера
     */
    async checkServerHealth() {
        try {
            const response = await fetch(`${this.baseUrl}/api/test`, {
                method: 'GET'
            });
            return response.ok;
        } catch (error) {
            console.error('Server health check failed:', error);
            return false;
        }
    }

    /**
     * Получает список файлов пользователя
     */
    async getFiles() {
        if (!this.currentUserId) {
            console.error('Cannot get files: user ID not found');
            return [];
        }

        const isServerAlive = await this.checkServerHealth();
        if (!isServerAlive) {
            console.error('Server is not responding');
            alert('Сервер недоступен. Проверьте, запущен ли сервер на localhost:6969');
            return [];
        }

        console.log('Server is alive, fetching files...');

        try {
            const url = `${this.baseUrl}/api/files?userId=${this.currentUserId}`;
            console.log('Fetching from:', url);

            const response = await fetch(url, {
                method: 'GET',
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

            // Логирование каждого полученного файла
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
        // категория с сервера, которая хранится в БД
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

        // Изображения
        if (mimeType.startsWith('image/') ||
            fileName.match(/\.(jpg|jpeg|png|gif|bmp|webp|svg|tiff)$/)) {
            return 'photos';
        }

        // Видео
        if (mimeType.startsWith('video/') ||
            fileName.match(/\.(mp4|avi|mov|wmv|flv|mkv|webm|mpeg|mpg)$/)) {
            return 'videos';
        }

        // Аудио
        if (mimeType.startsWith('audio/') ||
            fileName.match(/\.(mp3|wav|flac|aac|ogg|wma|m4a)$/)) {
            return 'music';
        }

        // Документы
        if (mimeType.includes('pdf') ||
            mimeType.includes('msword') ||
            mimeType.includes('excel') ||
            mimeType.includes('presentation') ||
            fileName.match(/\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|csv)$/)) {
            return 'documents';
        }

        // Если не удалось определить - оставляем "shared"
        console.warn(`Не удалось определить категорию для файла: ${file.name} (${file.type})`);
        return 'shared';
    }

    /**
     * Загружает файл на сервер С КАТЕГОРИЕЙ
     */
    /**
     * Загружает файл на сервер С КАТЕГОРИЕЙ
     */
    async uploadFile(file, category = null) {
        if (!this.currentUserId) {
            alert('Ошибка: ID пользователя не найден');
            return null;
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
            // Определяем финальную категорию
            let finalCategory = category;

            // Если категория не указана или это 'shared' - определяем автоматически
            if (!finalCategory || finalCategory === 'shared') {
                finalCategory = this.detectCategoryFromFile(file);
                console.log(`Автоматически определена категория: "${finalCategory}"`);
            }

            // Всегда передаем категорию на сервер (даже если это 'shared')
            let url = `${this.baseUrl}/api/files/upload?userId=${this.currentUserId}`;
            url += `&category=${encodeURIComponent(finalCategory)}`;

            console.log(`Передаём категорию на сервер: "${finalCategory}"`);
            console.log('Upload URL:', url);

            const response = await fetch(url, {
                method: 'POST',
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

            console.log('✅ Файл успешно загружен:', result.file);
            alert(`Файл "${file.name}" успешно загружен!`);
            return result.file;

        } catch (error) {
            console.error('❌ Upload error:', error);
            alert(`Ошибка загрузки: ${error.message}`);
            return null;
        }
    }

    /**
     * Форматирует размер файла в читаемый вид
     */
    formatFileSize(bytes) {
        if (bytes === 0 || !bytes) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    /**
     * Скачивает файл
     */
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

    /**
     * Удаляет файл
     */
    async deleteFile(fileId) {
        if (!this.currentUserId) {
            alert('Ошибка: ID пользователя не найден');
            return false;
        }

        if (!confirm('Вы уверены, что хотите удалить этот файл?')) {
            return false;
        }

        try {
            const url = `${this.baseUrl}/api/files/${fileId}?userId=${this.currentUserId}`;
            console.log('Deleting from:', url);

            const response = await fetch(url, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' }
            });

            console.log('Delete response status:', response.status);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Delete failed: ${response.status} - ${errorText}`);
            }

            const result = await response.json();

            if (!result.success) {
                throw new Error(`Delete failed: ${result.error || 'Unknown error'}`);
            }

            console.log('✅ File deleted successfully');
            alert('Файл успешно удалён!');
            return true;

        } catch (error) {
            console.error('❌ Delete error:', error);
            alert(`Ошибка удаления: ${error.message}`);
            return false;
        }
    }

    /**
     * Переименовывает файл
     */
    async renameFile(fileId, newName) {
        if (!this.currentUserId) {
            alert('Ошибка: ID пользователя не найден');
            return null;
        }

        if (!newName || newName.trim() === '') {
            alert('Имя файла не может быть пустым');
            return null;
        }

        try {
            const url = `${this.baseUrl}/api/files/${fileId}?userId=${this.currentUserId}`;
            console.log('Renaming via:', url);

            const response = await fetch(url, {
                method: 'PUT',
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

    /**
     * Создает HTML элемент файла
     */
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
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <circle cx="8.5" cy="8.5" r="1.5"/>
                        <polyline points="21 15 16 10 5 21"/>
                    </svg>`;
                fileTypeName = 'Изображение';
                break;
            case 'videos':
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <polygon points="10 8 16 12 10 16 10 8"/>
                    </svg>`;
                fileTypeName = 'Видеофайл';
                break;
            case 'music':
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <path d="M9 17H5V9H9V17Z"/>
                        <path d="M19 17H15V5H19V17Z"/>
                        <circle cx="7" cy="18" r="3"/>
                        <circle cx="17" cy="18" r="3"/>
                    </svg>`;
                fileTypeName = 'Аудиофайл';
                break;
            case 'documents':
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M14 2V8H20" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M16 13H8" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M16 17H8" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M10 9H9H8" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>`;
                fileTypeName = 'Документ';
                break;
            default:
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M14 2V8H20" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>`;
                fileTypeName = 'Файл';
        }

        fileElement.innerHTML = `
            <div class="file-icon">
                ${iconSVG}
            </div>
            <div class="file-info" style="overflow:hidden;">
                <h4 class="file-name">${fileName}</h4>
                <p>${fileTypeName}</p>
                <div class="file-meta">
                    <span>${fileSize}</span>
                    <span>${uploadDate}</span>
                </div>
            </div>
            <div class="file-actions">
                <button class="btn-download" data-file-id="${fileId}" data-file-name="${fileName}">
                    Скачать
                </button>
                <button class="btn-rename" data-file-id="${fileId}">
                    Переименовать
                </button>
                <button class="btn-delete" data-file-id="${fileId}">
                    Удалить
                </button>
            </div>
        `;

        return fileElement;
    }

    /**
     * Обновляет отображение файлов
     */
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

            const totalFilesEl = document.getElementById('total-files');
            if (totalFilesEl) {
                totalFilesEl.textContent = `${files.length} файл${files.length % 10 === 1 ? '' : files.length % 10 >= 2 && files.length % 10 <= 4 ? 'а' : 'ов'}`;
            }

            // обновляем категории
            this.categories.forEach(categoryName => {
                console.log(`🔍 Обновляем категорию: "${categoryName}"`);

                const contentElement = document.getElementById(`${categoryName}-content`);
                if (!contentElement) {
                    console.error(`❌ Элемент #${categoryName}-content не найден!`);
                    return;
                }

                const filesGrid = contentElement.querySelector('.files-grid');
                if (!filesGrid) {
                    console.error(`❌ .files-grid внутри #${categoryName}-content не найден!`);
                    return;
                }

                filesGrid.innerHTML = '';

                let categoryFiles = [];
                if (categoryName === 'shared') {
                    categoryFiles = files;
                } else {
                    categoryFiles = files.filter(file => {
                        const fileCategory = this.getFileCategory(file);
                        const matches = fileCategory === categoryName;
                        console.log(`  - "${file.originalName}": ${fileCategory} === ${categoryName} ? ${matches}`);
                        return matches;
                    });
                }

                console.log(`  📊 Файлов в категории "${categoryName}": ${categoryFiles.length}`);

                if (categoryFiles.length === 0) {
                    const emptyState = document.createElement('div');
                    emptyState.className = 'empty-state';
                    emptyState.innerHTML = `
                        <h3>Пока нет файлов</h3>
                        <p>Загрузите файлы для хранения</p>
                    `;
                    filesGrid.appendChild(emptyState);
                } else {
                    categoryFiles.forEach(file => {
                        const fileElement = this.createFileElement(file);
                        filesGrid.appendChild(fileElement);
                    });
                }
            });

            this.attachFileEventHandlers();

        } catch (error) {
            console.error('Error updating file display:', error);
        } finally {
            if (loadingOverlay) loadingOverlay.classList.remove('active');
        }
    }

    /**
     * Добавляет обработчики событий
     */
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

                    const updatedFile = await this.renameFile(fileId, newName);
                    if (updatedFile) {
                        fileNameElement.textContent = updatedFile.originalName || newName;
                        const downloadBtn = fileCard.querySelector('.btn-download');
                        if (downloadBtn) {
                            downloadBtn.dataset.fileName = updatedFile.originalName || newName;
                        }
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
    }

    /**
     * Инициализирует загрузку файлов для категории
     */
    initCategoryUpload(categoryId) {
        const uploadInput = document.getElementById(`${categoryId}-upload`);
        if (!uploadInput) {
            console.error(`❌ Элемент #${categoryId}-upload не найден!`);
            return;
        }

        console.log(`✅ Инициализация загрузчика для категории: "${categoryId}"`);

        uploadInput.addEventListener('change', async (e) => {
            const files = Array.from(e.target.files);

            if (files.length === 0) return;

            console.log(`Загрузка ${files.length} файлов в категорию "${categoryId}"`);

            const uploadArea = uploadInput.closest('.upload-area');
            const originalHTML = uploadArea.innerHTML;
            uploadArea.innerHTML = `
                <div class="upload-progress">
                    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <circle cx="12" cy="12" r="10" stroke-opacity="0.3"/>
                        <path d="M12 2a10 10 0 0 1 10 10" stroke-linecap="round"/>
                    </svg>
                    <h3>Загрузка ${files.length} файлов...</h3>
                    <p>Пожалуйста, подождите</p>
                </div>
            `;

            let successCount = 0;
            let uploadedCategories = {};

            for (const file of files) {
                try {
                    // ✅ Для категории "shared" передаем null, чтобы определилась автоматически
                    const uploadCategory = categoryId === 'shared' ? null : categoryId;
                    const uploadedFile = await this.uploadFile(file, uploadCategory);
                    if (uploadedFile) {
                        successCount++;
                        const cat = this.getFileCategory(uploadedFile);
                        uploadedCategories[cat] = (uploadedCategories[cat] || 0) + 1;
                    }
                } catch (error) {
                    console.error(`Error uploading ${file.name}:`, error);
                }
            }

            uploadArea.innerHTML = originalHTML;
            await this.updateFileDisplay();

            if (successCount > 0) {
                console.log(`✅ Успешно загружено ${successCount} файлов`);

                // Показываем информацию о распределении по категориям
                const categoriesList = Object.entries(uploadedCategories)
                    .map(([cat, count]) => `${cat}: ${count}`)
                    .join(', ');
                console.log(`📊 Распределение по категориям: ${categoriesList}`);
            } else {
                alert('Не удалось загрузить файлы. Проверьте подключение к серверу.');
            }

            uploadInput.value = '';
        });
    }

    /**
     * Инициализирует все загрузчики файлов
     */
    initAllUploaders() {
        console.log('Инициализация загрузчиков для категорий:', this.categories);
        this.categories.forEach(category => {
            this.initCategoryUpload(category);
        });
    }

    /**
     * Инициализирует FileManager
     */
    init() {
        if (!this.currentUserId) {
            console.error('FileManager: User ID not found');
            alert('Ошибка: ID пользователя не найден в URL');
            return;
        }

        console.log('Initializing FileManager for user:', this.currentUserId);

        this.initAllUploaders();
        this.updateFileDisplay();

        document.querySelectorAll('.category-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                setTimeout(() => this.attachFileEventHandlers(), 100);
            });
        });

        console.log('FileManager initialized successfully');
    }

    /**
     * Получает последние N файлов
     */
    async getRecentFiles(limit = 5) {
        const allFiles = await this.getFiles();

        if (allFiles.length === 0) {
            return [];
        }

        return allFiles.slice(0, limit);
    }
}

// Экспорт для использования в других файлах
export default FileManager;