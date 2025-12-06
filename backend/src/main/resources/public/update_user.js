console.log("update.js ЗАГРУЖЕН");

document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById('reg-button');
    const deleteBtn = document.getElementById('delete-button');

    console.log("btn =", btn);

    const usernameInput = document.getElementById("name");
    const passwordInput = document.getElementById("reg-password");
    const phoneInput = document.getElementById("phone");
    const emailInput = document.getElementById("email");

    const params = new URLSearchParams(window.location.search);
    const userId = params.get("id");
    console.log("userId из URL:", userId);

    if (!userId) {
        alert("Не удалось определить ID пользователя из URL (ожидаю ?id=UUID)");
        return;
    }

    // подстановка данных из бд
    fetch(`/api/user/${encodeURIComponent(userId)}`, {
        method: "GET",
        headers: {
            "Accept": "application/json"
        }
    })
        .then(async response => {
            console.log("GET /api/user status:", response.status);

            if (!response.ok) {
                const error = await response.text();
                alert("Ошибка загрузки пользователя: " + error);
                return;
            }
            const user = await response.json();
            console.log("Загружен пользователь:", user);
            usernameInput.value = user.name;
            emailInput.value = user.email;
            phoneInput.value = user.phoneNumber;
            passwordInput.value = "";
        })
        .catch(error => {
            console.error("Ошибка при загрузке пользователя:", error);
            alert("Ошибка сети при загрузке пользователя");
        });

    // обновление данных пользователя
    btn.onclick = function (event) {
        event.preventDefault();

        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();
        const phone = phoneInput.value.trim();
        const email = emailInput.value.trim();

        console.log("Перед отправкой (update):", { username, email, phone, password });



        fetch(`/user/${encodeURIComponent(userId)}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                name: username,
                email: email,
                phoneNumber: phone,
                password: password
            })
        })
            .then(async response => {
                console.log("PUT /user status:", response.status);

                const text = await response.text();
                let updatedUser = null;
                try {
                    updatedUser = text ? JSON.parse(text) : null;
                } catch (_) { }

                if (response.ok) {
                    console.log("Обновлён пользователь:", updatedUser);
                    alert("Пользователь обновлён");
                    if (updatedUser && updatedUser.id) {
                        window.location.href = "/user/" + encodeURIComponent(updatedUser.id);
                    }
                    return;
                }

                if (updatedUser && updatedUser.error) {
                    alert("Ошибка обновления: " + updatedUser.error);
                } else {
                    alert("Ошибка обновления: " + text);
                }
            })
            .catch(err => {
                console.error("Ошибка:", err);
                alert("Ошибка сети при обновлении");
            });
    };

    // удаление пользователя
    if (deleteBtn) {
        deleteBtn.onclick = function () {
            const sure = confirm("Точно удалить пользователя?");
            if (!sure) return;

            fetch(`/user/${encodeURIComponent(userId)}`, {
                method: "DELETE"
            })
                .then(async response => {
                    console.log("DELETE /user status:", response.status);

                    if (response.status === 204) {
                        alert("Пользователь удалён");
                        window.location.href = "/";
                        return;
                    }

                    const text = await response.text();
                    let data = null;
                    try {
                        data = text ? JSON.parse(text) : null;
                    } catch (_) { }

                    if (data && data.error) {
                        alert("Ошибка удаления: " + data.error);
                    } else {
                        alert("Ошибка удаления: " + text);
                    }
                })
                .catch(err => {
                    console.error("Ошибка:", err);
                    alert("Ошибка сети при удалении");
                });
        };
    }
});
