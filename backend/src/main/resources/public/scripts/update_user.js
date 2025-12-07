console.log("update_user.js загружен");

document.addEventListener("DOMContentLoaded", () => {
    const saveBtn = document.getElementById('save-button');
    const cancelBtn = document.getElementById('cancel-button');
    const deleteBtn = document.getElementById('delete-button');
    const usernameInput = document.getElementById("name");
    const phoneInput = document.getElementById("phone");
    const emailInput = document.getElementById("email");

    console.log("Загрузка данных пользователя для редактирования...");

    // Загружаем текущие данные пользователя
    fetch("/user", {
        method: "GET",
        headers: {
            "Accept": "application/json"
        },
        credentials: 'include'
    })
    .then(async response => {
        console.log("Статус ответа /user:", response.status);
        
        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("Ошибка загрузки пользователя");
        }
        
        const data = await response.json();
        console.log("Данные пользователя для редактирования:", data);
        
        if (data.success && data.user) {
            const user = data.user;
            usernameInput.value = user.name || "";
            emailInput.value = user.email || "";
            phoneInput.value = user.phoneNumber || "";
        }
    })
    .catch(error => {
        console.error("Ошибка при загрузке пользователя:", error);
        alert("Ошибка загрузки данных пользователя");
    });

    // Обработчик сохранения изменений
    if (saveBtn) {
        saveBtn.onclick = function (event) {
            event.preventDefault();

            const username = usernameInput.value.trim();
            const phone = phoneInput.value.trim();
            const email = emailInput.value.trim();

            console.log("Сохранение изменений:", { username, email, phone });

            // Обновление основных данных
            fetch("/user", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                credentials: 'include',
                body: JSON.stringify({
                    name: username,
                    email: email,
                    phoneNumber: phone
                })
            })
            .then(async response => {
                console.log("Статус ответа /user (PUT):", response.status);
                
                const data = await response.json();
                
                if (response.ok && data.success) {
                    alert("Данные успешно обновлены!");
                    window.location.href = "/profile";
                } else {
                    const errorMessage = data.error || "Неизвестная ошибка";
                    alert("Ошибка обновления: " + errorMessage);
                }
            })
            .catch(err => {
                console.error("Ошибка сети:", err);
                alert("Ошибка сети при обновлении");
            });
        };
    }

    // Обработчик отмены
    if (cancelBtn) {
        cancelBtn.onclick = function () {
            if (confirm("Отменить изменения?")) {
                window.location.href = "/profile";
            }
        };
    }

    // Обработчик удаления аккаунта
    if (deleteBtn) {
        deleteBtn.onclick = function () {
            if (confirm("Вы уверены, что хотите удалить аккаунт? Это действие необратимо.")) {
                fetch("/user", {
                    method: "DELETE",
                    credentials: 'include'
                })
                .then(async response => {
                    const data = await response.json();
                    
                    if (response.ok && data.success) {
                        alert("Аккаунт успешно удален!");
                        window.location.href = "/";
                    } else {
                        const errorMessage = data.error || "Неизвестная ошибка";
                        alert("Ошибка удаления: " + errorMessage);
                    }
                })
                .catch(err => {
                    console.error("Ошибка сети:", err);
                    alert("Ошибка сети при удалении");
                });
            }
        };
    }
});