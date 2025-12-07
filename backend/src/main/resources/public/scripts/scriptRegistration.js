console.log("scriptRegistration.js загружен");

const btn = document.getElementById('reg-button');

function sendRegisterRequest(event) {
    event.preventDefault();

    const username = document.getElementById("name").value.trim();
    const password = document.getElementById("reg-password").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const email = document.getElementById("email").value.trim();

    console.log("Отправка регистрации:", { username, email, phone });

    fetch("/auth/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ 
            name: username, 
            email: email, 
            phoneNumber: phone, 
            password: password 
        })
    })
    .then(async response => {
        console.log("Статус ответа:", response.status);
        
        const data = await response.json();
        
        if (response.status === 201 && data.success) {
            console.log("Регистрация успешна:", data);
            
            // Автоматически логинимся после регистрации
            fetch("/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ email, password })
            })
            .then(async loginResponse => {
                const loginData = await loginResponse.json();
                if (loginResponse.ok && loginData.success) {
                    window.location.href = "/profile";
                } else {
                    window.location.href = "/login";
                }
            })
            .catch(() => {
                window.location.href = "/login";
            });
            
        } else {
            const errorMessage = data.error || "Неизвестная ошибка";
            alert("Ошибка регистрации: " + errorMessage);
        }
    })
    .catch(err => {
        console.error("Ошибка сети:", err);
        alert("Ошибка сети при регистрации");
    });
}

// Привязываем обработчики
if (btn) {
    btn.onclick = sendRegisterRequest;
}

// Обработка нажатия Enter
document.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        sendRegisterRequest(event);
    }
});