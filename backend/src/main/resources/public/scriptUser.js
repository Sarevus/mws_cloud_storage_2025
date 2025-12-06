console.log("скрипт выполняется");

document.addEventListener("DOMContentLoaded", () => {
    const editBtn = document.getElementById("edit-profile-btn");

    const params = new URLSearchParams(window.location.search);
    const userId = params.get("id");

    console.log("userId =", userId);

    fetch("/api/user/" + encodeURIComponent(userId))
        .then(res => res.json())
        .then(user => {
            document.getElementById("user-name").textContent = user.name;
            document.getElementById("user-email").textContent = user.email;
            document.getElementById("user-number").textContent = user.phoneNumber;
        });

    editBtn.onclick = function () {
        console.log("click!");
        window.location.href = "/user/" + encodeURIComponent(userId) + "/edit";
    };
});
