/**
 * File Manager для Cloud Storage
 * API взаимодействие с бэкендом для работы с файлами
 */

class FileManager {
    constructor() {
        this.baseUrl = 'http://localhost:6969'; // ИЗМЕНЕНО: порт 6969 вместо 4567
        this.currentUserId = this.getUserIdFromUrl();
    }

    /**
     * Получает ID пользователя из URL
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
     * GET /api/files?userId={userId}
     */
    async getFiles() {
        if (!this.currentUserId) {
            console.error('Cannot get files: user ID not found');
            return [];
        }

        // Сначала проверяем доступность сервера
        const isServerAlive = await this.checkServerHealth();
        if (!isServerAlive) {
            console.error('Server is not responding');
            alert('Сервер недоступен. Проверьте, запущен ли сервер на localhost:6969');
            return [];
        }

        console.log('✅ Server is alive, fetching files...');

        try {
            const url = `${this.baseUrl}/api/files?userId=${this.currentUserId}`;
            console.log('Fetching from:', url);

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                }
            });

            console.log('Response status:', response.status, response.statusText);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Server error response:', errorText);
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
            console.log(`✅ Got ${files.length} files`);

            // ДЛЯ ОТЛАДКИ: выводим структуру первого файла
            if (files.length > 0) {
                console.log('Пример структуры файла:', files[0]);
                console.log('Все поля файла:', Object.keys(files[0]));
            }

            return files;

        } catch (error) {
            console.error('❌ Error fetching files:', error);
            alert(`Ошибка загрузки файлов: ${error.message}`);
            return [];
        }
    }

    /**
     * Получает категорию файла по MIME-типу или расширению
     */
    getFileCategory(file) {
        // Пытаемся получить MIME-тип из файла
        const mimeType = file.mimeType || file.type || '';
        const fileName = file.originalName || file.name || file.fileName || '';

        // Логируем для отладки
        console.log(`Определяем категорию для файла: ${fileName}, MIME: ${mimeType}`);

        // Сначала пробуем по MIME-типу
        if (mimeType) {
            if (mimeType.startsWith('image/')) return 'image';
            if (mimeType.startsWith('video/')) return 'video';
            if (mimeType.startsWith('audio/')) return 'music';
            if (mimeType.includes('pdf') ||
                mimeType.includes('word') ||
                mimeType.includes('excel') ||
                mimeType.includes('presentation') ||
                mimeType.includes('text/') ||
                mimeType.includes('application/vnd')) {
                return 'document';
            }
        }

        // Если MIME-типа нет или он не определён, используем расширение
        return this.getFileIcon(fileName);
    }

    /**
     * Получает последние N файлов
     * Для профиля - показываем первые 5 файлов
     */
    async getRecentFiles(limit = 5) {
        const allFiles = await this.getFiles();

        if (allFiles.length === 0) {
            return [];
        }

        // Возвращаем первые N файлов из списка
        // (предполагаем, что API возвращает отсортированными от новых к старым)
        return allFiles.slice(0, limit);
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
     * Получает иконку для типа файла
     */
    getFileIcon(filename) {
        if (!filename) return 'file';

        const ext = filename.split('.').pop().toLowerCase();

        const iconMap = {
            // Изображения
            'jpg': 'image', 'jpeg': 'image', 'png': 'image', 'gif': 'image',
            'bmp': 'image', 'svg': 'image', 'webp': 'image', 'ico': 'image',

            // Документы
            'pdf': 'document',
            'doc': 'document', 'docx': 'document',
            'xls': 'document', 'xlsx': 'document',
            'ppt': 'document', 'pptx': 'document',
            'txt': 'document', 'md': 'document', 'rtf': 'document',

            // Видео
            'mp4': 'video', 'avi': 'video', 'mov': 'video', 'mkv': 'video',
            'webm': 'video', 'wmv': 'video', 'flv': 'video', 'm4v': 'video',

            // Аудио
            'mp3': 'music', 'wav': 'music', 'flac': 'music',
            'aac': 'music', 'ogg': 'music', 'm4a': 'music',

            // Архивы
            'zip': 'archive', 'rar': 'archive', '7z': 'archive',
            'tar': 'archive', 'gz': 'archive'
        };

        return iconMap[ext] || 'file';
    }

    /**
     * Загружает файл на сервер
     * POST /api/files/upload?userId={userId}
     */
    async uploadFile(file) {
        if (!this.currentUserId) {
            alert('Ошибка: ID пользователя не найден');
            return null;
        }

        console.log('Starting file upload:', {
            name: file.name,
            size: this.formatFileSize(file.size),
            type: file.type
        });

        const formData = new FormData();
        formData.append('file', file);

        try {
            const url = `${this.baseUrl}/api/files/upload?userId=${this.currentUserId}`;
            console.log('Uploading to:', url);

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
            console.log('Upload response:', result);

            if (!result.success) {
                throw new Error(`Upload failed: ${result.error || 'Unknown error'}`);
            }

            console.log('✅ File uploaded successfully:', result.file);
            alert(`Файл "${file.name}" успешно загружен!`);
            return result.file;

        } catch (error) {
            console.error('❌ Upload error:', error);
            alert(`Ошибка загрузки: ${error.message}`);
            return null;
        }
    }

    /**
     * Скачивает файл
     * GET /api/files/:id/download?userId={userId}
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
     * DELETE /api/files/:id?userId={userId}
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
                headers: {
                    'Content-Type': 'application/json',
                }
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
     * PUT /api/files/:id?userId={userId}
     * Body: {"newName": "новое_имя"}
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
                headers: {
                    'Content-Type': 'application/json',
                },
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
        console.log("33333333", file)
        const fileId = file.id || file.fileId;
        const fileName = file.originalName || file.name || file.fileName || 'Без имени';
        const fileSize = this.formatFileSize(file.size || file.fileSize || 0);
        const fileType = this.getFileIcon(fileName);
        
        // Пытаемся получить дату
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
        
        // Иконка в зависимости от типа файла
        let iconSVG = '';
        switch(fileType) {
            case 'image':
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <circle cx="8.5" cy="8.5" r="1.5"/>
                        <polyline points="21 15 16 10 5 21"/>
                    </svg>`;
                break;
            case 'video':
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <polygon points="10 8 16 12 10 16 10 8"/>
                    </svg>`;
                break;
            case 'music':
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <path d="M9 17H5V9H9V17Z"/>
                        <path d="M19 17H15V5H19V17Z"/>
                        <circle cx="7" cy="18" r="3"/>
                        <circle cx="17" cy="18" r="3"/>
                    </svg>`;
                break;
            case 'document':
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M14 2V8H20" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M16 13H8" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M16 17H8" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M10 9H9H8" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>`;
                break;
            default:
                iconSVG = `
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                        <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M14 2V8H20" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>`;
        }
        
        fileElement.innerHTML = `
            <div class="file-icon">
                ${iconSVG}
            </div>
            <div class="file-info" style="overflow:hidden;">
                <h4 class="file-name">${fileName}</h4>
                <p>${fileType === 'image' ? 'Изображение' : 
                    fileType === 'video' ? 'Видеофайл' : 
                    fileType === 'music' ? 'Аудиофайл' : 
                    fileType === 'document' ? 'Документ' : 'Файл'}</p>
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

        console.log(fileElement);

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
        
        // Показываем индикатор загрузки
        const loadingOverlay = document.getElementById('loading-overlay');
        if (loadingOverlay) loadingOverlay.classList.add('active');
        
        try {
            console.log("111111111");
            const files = await this.getFiles();
            console.log('Updating display with', files.length, 'files');
            
            // Обновляем статистику
            const totalFilesEl = document.getElementById('total-files');
            if (totalFilesEl) {
                totalFilesEl.textContent = `${files.length} файл${files.length % 10 === 1 ? '' : files.length % 10 >= 2 && files.length % 10 <= 4 ? 'а' : 'ов'}`;
            }
            
            // Обновляем каждую категорию
            const categories = ['image', 'videos', 'documents', 'music', 'shared'];

            categories.forEach(category => {
                console.log(category);
                const contentElement = document.getElementById(`${category}-content`);
                if (!contentElement) return;
                
                const filesGrid = contentElement.querySelector('.files-grid');
                console.log("444444",category.slice(0, -1),  filesGrid);
                if (!filesGrid) return;
                
                // Очищаем существующие файлы
                filesGrid.innerHTML = '';
                
                // Фильтруем файлы по категории
                let categoryFiles = [];
                if (category === 'shared') {
                    // Для "Все файлы" показываем все
                    categoryFiles = files;
                } else {
                    categoryFiles = files.filter(file => {
                        const fileCategory = this.getFileCategory(file);
                        const targetCategory = category.slice(0, category.length);
                        console.log("2222", fileCategory, targetCategory, fileCategory === targetCategory);
                        return fileCategory === targetCategory;
                    });
                }
                
                if (categoryFiles.length === 0) {
                    // Показываем сообщение об отсутствии файлов
                    const emptyState = document.createElement('div');
                    emptyState.className = 'empty-state';
                    emptyState.innerHTML = `
                        <h3>Пока нет файлов</h3>
                        <p>Загрузите файлы для хранения</p>
                    `;
                    filesGrid.appendChild(emptyState);
                } else {
                    // Добавляем файлы
                    categoryFiles.forEach(file => {
                        const fileElement = this.createFileElement(file);
                        filesGrid.appendChild(fileElement);
                    });
                }
            });
            
            // Добавляем обработчики событий
            this.attachFileEventHandlers();
            
        } catch (error) {
            console.error('Error updating file display:', error);
        } finally {
            // Скрываем индикатор загрузки
            if (loadingOverlay) loadingOverlay.classList.remove('active');
        }
    }

    /**
     * Добавляет обработчики событий к элементам файлов
     */
    attachFileEventHandlers() {
        // Обработчики скачивания
        document.querySelectorAll('.btn-download').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const fileId = btn.dataset.fileId;
                const fileName = btn.dataset.fileName || 'file';
                
                btn.textContent = 'Скачивание...';
                btn.disabled = true;
                
                const success = await this.downloadFile(fileId, fileName);
                
                btn.textContent = 'Скачать';
                btn.disabled = false;
            });
        });
        
        // Обработчики переименования
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
                        // Обновляем data-атрибут для кнопки скачивания
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
        
        // Обработчики удаления
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
                        // Обновляем статистику
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
        if (!uploadInput) return;
        
        uploadInput.addEventListener('change', async (e) => {
            const files = Array.from(e.target.files);
            
            if (files.length === 0) return;
            
            // Показываем индикатор загрузки
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
            
            // Загружаем файлы по очереди
            let successCount = 0;
            
            for (const file of files) {
                try {
                    const uploadedFile = await this.uploadFile(file);
                    if (uploadedFile) {
                        successCount++;
                    }
                } catch (error) {
                    console.error(`Error uploading ${file.name}:`, error);
                }
            }
            
            // Восстанавливаем оригинальный вид
            uploadArea.innerHTML = originalHTML;
            
            // Обновляем список файлов
            await this.updateFileDisplay();
            
            // Показываем результат
            if (successCount > 0) {
                console.log(`Successfully uploaded ${successCount} files`);
            } else {
                alert('Не удалось загрузить файлы. Проверьте подключение к серверу.');
            }
            
            // Сбрасываем input
            uploadInput.value = '';
        });
    }

    /**
     * Инициализирует все загрузчики файлов
     */
    initAllUploaders() {
        const categories = ['photos', 'videos', 'documents', 'music', 'shared'];
        categories.forEach(category => {
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
        
        // Инициализируем загрузчики
        this.initAllUploaders();
        
        // Загружаем файлы при загрузке страницы
        this.updateFileDisplay();
        
        // Обновляем файлы при переключении категорий
        document.querySelectorAll('.category-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                setTimeout(() => this.attachFileEventHandlers(), 100);
            });
        });
        
        console.log('FileManager initialized successfully');
    }
}

// Экспорт для использования в других файлах
export default FileManager;