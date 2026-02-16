console.log("скрипт выполняется");

document.addEventListener("DOMContentLoaded", () => {
    console.log("DOM загружен");

    const editBtn = document.getElementById("edit-profile-btn");
    console.log("editBtn найден:", editBtn ? "да" : "нет");

    // Сначала пробуем получить ID из URL
    const params = new URLSearchParams(window.location.search);
    let userId = params.get("id");

    console.log("userId из URL =", userId);

    // Если в URL нет ID, пробуем взять из localStorage
    if (!userId) {
        userId = localStorage.getItem('lastUserId');
        console.log("userId из localStorage =", userId);

        // Если нашли в localStorage, обновляем URL
        if (userId) {
            console.log("Обновляем URL с ID из localStorage");
            window.location.href = `/myProfile.html?id=${encodeURIComponent(userId)}`;
            return;
        }
    }

    if (!userId) {
        console.error("❌ ID пользователя не найден ни в URL, ни в localStorage");
        alert("Ошибка: не удалось определить пользователя");
        window.location.href = "/loginIndex.html";
        return;
    }

    console.log("Загрузка данных пользователя с ID:", userId);

    fetch("/api/user/" + encodeURIComponent(userId))
        .then(res => {
            console.log("Статус ответа /api/user:", res.status);
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            return res.json();
        })
        .then(user => {
            console.log("Данные пользователя:", user);
            document.getElementById("user-name").textContent = user.name || "—";
            document.getElementById("user-email").textContent = user.email || "—";
            document.getElementById("user-number").textContent = user.phoneNumber || "—";
        })
        .catch(err => {
            console.error("Ошибка загрузки:", err);
            alert("Не удалось загрузить данные пользователя");
        });

    editBtn.onclick = function () {
        console.log("Кнопка редактирования нажата, userId =", userId);
        if (userId) {
            window.location.href = `/editProfile.html?id=${encodeURIComponent(userId)}`;
        }
    };
});