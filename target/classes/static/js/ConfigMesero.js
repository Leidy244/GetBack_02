// === ELEMENTOS DEL DOM ===
const modal = document.getElementById("settingsModal");
const openSettingsBtn = document.querySelector(".fa-gear")?.closest("a");
const closeSettings = document.getElementById("closeSettings");
const darkModeSwitch = document.getElementById("darkModeSwitch");
const darkModeToggle = document.getElementById("darkModeToggle");
const saveBtn = document.querySelector(".save-btn");

// === INICIALIZACIÓN ===
document.addEventListener('DOMContentLoaded', function() {
	// Cargar configuraciones guardadas
	loadSavedSettings();

	// Sincronizar modo oscuro si existe el toggle flotante
	syncDarkMode();
});

// === ABRIR MODAL ===
if (openSettingsBtn) {
	openSettingsBtn.addEventListener("click", (e) => {
		e.preventDefault();
		abrirModal();
	});
}

function abrirModal() {
	modal.style.display = "flex";
	setTimeout(() => modal.classList.add("show"), 10);

	// Sincronizar estado actual con los switches
	if (darkModeSwitch) {
		const isDark = document.body.classList.contains("dark-mode") ||
			document.documentElement.getAttribute('data-theme') === 'dark';
		darkModeSwitch.checked = isDark;
	}

	// Cargar otras configuraciones
	loadCurrentSettings();
}

// === CERRAR MODAL ===
if (closeSettings) {
	closeSettings.addEventListener("click", () => cerrarModal());
}

if (modal) {
	window.addEventListener("click", (e) => {
		if (e.target === modal) cerrarModal();
	});
}

function cerrarModal() {
	modal.classList.remove("show");
	setTimeout(() => {
		modal.style.display = "none";
	}, 250);
}

// === CARGAR CONFIGURACIONES ACTUALES EN EL MODAL ===
function loadCurrentSettings() {
	const savedSettings = JSON.parse(localStorage.getItem('meseroSettings')) || {};
	const fontSizeSelect = document.getElementById("fontSizeSelect");
	const notificacionesSwitch = document.getElementById("notificacionesSwitch");

	if (fontSizeSelect && savedSettings.fontSize) {
		fontSizeSelect.value = savedSettings.fontSize;
	}


	if (notificacionesSwitch && savedSettings.notificaciones !== undefined) {
		notificacionesSwitch.checked = savedSettings.notificaciones;
	}
}

// === APLICAR TAMAÑO DE FUENTE ===
function applyFontSize(size) {
	const sizes = {
		'normal': '16px',
		'grande': '18px',
		'extra': '20px'
	};

	if (sizes[size]) {
		document.body.style.fontSize = sizes[size];
	}
}

// === GUARDAR CAMBIOS ===
if (saveBtn) {
	saveBtn.addEventListener("click", (e) => {
		e.preventDefault();
		guardarConfiguracion();
	});
}

function guardarConfiguracion() {
	const fontSizeSelect = document.getElementById("fontSizeSelect");
	const notificacionesSwitch = document.getElementById("notificacionesSwitch");

	const settings = {
		fontSize: fontSizeSelect?.value || 'normal',
		notificaciones: notificacionesSwitch?.checked || false
	};

	// Aplicar cambios inmediatamente
	if (fontSizeSelect) {
		applyFontSize(settings.fontSize);
	}

	// Guardar en localStorage
	localStorage.setItem('meseroSettings', JSON.stringify(settings));

	// Mostrar confirmación y cerrar
	mostrarToast("Configuración guardada correctamente");
	cerrarModal();
}

// === TOAST FLOTANTE ===
function mostrarToast(mensaje) {
	const toast = document.createElement("div");
	toast.textContent = mensaje;
	toast.style.cssText = `
        position: fixed;
        bottom: 30px;
        right: 30px;
        background: linear-gradient(90deg, var(--morado), var(--turquesa));
        color: #fff;
        padding: 12px 20px;
        border-radius: 25px;
        font-weight: 500;
        box-shadow: var(--sombra);
        opacity: 0;
        transition: opacity 0.4s ease, transform 0.4s ease;
        transform: translateY(15px);
        z-index: 10000;
        font-size: 14px;
    `;

	document.body.appendChild(toast);

	setTimeout(() => {
		toast.style.opacity = "1";
		toast.style.transform = "translateY(0)";
	}, 100);

	setTimeout(() => {
		toast.style.opacity = "0";
		toast.style.transform = "translateY(15px)";
		setTimeout(() => {
			if (toast.parentNode) {
				toast.remove();
			}
		}, 400);
	}, 2300);
}

