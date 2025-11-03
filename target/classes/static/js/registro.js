document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const password = document.getElementById("clave");
    const confirmPassword = document.getElementById("confirmarclave");
    const errorMsg = document.getElementById("error-msg");

	
	
    confirmPassword.addEventListener("input", () => {
        if (confirmPassword.value === "") {
            confirmPassword.classList.remove("valid", "invalid");
            errorMsg.style.display = "none";
            return;
        }

        if (confirmPassword.value === password.value) {
            confirmPassword.classList.add("valid");
            confirmPassword.classList.remove("invalid");
            errorMsg.style.display = "none";
        } else {
            confirmPassword.classList.add("invalid");
            confirmPassword.classList.remove("valid");
            errorMsg.style.display = "block";
        }
    });

    form.addEventListener("submit", (e) => {
        if (password.value !== confirmPassword.value) {
            e.preventDefault();
            confirmPassword.classList.add("invalid");
            confirmPassword.classList.remove("valid");
            errorMsg.style.display = "block";
            alert("Las contraseñas deben coincidir");
        }
    });
});
