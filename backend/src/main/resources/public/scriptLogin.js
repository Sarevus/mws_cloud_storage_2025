const loginBtn = document.getElementById("login-button");

loginBtn.onclick = function () {
    const email = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value.trim();

    fetch("/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ email, password })
    })
    .then(async response => {
        console.log("status:", response.status);

        if (response.status === 200) {
            const user = await response.json();
            window.location.href = "/user/" + encodeURIComponent(user.id);
        } else {
            const errorText = await response.text();
            alert("Ошибка: " + errorText);
        }
    })
    .catch(err => console.error("Ошибка:", err));
};
