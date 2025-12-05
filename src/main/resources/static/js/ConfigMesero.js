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
	const themeSelect = document.getElementById("themeSelect");
	const contrastRange = document.getElementById("contrastRange");

	if (fontSizeSelect && savedSettings.fontSize) {
		fontSizeSelect.value = savedSettings.fontSize;
	}

	if (notificacionesSwitch && savedSettings.notificaciones !== undefined) {
		notificacionesSwitch.checked = savedSettings.notificaciones;
	}

	if (themeSelect && savedSettings.theme) {
		themeSelect.value = savedSettings.theme;
	}

	if (contrastRange && savedSettings.contrastLevel !== undefined) {
		contrastRange.value = savedSettings.contrastLevel;
	}
}

// === APLICAR TAMAÑO DE FUENTE ===
function applyFontSize(size) {
	const sizes = {
		'pequeno': '14px',
		'mediano': '16px',
		'grande': '18px'
	};

	if (sizes[size]) {
		// Aplicar al root (html) para que rem escale todo el sitio
		document.documentElement.style.fontSize = sizes[size];
		// Mantener en body por compatibilidad con estilos heredados
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

function applyTheme(theme) {
	if (theme && theme !== 'default') {
		// Usamos un atributo específico para colores para no romper data-theme="dark"
		// que se usa para el modo oscuro.
		// Por ejemplo: data-color="azul" o data-color="verde".
		document.documentElement.setAttribute('data-color', theme);
	} else {
		// Tema por defecto: quitamos el atributo para usar los colores base de :root
		document.documentElement.removeAttribute('data-color');
	}
}

function applyContrastLevel(level) {
	// level: 0 (bajo), 1 (medio), 2 (alto)
	if (level === undefined || level === null) {
		document.documentElement.removeAttribute('data-contrast');
		return;
	}
	const normalized = String(level);
	document.documentElement.setAttribute('data-contrast', normalized);
}

function guardarConfiguracion() {
	const fontSizeSelect = document.getElementById("fontSizeSelect");
	const notificacionesSwitch = document.getElementById("notificacionesSwitch");
	const themeSelect = document.getElementById("themeSelect");
	const contrastRange = document.getElementById("contrastRange");

	const settings = {
		fontSize: fontSizeSelect?.value || 'normal',
		notificaciones: notificacionesSwitch?.checked || false,
		theme: themeSelect?.value || 'default',
		contrastLevel: contrastRange ? parseInt(contrastRange.value, 10) : 0
	};

	// Aplicar cambios inmediatamente
	if (fontSizeSelect) {
		applyFontSize(settings.fontSize);
	}

	if (themeSelect) {
		applyTheme(settings.theme);
	}

	applyContrastLevel(settings.contrastLevel);

	// Guardar en localStorage
	localStorage.setItem('meseroSettings', JSON.stringify(settings));

	// Mostrar confirmación y cerrar
	mostrarToast("Configuración guardada correctamente");
	cerrarModal();
}

// === UTILIDADES DE CONFIGURACIÓN ===
function loadSavedSettings() {
	try {
		const saved = JSON.parse(localStorage.getItem('meseroSettings')) || {};
		if (saved.fontSize) {
			applyFontSize(saved.fontSize);
		}
		if (saved.theme) {
			applyTheme(saved.theme);
		}
		if (saved.contrastLevel !== undefined) {
			applyContrastLevel(saved.contrastLevel);
		}
	} catch (e) {
		console.warn('No se pudieron cargar las configuraciones del mesero', e);
	}
}

function syncDarkMode() {
	const theme = localStorage.getItem('theme');
	if (theme === 'dark') {
		document.documentElement.setAttribute('data-theme', 'dark');
	}
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
	if (!formToShow || !formToHide) return;
	if (formToHide.style.display === "block") {
		formToHide.style.display = "none";
	}
	formToShow.style.display = formToShow.style.display === "block" ? "none" : "block";
}

// Solo configurar listeners de perfil si existen los elementos (páginas de configuración)
if (btnEditProfile && btnChangePhoto && btnCancelEdit && formEditar && formFoto) {
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
}

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

	// Cerrar formularios al hacer clic fuera de ellos (solo si existen)
	if (formEditar && formFoto && btnEditProfile && btnChangePhoto) {
		document.addEventListener("click", (e) => {
			if (!formEditar.contains(e.target) && !btnEditProfile.contains(e.target) && formEditar.style.display === "block") {
				formEditar.style.display = "none";
			}
			if (!formFoto.contains(e.target) && !btnChangePhoto.contains(e.target) && formFoto.style.display === "block") {
				formFoto.style.display = "none";
			}
		});
	}
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
const icon = toggle?.querySelector('i');

// Verifica si el usuario ya tenía modo oscuro activado
if (localStorage.getItem('theme') === 'dark') {
	document.documentElement.setAttribute('data-theme', 'dark');
	if (icon) {
		icon.classList.replace('fa-moon', 'fa-sun');
	}
}

// Al hacer clic en el botón
if (toggle) {
	toggle.addEventListener('click', () => {
		const currentTheme = document.documentElement.getAttribute('data-theme');
		if (currentTheme === 'dark') {
			document.documentElement.removeAttribute('data-theme');
			if (icon) icon.classList.replace('fa-sun', 'fa-moon');
			localStorage.setItem('theme', 'light');
		} else {
			document.documentElement.setAttribute('data-theme', 'dark');
			if (icon) icon.classList.replace('fa-moon', 'fa-sun');
			localStorage.setItem('theme', 'dark');
		}
	});
}

// === ConfigMesero.js ===

// Espera a que todo el contenido del DOM esté listo antes de ejecutar
document.addEventListener("DOMContentLoaded", () => {
  const hamburger = document.getElementById("hamburger"); // Botón hamburguesa
  const navMenu = document.getElementById("navMenu"); // Menú de navegación

  if (hamburger && navMenu) {
    // Cuando se hace clic en el botón hamburguesa
    hamburger.addEventListener("click", () => {
      // Alterna la clase "active" para mostrar/ocultar el menú
      navMenu.classList.toggle("active");

      // También cambia el estilo del botón (opcional)
      hamburger.classList.toggle("open");
    });
  }
});

window.meseroApp = window.meseroApp || {};
window.meseroApp.logout = function() {
  const ejecutar = () => {
    try {
      fetch('/logout', { method: 'POST', headers: { 'Accept': 'text/html' } })
        .then(() => { window.location.href = '/login'; })
        .catch(() => { window.location.href = '/login'; });
    } catch (_) { window.location.href = '/login'; }
  };
  if (typeof Swal !== 'undefined') {
    Swal.fire({
      icon: 'question',
      title: '¿Seguro desea cerrar sesión?',
      showCancelButton: true,
      confirmButtonText: 'Cerrar sesión',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#7b2ff7',
      cancelButtonColor: '#6c757d'
    }).then(r => {
      if (r.isConfirmed) {
        try { mostrarToast('Cerrando sesión...'); } catch (_) {}
        setTimeout(ejecutar, 300);
      }
    });
  } else {
    if (confirm('¿Seguro desea cerrar sesión?')) ejecutar();
  }
};

document.addEventListener('DOMContentLoaded', () => {
  const logoutBtn = document.getElementById('logoutMesero');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      e.preventDefault();
      window.meseroApp.logout();
    });
  }
});