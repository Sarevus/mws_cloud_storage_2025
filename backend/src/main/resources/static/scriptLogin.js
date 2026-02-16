console.log("🔥 ЗАГРУЖЕН scriptLogin.js (НОВАЯ ВЕРСИЯ)");

const loginBtn = document.getElementById("login-button");

function sendLoginRequest(event) {
    event.preventDefault();

    const email = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value.trim();

    console.log("Логин с email:", email);

    fetch("/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ email, password })
    })
    .then(async response => {
        console.log("Статус ответа /login:", response.status);

        if (response.status === 200) {
            const user = await response.json();
            console.log("Получен пользователь:", user);
            console.log("ID пользователя:", user.id);

            if (!user.id) {
                console.error("❌ ID пользователя пустой!");
                return;
            }

            // ✅ ПРАВИЛЬНЫЙ URL
            const redirectUrl = `/myProfile.html?id=${encodeURIComponent(user.id)}`;
            console.log("✅ Редирект на:", redirectUrl);

            // Сохраняем в localStorage
            localStorage.setItem('lastUserId', user.id);
            console.log("✅ Сохранено в localStorage:", localStorage.getItem('lastUserId'));

            window.location.href = redirectUrl;
        } else {
            const errorText = await response.text();
            console.error("❌ Ошибка:", errorText);
            alert("Ошибка: " + errorText);
        }
    })
    .catch(err => {
        console.error("❌ Ошибка fetch:", err);
        alert("Ошибка сети");
    });
}

loginBtn.onclick = sendLoginRequest;

document.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        sendLoginRequest(event);
    }
});