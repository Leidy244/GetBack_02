(function () {
    try {
        const saved = localStorage.getItem("caja-config");
        if (!saved) return;
        const cfg = JSON.parse(saved) || {};
        const root = document.documentElement;

        if (cfg.tema && cfg.tema !== "default") {
            root.setAttribute("data-admin-theme", cfg.tema);
        }
        if (Number.isFinite(cfg.contraste)) {
            root.setAttribute("data-admin-contrast", String(cfg.contraste));
        }
        if (cfg.fontSize) {
            root.setAttribute("data-admin-font-size", cfg.fontSize);
        }
    } catch (e) {
        console.error("No se pudo aplicar configuración inicial de caja", e);
    }
})();

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

    const notificacionesSwitch = document.getElementById("notificacionesSwitch");
    const contrastRange = document.getElementById("contrastRange");
    const themeSelect = document.getElementById("themeSelect");
    const fontSizeSelect = document.getElementById("fontSizeSelect");

    const applyCajaConfig = (config, options = {}) => {
        const body = document.body;
        const root = document.documentElement;

        const cfg = {
            notificaciones: config.notificaciones ?? true,
            contraste: Number.isFinite(config.contraste) ? config.contraste : 1,
            tema: config.tema || "default",
            fontSize: config.fontSize || "pequeno",
        };

        if (notificacionesSwitch) {
            notificacionesSwitch.checked = !!cfg.notificaciones;
        }
        if (contrastRange) {
            contrastRange.value = String(cfg.contraste);
        }
        if (themeSelect) {
            themeSelect.value = cfg.tema;
        }
        if (fontSizeSelect) {
            fontSizeSelect.value = cfg.fontSize;
        }

        if (root) {
            if (cfg.tema && cfg.tema !== "default") {
                root.setAttribute("data-admin-theme", cfg.tema);
            } else {
                root.removeAttribute("data-admin-theme");
            }

            root.setAttribute("data-admin-contrast", String(cfg.contraste));
            root.setAttribute("data-admin-font-size", cfg.fontSize);
        }

        if (body) {
            body.setAttribute("data-admin-font-size", cfg.fontSize);
        }

        if (!options.silent) {
            try {
                localStorage.setItem("caja-config", JSON.stringify(cfg));
            } catch (e) {
                console.error("No se pudo guardar la configuración de caja", e);
            }
        }
    };

    try {
        const saved = localStorage.getItem("caja-config");
        if (saved) {
            const parsed = JSON.parse(saved);
            applyCajaConfig(parsed, { silent: true });
        }
    } catch (e) {
        console.error("No se pudo leer la configuración de caja", e);
    }

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

    // Aplicar en tiempo real (sin guardar todavía) al mover los controles
    const applyLiveFromControls = () => {
        let contraste = 1;
        if (contrastRange) {
            const parsed = Number(contrastRange.value);
            contraste = Number.isFinite(parsed) ? parsed : 1;
        }

        const liveCfg = {
            notificaciones: notificacionesSwitch ? notificacionesSwitch.checked : true,
            contraste,
            tema: themeSelect ? themeSelect.value : "default",
            fontSize: fontSizeSelect ? fontSizeSelect.value : "pequeno",
        };
        applyCajaConfig(liveCfg, { silent: true });
    };

    if (contrastRange) {
        contrastRange.addEventListener("input", applyLiveFromControls);
    }
    if (themeSelect) {
        themeSelect.addEventListener("change", applyLiveFromControls);
    }
    if (fontSizeSelect) {
        fontSizeSelect.addEventListener("change", applyLiveFromControls);
    }

    if (saveSettingsBtn && adminSettingsModal) {
        saveSettingsBtn.addEventListener("click", () => {
            let contraste = 1;
            if (contrastRange) {
                const parsed = Number(contrastRange.value);
                contraste = Number.isFinite(parsed) ? parsed : 1;
            }

            const cfg = {
                notificaciones: notificacionesSwitch ? notificacionesSwitch.checked : true,
                contraste,
                tema: themeSelect ? themeSelect.value : "default",
                fontSize: fontSizeSelect ? fontSizeSelect.value : "pequeno",
            };

            applyCajaConfig(cfg);
            adminSettingsModal.classList.remove("show");
        });
    }
});