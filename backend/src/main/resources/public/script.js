console.log("script.js ЗАГРУЖЕН");

const btn = document.getElementById('reg-button');
console.log("btn =", btn);


btn.onclick = function () {
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

    }).then(response => {
        console.log("status:", response.status);
        return response.text();
        })
        .then(text => console.log("Ответ сервера:", text))
        .catch(err => console.error("Ошибка:", err));

}