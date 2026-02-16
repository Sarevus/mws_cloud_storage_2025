console.log("🔥 ЗАГРУЖЕН scriptRegistration.js (НОВАЯ ВЕРСИЯ)");

const btn = document.getElementById('reg-button');

function sendRegisterRequest(event) {
    event.preventDefault();

    console.log("Кнопка нажата");
    var usernameInput = document.getElementById("name");
    var passwordInput = document.getElementById("reg-password");
    var phoneInput = document.getElementById("phone");
    var emailInput = document.getElementById("email");

    var username = usernameInput.value.trim();
    var password = passwordInput.value.trim();
    var phone = phoneInput.value.trim();
    var email = emailInput.value.trim();

    console.log("Перед отправкой:", { username, email, phone, password });

    fetch("/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ name: username, email: email, phoneNumber: phone, password: password })
    })
    .then(async response => {
        console.log("Статус ответа /register:", response.status);

        if (response.status === 201) {
            const user = await response.json();
            console.log("Создан пользователь:", user);
            console.log("ID пользователя:", user.id);

            if (!user.id) {
                console.error("❌ ID пользователя пустой!");
                return;
            }

            // ✅ ПРАВИЛЬНЫЙ URL
            const redirectUrl = `/myProfile.html?id=${encodeURIComponent(user.id)}`;
            console.log("✅ Редирект на:", redirectUrl);

            localStorage.setItem('lastUserId', user.id);
            console.log("✅ Сохранено в localStorage:", localStorage.getItem('lastUserId'));

            window.location.href = redirectUrl;
        } else {
            const error = await response.text();
            console.error("❌ Ошибка:", error);
            alert(error);
        }
    })
    .catch(err => {
        console.error("❌ Ошибка fetch:", err);
        alert("Ошибка сети");
    });
}

btn.onclick = sendRegisterRequest;

document.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        sendRegisterRequest(event);
    }
});