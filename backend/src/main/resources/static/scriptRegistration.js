console.log("script.js ЗАГРУЖЕН");

const btn = document.getElementById('reg-button');
console.log("btn =", btn);

function sendRegisterRequest(event) {
    event.preventDefault();

    console.log("Кнопка нажата");
    const usernameInput = document.getElementById("name");
    const passwordInput = document.getElementById("reg-password");
    const phoneInput = document.getElementById("phone");
    const emailInput = document.getElementById("email");

    const username = usernameInput.value.trim();
    const password = passwordInput.value.trim();
    const phone = phoneInput.value.trim();
    const email = emailInput.value.trim();

    console.log("Перед отправкой:", { username, email, phone, password });

    fetch("/api/auth/register/request", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({
            name: username,
            email: email,
            phoneNumber: phone,
            password: password
        })
    })
        .then(async response => {
            console.log("status:", response.status);

            if (response.ok) {
                window.location.href = `/verificationPage?email=${encodeURIComponent(email)}`;
                return;
            }

            const error = await response.text();
            alert(error);
        })
        .catch(err => {
            console.error("Ошибка:", err);
            alert("Не удалось отправить запрос");
        });
}

btn.onclick = sendRegisterRequest;

document.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        sendRegisterRequest(event);
    }
});