// === CERRAR CON TECLA ESC ===
document.addEventListener("keydown", (e) => {
	if (e.key === "Escape" && modal.classList.contains("show")) {
		cerrarModal();
	}
});

// Referencias a los formularios
const formEditar = document.getElementById("formEditar");
const formFoto = document.getElementById("formFoto");

// Botones
const btnEditProfile = document.getElementById("btnEditProfile");
const btnChangePhoto = document.getElementById("btnChangePhoto");
const btnCancelEdit = document.getElementById("btnCancelEdit");
const btnCancelFoto = document.getElementById("cancelFoto");

// Función para mostrar/ocultar formularios
function toggleForm(formToShow, formToHide) {
	if (formToHide.style.display === "block") {
		formToHide.style.display = "none";
	}
	formToShow.style.display = formToShow.style.display === "block" ? "none" : "block";
}

// --- Abrir Formulario de Edición ---
btnEditProfile.addEventListener("click", () => {
	toggleForm(formEditar, formFoto);
});

// --- Abrir Formulario de Foto ---
btnChangePhoto.addEventListener("click", () => {
	toggleForm(formFoto, formEditar);
});

// --- Cerrar formularios ---
btnCancelEdit.addEventListener("click", () => {
	formEditar.style.display = "none";
});

btnCancelFoto?.addEventListener("click", () => {
	formFoto.style.display = "none";
});

// --- Nombre de la imagen seleccionada ---
document.addEventListener("DOMContentLoaded", () => {
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

	// Cerrar formularios al hacer clic fuera de ellos
	document.addEventListener("click", (e) => {
		if (!formEditar.contains(e.target) && !btnEditProfile.contains(e.target) && formEditar.style.display === "block") {
			formEditar.style.display = "none";
		}
		if (!formFoto.contains(e.target) && !btnChangePhoto.contains(e.target) && formFoto.style.display === "block") {
			formFoto.style.display = "none";
		}
	});
});

// Validación básica del formulario
const form = document.querySelector('form[method="post"]');
if (form) {
	form.addEventListener('submit', (e) => {
		const inputs = form.querySelectorAll('input[required]');
		let isValid = true;

		inputs.forEach(input => {
			if (!input.value.trim()) {
				isValid = false;
				input.style.borderColor = 'var(--danger-color)';
			} else {
				input.style.borderColor = '';
			}
		});

		if (!isValid) {
			e.preventDefault();
			alert('Por favor, complete todos los campos requeridos.');
		}
	});
}

// --- Activar/desactivar modo oscuro ---
const toggle = document.getElementById('darkModeToggle');
const icon = toggle.querySelector('i');

// Verifica si el usuario ya tenía modo oscuro activado
if (localStorage.getItem('theme') === 'dark') {
	document.documentElement.setAttribute('data-theme', 'dark');
	icon.classList.replace('fa-moon', 'fa-sun');
}

// Al hacer clic en el botón
toggle.addEventListener('click', () => {
	const currentTheme = document.documentElement.getAttribute('data-theme');
	if (currentTheme === 'dark') {
		document.documentElement.removeAttribute('data-theme');
		icon.classList.replace('fa-sun', 'fa-moon');
		localStorage.setItem('theme', 'light');
	} else {
		document.documentElement.setAttribute('data-theme', 'dark');
		icon.classList.replace('fa-moon', 'fa-sun');
		localStorage.setItem('theme', 'dark');
	}
});

// === ConfigMesero.js ===

// Espera a que todo el contenido del DOM esté listo antes de ejecutar
document.addEventListener("DOMContentLoaded", () => {
  const hamburger = document.getElementById("hamburger"); // Botón hamburguesa
  const navMenu = document.getElementById("navMenu"); // Menú de navegación

  // Cuando se hace clic en el botón hamburguesa
  hamburger.addEventListener("click", () => {
    // Alterna la clase "active" para mostrar/ocultar el menú
    navMenu.classList.toggle("active");

    // También cambia el estilo del botón (opcional)
    hamburger.classList.toggle("open");
  });
});
