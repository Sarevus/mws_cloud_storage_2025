const verifyBtn = document.getElementById("verify-button");
const codeInput = document.getElementById("verification-code");
const emailLabel = document.getElementById("email-label");

const params = new URLSearchParams(window.location.search);
const email = params.get("email");

if (email) {
    emailLabel.textContent = `Код отправлен на: ${email}`;
}

function verifyCode(event) {
    event.preventDefault();

    const code = codeInput.value.trim();

    fetch(`/api/auth/register/confirm?email=${encodeURIComponent(email)}&code=${encodeURIComponent(code)}`, {
        method: "POST",
        credentials: "include"
    })
        .then(async response => {
            if (response.status === 201 || response.ok) {
                const user = await response.json();
                console.log("Пользователь подтвержден:", user);
                window.location.href = `/myProfile?id=${encodeURIComponent(user.id)}`;
                return;
            }

            const error = await response.text();
            alert(error);
        })
        .catch(err => {
            console.error("Ошибка:", err);
            alert("Не верный код");
        });
}

verifyBtn.onclick = verifyCode;

document.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        verifyCode(event);
    }
});