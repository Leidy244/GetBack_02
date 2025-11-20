
document.addEventListener("DOMContentLoaded", () => {
    // ===== Formularios de editar perfil y cambiar foto =====
    const formEditar = document.getElementById("formEditar");
    const formFoto = document.getElementById("formFoto");

    const btnEditProfile = document.getElementById("btnEditProfile");
    const btnChangePhoto = document.getElementById("btnChangePhoto");
    const btnCancelEdit = document.getElementById("btnCancelEdit");
    const btnCancelFoto = document.getElementById("cancelFoto");

    // Si no estamos en la vista de configuración de caja, salimos
    if (formEditar && formFoto && btnEditProfile && btnChangePhoto) {

        const toggleForm = (formToShow, formToHide) => {
            if (formToHide) formToHide.style.display = "none";
            if (formToShow) {
                formToShow.style.display =
                    formToShow.style.display === "block" ? "none" : "block";
            }
        };

        // Abrir formulario de edición
        btnEditProfile.addEventListener("click", () => {
            toggleForm(formEditar, formFoto);
        });

        // Abrir formulario de foto
        btnChangePhoto.addEventListener("click", () => {
            toggleForm(formFoto, formEditar);
        });

        // Cerrar formularios
        if (btnCancelEdit) {
            btnCancelEdit.addEventListener("click", () => {
                formEditar.style.display = "none";
            });
        }
        if (btnCancelFoto) {
            btnCancelFoto.addEventListener("click", () => {
                formFoto.style.display = "none";
            });
        }

        // Nombre del archivo y (opcional) preview de foto
        const inputFoto = document.getElementById("inputFoto");
        const nombreArchivo = document.getElementById("nombreArchivo");

        if (inputFoto && nombreArchivo) {
            inputFoto.addEventListener("change", () => {
                const archivo = inputFoto.files[0];
                nombreArchivo.textContent = archivo
                    ? archivo.name
                    : "Ningún archivo seleccionado";
            });
        }
    }

    // ===== Modal de configuración (apariencia) =====
    const adminSettingsBtn = document.getElementById("adminSettingsBtn");
    const adminSettingsModal = document.getElementById("adminsettingsModal");
    const closeSettingsBtn = document.getElementById("closeSettings");
    const saveSettingsBtn = document.getElementById("saveSettingsBtn");

    if (adminSettingsBtn && adminSettingsModal) {
        adminSettingsBtn.addEventListener("click", () => {
            adminSettingsModal.classList.add("show");
        });
    }

    if (closeSettingsBtn && adminSettingsModal) {
        closeSettingsBtn.addEventListener("click", () => {
            adminSettingsModal.classList.remove("show");
        });
    }

    if (adminSettingsModal) {
        adminSettingsModal.addEventListener("click", (e) => {
            if (e.target === adminSettingsModal) {
                adminSettingsModal.classList.remove("show");
            }
        });
    }

    if (saveSettingsBtn && adminSettingsModal) {
        saveSettingsBtn.addEventListener("click", () => {
            adminSettingsModal.classList.remove("show");
        });
    }
});