console.log("🔥 ЗАГРУЖЕН scriptUser.js (НОВАЯ ВЕРСИЯ)");

document.addEventListener("DOMContentLoaded", () => {
    // Проверяем, не находимся ли мы на неправильном URL
    const currentPath = window.location.pathname;
    console.log("Текущий путь:", currentPath);

    // Если мы на /user/..., перенаправляем на правильный URL
    if (currentPath.startsWith('/user/')) {
        const userId = currentPath.split('/').pop();
        console.log("⚠️ Обнаружен старый формат URL, перенаправляю на правильный:", userId);
        window.location.href = `/myProfile.html?id=${encodeURIComponent(userId)}`;
        return;
    }

    const editBtn = document.getElementById("edit-profile-btn");
    if (!editBtn) {
        console.error("❌ Кнопка редактирования не найдена!");
        return;
    }

    // Получаем ID из URL параметра
    const params = new URLSearchParams(window.location.search);
    let userId = params.get("id");

    console.log("📌 userId из URL =", userId);

    // Если в URL нет, пробуем из localStorage
    if (!userId) {
        userId = localStorage.getItem('lastUserId');
        console.log("📌 userId из localStorage =", userId);
    }

    // Если всё равно нет - ошибка
    if (!userId) {
        console.error("❌ ID пользователя не найден!");
        alert("Ошибка: не удалось определить пользователя");
        window.location.href = "/loginIndex.html";
        return;
    }

    console.log("✅ Загрузка данных пользователя с ID:", userId);

    fetch("/api/user/" + encodeURIComponent(userId))
        .then(res => {
            console.log("📡 Статус ответа /api/user:", res.status);
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            return res.json();
        })
        .then(user => {
            console.log("✅ Данные пользователя получены:", user);
            document.getElementById("user-name").textContent = user.name || "—";
            document.getElementById("user-email").textContent = user.email || "—";
            document.getElementById("user-number").textContent = user.phoneNumber || "—";
        })
        .catch(err => {
            console.error("❌ Ошибка загрузки:", err);
            alert("Не удалось загрузить данные пользователя");
        });

    editBtn.onclick = function () {
        console.log("🖱️ Кнопка редактирования нажата, userId =", userId);
        window.location.href = `/editProfile.html?id=${encodeURIComponent(userId)}`;
    };
});