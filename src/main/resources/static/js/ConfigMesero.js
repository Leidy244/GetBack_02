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


// JavaScript para el modo oscuro - DEBE ir en tu HTML
document.addEventListener('DOMContentLoaded', function() {
    const darkModeToggle = document.getElementById('darkModeToggle');
    const htmlElement = document.documentElement;
    
    // Verificar preferencia guardada
    const savedTheme = localStorage.getItem('theme') || 'light';
    htmlElement.setAttribute('data-theme', savedTheme);
    updateToggleIcon(savedTheme);
    
    // Evento para cambiar modo
    darkModeToggle.addEventListener('click', function() {
        const currentTheme = htmlElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';
        
        htmlElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateToggleIcon(newTheme);
        
        // Efecto de animación
        this.style.transform = 'scale(0.9)';
        setTimeout(() => {
            this.style.transform = 'scale(1)';
        }, 150);
    });
    
    function updateToggleIcon(theme) {
        const icon = darkModeToggle.querySelector('i');
        if (theme === 'dark') {
            icon.className = 'fas fa-sun';
            darkModeToggle.setAttribute('aria-label', 'Cambiar a modo claro');
        } else {
            icon.className = 'fas fa-moon';
            darkModeToggle.setAttribute('aria-label', 'Cambiar a modo oscuro');
        }
    }
});
