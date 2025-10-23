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
