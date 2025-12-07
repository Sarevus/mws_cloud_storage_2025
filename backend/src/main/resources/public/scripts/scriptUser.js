console.log("scriptUser.js загружен");

document.addEventListener("DOMContentLoaded", () => {
    // Элементы страницы
    const editBtn = document.getElementById("edit-profile-btn");
    const logoutBtn = document.getElementById("logout-btn");
    const filesList = document.getElementById("files-list");
    const userName = document.getElementById("user-name");
    const userEmail = document.getElementById("user-email");
    const userNumber = document.getElementById("user-number");

    // Загружаем данные пользователя
    function loadUserData() {
        console.log("Загрузка данных пользователя...");
        
        fetch("/user", {
            method: "GET",
            headers: {
                "Accept": "application/json"
            },
            credentials: 'include' // Важно для отправки куки
        })
        .then(async response => {
            console.log("Статус ответа /user:", response.status);
            
            if (!response.ok) {
                if (response.status === 401) {
                    // Не авторизован - перенаправляем на логин
                    window.location.href = "/login";
                    return;
                }
                throw new Error("Ошибка загрузки пользователя");
            }
            
            const data = await response.json();
            console.log("Данные пользователя:", data);
            
            if (data.success && data.user) {
                const user = data.user;
                userName.textContent = user.name || "Имя не указано";
                userEmail.textContent = user.email || "Email не указан";
                userNumber.textContent = user.phoneNumber || "Телефон не указан";
            }
        })
        .catch(error => {
            console.error("Ошибка при загрузке пользователя:", error);
            alert("Ошибка загрузки данных пользователя");
        });
    }

    // Загружаем список файлов
    function loadUserFiles() {
        console.log("Загрузка файлов пользователя...");
        
        fetch("/files", {
            method: "GET",
            headers: {
                "Accept": "application/json"
            },
            credentials: 'include'
        })
        .then(async response => {
            if (!response.ok) {
                console.log("Файлы не загружены или нет прав");
                return;
            }
            
            const data = await response.json();
            console.log("Файлы пользователя:", data);
            
            if (data.success && data.files && data.files.length > 0) {
                renderFiles(data.files);
            } else {
                filesList.innerHTML = '<div class="empty-state">Нет загруженных файлов</div>';
            }
        })
        .catch(error => {
            console.error("Ошибка при загрузке файлов:", error);
        });
    }

    // Отображаем файлы
    function renderFiles(files) {
        filesList.innerHTML = '';
        
        files.forEach(file => {
            const fileItem = document.createElement('div');
            fileItem.className = 'file-item';
            fileItem.innerHTML = `
                <div class="file-icon">
                    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.9 22 6 22H18C19.1 22 20 21.1 20 20V8L14 2Z" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M14 2V8H20" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M16 13H8" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M16 17H8" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M10 9H9H8" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </div>
                <div class="file-info">
                    <h4>${file.originalName}</h4>
                    <p>${file.formattedSize} • ${file.mimeType}</p>
                </div>
                <div class="file-actions">
                    <button class="file-btn download-file" data-file-id="${file.id}">Скачать</button>
                </div>
            `;
            filesList.appendChild(fileItem);
        });

        // Добавляем обработчики для кнопок скачивания
        document.querySelectorAll('.download-file').forEach(btn => {
            btn.addEventListener('click', function() {
                const fileId = this.getAttribute('data-file-id');
                window.open(`/files/${fileId}/download`, '_blank');
            });
        });
    }

    // Обработчик кнопки редактирования
    if (editBtn) {
        editBtn.onclick = function () {
            console.log("Переход к редактированию профиля");
            window.location.href = "/edit-profile";
        };
    }

    // Обработчик кнопки выхода
    if (logoutBtn) {
        logoutBtn.onclick = function () {
            console.log("Выход из системы");
            
            fetch("/auth/logout", {
                method: "POST",
                credentials: 'include'
            })
            .then(async response => {
                const data = await response.json();
                if (data.success) {
                    window.location.href = "/";
                } else {
                    alert("Ошибка при выходе");
                }
            })
            .catch(error => {
                console.error("Ошибка при выходе:", error);
                window.location.href = "/";
            });
        };
    }

    // Загружаем данные при открытии страницы
    loadUserData();
    loadUserFiles();
});