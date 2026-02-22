console.log("скрипт выполняется");

document.addEventListener("DOMContentLoaded", () => {
    const editBtn = document.getElementById("edit-profile-btn");

    const params = new URLSearchParams(window.location.search);
    const userId = params.get("id");

    console.log("userId =", userId);

    fetch("/api/user/" + encodeURIComponent(userId), {
        credentials: 'include'
    })
        .then(res => res.json())
        .then(user => {
            document.getElementById("user-name").textContent = user.name;
            document.getElementById("user-email").textContent = user.email;
            document.getElementById("user-number").textContent = user.phoneNumber;
        });

    editBtn.onclick = function () {
        console.log("click!");
        window.location.href = `/editProfile.html?id=${encodeURIComponent(userId)}`;
    };
});

// Проверка сессии каждую минуту
setInterval(() => {
    fetch("/api/auth/me", { credentials: 'include' })
        .then(res => {
            if (!res.ok) {
                console.log("Сессия истекла, редирект...");
                window.location.href = "/loginIndex.html";
            }
        })
        .catch(() => {
            window.location.href = "/loginIndex.html";
        });
}, 60000);