console.log("скрипт выполняется");
var pathname = new URLSearchParams(window.location.search);
var userId = pathname.get("id");
fetch("/api/user/" + encodeURIComponent(userId))
  .then(response => response.json())
  .then(user => {
      document.getElementById("user-name").textContent = user.name;
  });
