const loginBtn = document.getElementById("login-button");

function sendLoginRequest(event) {
    event.preventDefault();

    const email = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value.trim();

    console.log("Отправка запроса на вход:", { email });

    fetch("/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ email, password })
    })
    .then(async response => {
        console.log("Статус ответа:", response.status);
        
        const data = await response.json();
        
        if (response.status === 200 && data.success) {
            console.log("Вход успешен:", data);
            
            // Перенаправляем на страницу профиля
            window.location.href = "/profile";
        } else {
            const errorMessage = data.error || "Неизвестная ошибка";
            alert("Ошибка входа: " + errorMessage);
        }
    })
    .catch(err => {
        console.error("Ошибка сети:", err);
        alert("Ошибка сети при входе");
    });
}

// Привязываем обработчики
if (loginBtn) {
    loginBtn.onclick = sendLoginRequest;
}

// Обработка нажатия Enter
document.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        sendLoginRequest(event);
    }
});