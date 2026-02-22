console.log("script.js ЗАГРУЖЕН");

const btn = document.getElementById('reg-button');
console.log("btn =", btn);

function sendRegisterRequest(event) {
    event.preventDefault(); // чтобы форма не перезагружала страницу

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

    fetch("/api/auth/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        credentials: 'include',
        body: JSON.stringify({ name: username, email: email, phoneNumber: phone, password: password })
    })
        .then(async response => {
            console.log("status:", response.status);

            if (response.status === 201) {
                const user = await response.json();
                console.log("Создан пользователь:", user);
                window.location.href = `/myProfile.html?id=${encodeURIComponent(user.id)}`;
                return;
            }

            const error = await response.text();
            alert(error);
        })
        .catch(err => console.error("Ошибка:", err));
}

btn.onclick = sendRegisterRequest;
document.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        sendRegisterRequest(event);
    }
});
