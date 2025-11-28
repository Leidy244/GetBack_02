// admin.js - Versión mejorada con modal único para productos
const adminApp = {
	currentProduct: null,
	productos: [],
	categorias: [],

	init() {
		this.setupEventListeners();
		this.setupImagePreviews();
		this.setupProductModal();
		this.setupSearchFunctionality();
		this.hidePreloader();
		this.restoreTheme();
		this.setupNavigation();
		this.setupAdminAppearanceSettings();
		this.setupFlashMessages();
		this.setupPerfilSection();
		console.log("GETBACK Admin Panel inicializado ✅");
	},

	/* -------------------------------
	   APARIENCIA DEL PANEL ADMIN (TEMA / FUENTE / CONTRASTE)
	--------------------------------*/
	applyAdminFontSize(size) {
		const sizes = {
			'pequeno': '14px',
			'mediano': '16px',
			'grande': '18px'
		};

		if (sizes[size]) {
			// Aplicar al root para que rem escale todo el panel
			document.documentElement.style.fontSize = sizes[size];
			// Compatibilidad con estilos heredados desde body
			document.body.style.fontSize = sizes[size];
		}
	},

	applyAdminTheme(theme) {
		const body = document.body;
		if (!body) return;

		// "default" limpia el atributo para usar los valores por defecto de :root
		if (!theme || theme === 'default') {
			body.removeAttribute('data-admin-theme');
			return;
		}

		body.setAttribute('data-admin-theme', theme);
	},

	applyAdminContrast(contrast) {
		const body = document.body;
		if (!body) return;

		// valores esperados: '0', '1', '2'
		if (contrast === undefined || contrast === null || contrast === '') {
			body.removeAttribute('data-admin-contrast');
			return;
		}

		body.setAttribute('data-admin-contrast', String(contrast));
	},

	// Interacciones de la sección de perfil (editar datos, cambiar foto, modal de configuración)
	setupPerfilSection() {
		// Botones y cajas de edición / foto
		const inputFoto        = document.getElementById("inputFoto");
		const nombreArchivo    = document.getElementById("nombreArchivo");
		const preview          = document.getElementById("previewFoto");
		const previewIniciales = document.getElementById("previewIniciales");
		const btnShowEdit      = document.getElementById("btnShowEdit");
		const btnHideEdit      = document.getElementById("btnHideEdit");
		const btnShowPhoto     = document.getElementById("btnShowPhoto");
		const btnHidePhoto     = document.getElementById("btnHidePhoto");
		const boxEdit          = document.getElementById("editar-perfil");
		const boxPhoto         = document.getElementById("cambiar-foto");

		// Si no estamos en la vista de perfil, no hacer nada
		if (!document.querySelector('.content-section #btnShowEdit')) {
			return;
		}

		// Editar perfil
		if (btnShowEdit && boxEdit && boxPhoto) {
			btnShowEdit.addEventListener('click', () => {
				const willShow = boxEdit.style.display === 'none' || boxEdit.style.display === '';
				boxPhoto.style.display = 'none';
				boxEdit.style.display = willShow ? 'block' : 'none';
			});
		}
		if (btnHideEdit && boxEdit) {
			btnHideEdit.addEventListener('click', () => {
				boxEdit.style.display = 'none';
			});
		}

		// Cambiar foto
		if (btnShowPhoto && boxPhoto && boxEdit) {
			btnShowPhoto.addEventListener('click', () => {
				const willShow = boxPhoto.style.display === 'none' || boxPhoto.style.display === '';
				boxEdit.style.display = 'none';
				boxPhoto.style.display = willShow ? 'block' : 'none';
			});
		}
		if (btnHidePhoto && boxPhoto) {
			btnHidePhoto.addEventListener('click', () => {
				boxPhoto.style.display = 'none';
			});
		}

		// Preview de foto
		if (inputFoto && nombreArchivo) {
			inputFoto.addEventListener("change", () => {
				const archivo = inputFoto.files[0];
				nombreArchivo.textContent = archivo ? archivo.name : "Ningún archivo seleccionado";

				if (archivo) {
					const url = URL.createObjectURL(archivo);
					if (preview) {
						preview.src = url;
						preview.style.display = 'block';
					}
					if (previewIniciales) {
						previewIniciales.style.display = 'none';
					}
					const headerAvatar = document.querySelector('.admin-header .user-avatar .avatar-img');
					if (headerAvatar) {
						headerAvatar.src = url;
					}
				}
			});
		}

		// Configuración: Modal de ajustes
		const adminSettingsBtn = document.getElementById('adminSettingsBtn');
		const adminSettingsModal = document.getElementById('adminsettingsModal');
		const closeSettingsBtn = document.getElementById('closeSettings');
		const saveSettingsBtn = document.getElementById('saveSettingsBtn');
		const fontSizeSelect = document.getElementById('fontSizeSelect');
		const themeSelect = document.getElementById('themeSelect');
		const contrastRange = document.getElementById('contrastRange');

		// Abrir modal al hacer clic en el engranaje
		if (adminSettingsBtn && adminSettingsModal) {
		    adminSettingsBtn.addEventListener('click', () => {
		        adminSettingsModal.classList.add('show');
		    });
		}

		// Cerrar modal al hacer clic en la X
		if (closeSettingsBtn && adminSettingsModal) {
		    closeSettingsBtn.addEventListener('click', () => {
		        adminSettingsModal.classList.remove('show');
		    });
		}

		// Cerrar modal al hacer clic fuera del contenido
		if (adminSettingsModal) {
		    adminSettingsModal.addEventListener('click', (e) => {
		        if (e.target === adminSettingsModal) {
		            adminSettingsModal.classList.remove('show');
		        }
		    });
		}

		// Guardar configuración y cerrar modal
		if (saveSettingsBtn) {
		    saveSettingsBtn.addEventListener('click', () => {
		        const newSettings = {
		            fontSize: fontSizeSelect?.value || 'mediano',
		            theme: themeSelect?.value || 'default',
		            contrast: contrastRange?.value ?? '1'
		        };

		        // Aplicar cambios
		        this.applyAdminFontSize(newSettings.fontSize);
		        this.applyAdminTheme(newSettings.theme);
		        this.applyAdminContrast(newSettings.contrast);

		        // Guardar en localStorage
		        localStorage.setItem('adminAppearanceSettings', JSON.stringify(newSettings));
		        
		        // CERRAR EL MODAL
		        adminSettingsModal.classList.remove('show');
		        
		        // Mostrar confirmación
		        this.showNotification('Configuración guardada correctamente', 'success');
		    });
		}
	},

	// Mensajes flash (success / error) usando SweetAlert2
	setupFlashMessages() {
		if (typeof Swal === 'undefined') return;

		const successEl = document.getElementById('flash-success');
		const errorEl = document.getElementById('flash-error');

		if (successEl) {
			const msg = successEl.getAttribute('data-message') || '';
			// Eliminar alerts Bootstrap duplicados si existen
			document.querySelectorAll('.alert.alert-success').forEach(a => a.remove());
			Swal.fire({
				icon: 'success',
				title: 'Guardado con éxito',
				text: msg,
				showConfirmButton: false,
				timer: 1800
			});
		}

		if (errorEl) {
			const msg = errorEl.getAttribute('data-message') || '';
			// Eliminar alerts Bootstrap duplicados si existen
			document.querySelectorAll('.alert.alert-danger').forEach(a => a.remove());
			Swal.fire({
				icon: 'error',
				title: 'Ocurrió un error',
				text: msg,
				confirmButtonText: 'Entendido'
			});
		}
	},

	setupAdminAppearanceSettings() {
		// Cargar ajustes guardados al iniciar
		let saved = {};
		try {
			saved = JSON.parse(localStorage.getItem('adminAppearanceSettings')) || {};
		} catch (e) {
			console.warn('No se pudieron cargar las preferencias de apariencia del admin', e);
		}

		// Aplicar inmediatamente lo guardado (si existe)
		if (saved.fontSize) {
			this.applyAdminFontSize(saved.fontSize);
		}
		if (saved.theme) {
			this.applyAdminTheme(saved.theme);
		}
		if (saved.contrast !== undefined) {
			this.applyAdminContrast(saved.contrast);
		}

		// Enlazar controles del modal si existen en la vista actual
		const fontSizeSelect = document.getElementById('fontSizeSelect');
		const themeSelect = document.getElementById('themeSelect');
		const contrastRange = document.getElementById('contrastRange');
		const saveSettingsBtn = document.getElementById('saveSettingsBtn');

		if (!saveSettingsBtn) {
			return; // Estamos en otra sección sin el modal de perfil
		}

		// Sincronizar valores actuales de los controles con lo guardado
		try {
			const stored = JSON.parse(localStorage.getItem('adminAppearanceSettings')) || {};
			if (fontSizeSelect && stored.fontSize) {
				fontSizeSelect.value = stored.fontSize;
			}
			if (themeSelect && stored.theme) {
				themeSelect.value = stored.theme;
			}
			if (contrastRange && stored.contrast !== undefined) {
				contrastRange.value = stored.contrast;
			}
		} catch (_) { }
	},

	/* -------------------------------
	   MODAL ÚNICO PARA PRODUCTOS
	--------------------------------*/
	setupProductModal() {
		const productModal = document.getElementById('productModal');
		if (!productModal) return;

		// Configurar evento cuando se abre el modal
		productModal.addEventListener('show.bs.modal', (e) => {
			const button = e.relatedTarget;
			const mode = button.getAttribute('data-mode');
			const productId = button.getAttribute('data-product-id');

			this.setupModalMode(mode, productId);
		});

		// Configurar evento cuando se cierra el modal
		productModal.addEventListener('hidden.bs.modal', () => {
			this.resetProductModal();
		});

		// Configurar envío del formulario
		const productForm = document.getElementById('productForm');
		if (productForm) {
			productForm.addEventListener('submit', (e) => this.handleProductSubmit(e));
		}
	},

	setupModalMode(mode, productId) {
		const title = document.getElementById('productModalTitle');
		const submitBtn = document.getElementById('productSubmitBtn');
		const imageInput = document.getElementById('productImage');
		const imageHelp = document.getElementById('imageHelp');
		const imageLabel = document.getElementById('imageLabel');

		if (mode === 'edit' && productId) {
			// Modo edición
			title.textContent = 'Editar Producto';
			submitBtn.textContent = 'Guardar Cambios';
			imageInput.required = false;
			imageHelp.textContent = 'Dejar vacío para mantener la imagen actual';
			imageLabel.textContent = 'Cambiar Imagen (opcional)';

			this.loadProductData(productId);
		} else {
			// Modo nuevo
			title.textContent = 'Nuevo Producto';
			submitBtn.textContent = 'Guardar Producto';
			imageInput.required = true;
			imageHelp.textContent = 'Formatos permitidos: JPG, PNG, GIF, WebP (Máx. 5MB)';
			imageLabel.textContent = 'Imagen';

			this.currentProduct = null;
		}
	},

	async loadProductData(productId) {
		try {
			// En una implementación real, aquí harías una petición al servidor
			// Por ahora simulamos la carga de datos
			const product = this.findProductById(productId);
			if (product) {
				this.currentProduct = product;
				this.populateProductForm(product);
			}
		} catch (error) {
			console.error('Error loading product data:', error);
			this.showNotification('Error al cargar los datos del producto', 'error');
		}
	},

	findProductById(id) {
		// Simulación - en una implementación real esto vendría del servidor
		return this.productos.find(p => p.id == id) || null;
	},

	populateProductForm(product) {
		document.getElementById('productId').value = product.id || '';
		document.getElementById('productName').value = product.nombre || '';
		document.getElementById('productDescription').value = product.descripcion || '';
		document.getElementById('productCategory').value = product.categoria?.id || '';

		// Manejar imagen actual
		const currentImage = document.getElementById('currentProductImage');
		const noImageMessage = document.getElementById('noImageMessage');

		if (product.imagen) {
			currentImage.src = `/images/${product.imagen}`;
			currentImage.style.display = 'block';
			noImageMessage.style.display = 'none';
		} else {
			currentImage.style.display = 'none';
			noImageMessage.style.display = 'block';
		}
	},

	resetProductModal() {
		const form = document.getElementById('productForm');
		if (form) {
			form.reset();
			form.action = '/admin/productos/guardar'; // Reset to default action

			// Reset imagen actual
			document.getElementById('currentProductImage').style.display = 'none';
			document.getElementById('noImageMessage').style.display = 'block';

			// Reset preview
			document.getElementById('productImagePreview').innerHTML =
				'<span class="text-muted">La imagen aparecerá aquí</span>';

			// Limpiar validaciones
			this.clearFormValidations(form);
		}
		this.currentProduct = null;
	},

	async handleProductSubmit(e) {
		e.preventDefault();
		const form = e.target;

		if (!this.validateProductForm(form)) {
			return;
		}

		// Determinar la acción (guardar o editar)
		const productId = document.getElementById('productId').value;
		if (productId) {
			form.action = `/admin/productos/editar/${productId}`;
		} else {
			form.action = '/admin/productos/guardar';
		}

		this.showLoadingState(form, true);

		try {
			// En una implementación real, aquí se enviaría el formulario
			// Por ahora simulamos el envío
			await this.simulateFormSubmit(form);
			this.showNotification(
				productId ? 'Producto actualizado correctamente' : 'Producto creado correctamente',
				'success'
			);

			// Cerrar modal después de éxito
			const modal = bootstrap.Modal.getInstance(document.getElementById('productModal'));
			modal.hide();

			// Recargar la página o actualizar la tabla
			setTimeout(() => {
				window.location.reload();
			}, 1000);

		} catch (error) {
			this.showNotification('Error al guardar el producto', 'error');
		} finally {
			this.showLoadingState(form, false);
		}
	},

	simulateFormSubmit(form) {
		return new Promise((resolve) => {
			setTimeout(() => {
				console.log('Formulario enviado:', new FormData(form));
				resolve();
			}, 1500);
		});
	},

	/* -------------------------------
	   MANEJO DE IMÁGENES MEJORADO
	--------------------------------*/
	setupImagePreviews() {
		const imageInput = document.getElementById('productImage');
		if (imageInput) {
			imageInput.addEventListener('change', (e) => this.handleImagePreview(e));
		}
	},

	handleImagePreview(e) {
		const input = e.target;
		const preview = document.getElementById('productImagePreview');

		if (!preview) return;

		if (input.files && input.files[0]) {
			// Validar tamaño máximo (5MB)
			if (input.files[0].size > 5 * 1024 * 1024) {
				this.showNotification('La imagen no debe superar los 5MB', 'error');
				input.value = '';
				preview.innerHTML = '<span class="text-danger">Imagen demasiado grande</span>';
				return;
			}

			// Validar tipo de archivo
			const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
			if (!validTypes.includes(input.files[0].type)) {
				this.showNotification('Solo se permiten imágenes JPG, PNG, GIF o WebP', 'error');
				input.value = '';
				preview.innerHTML = '<span class="text-danger">Formato no válido</span>';
				return;
			}

			const reader = new FileReader();
			reader.onload = (e) => {
				preview.innerHTML = `
                    <div class="preview-container">
                        <img src="${e.target.result}" class="img-preview-large">
                        <div class="preview-info">
                            <small>${input.files[0].name}</small>
                            <small>${(input.files[0].size / 1024).toFixed(1)} KB</small>
                        </div>
                    </div>
                `;
			};
			reader.readAsDataURL(input.files[0]);
		} else {
			preview.innerHTML = '<span class="text-muted">La imagen aparecerá aquí</span>';
		}
	},

	/* -------------------------------
	   VALIDACIÓN DE FORMULARIOS
	--------------------------------*/
	validateProductForm(form) {
		let isValid = true;

		// Validar nombre
		const nombreInput = form.querySelector('input[name="nombre"]');
		if (!nombreInput.value.trim()) {
			this.markInvalid(nombreInput, 'El nombre es requerido');
			isValid = false;
		} else {
			this.markValid(nombreInput);
		}

		// Validar categoría
		const categoriaSelect = form.querySelector('select[name="categoria.id"]');
		if (!categoriaSelect.value) {
			this.markInvalid(categoriaSelect, 'Seleccione una categoría');
			isValid = false;
		} else {
			this.markValid(categoriaSelect);
		}

		// Validar archivo de imagen solo si es nuevo producto
		const fileInput = form.querySelector('input[type="file"]');
		const productId = document.getElementById('productId').value;

		if (!productId && fileInput && fileInput.files.length === 0) {
			this.markInvalid(fileInput, 'La imagen es requerida para nuevos productos');
			isValid = false;
		} else if (fileInput && fileInput.files.length > 0) {
			const file = fileInput.files[0];
			if (file.size > 5 * 1024 * 1024) {
				this.markInvalid(fileInput, 'La imagen no debe superar 5MB');
				isValid = false;
			} else {
				this.markValid(fileInput);
			}
		}

		return isValid;
	},

	markInvalid(element, message) {
		element.classList.add('is-invalid');
		element.classList.remove('is-valid');

		// Remover feedback anterior
		const existingFeedback = element.nextElementSibling;
		if (existingFeedback && existingFeedback.classList.contains('invalid-feedback')) {
			existingFeedback.remove();
		}

		// Agregar nuevo feedback
		const feedback = document.createElement('div');
		feedback.className = 'invalid-feedback';
		feedback.textContent = message;
		element.parentNode.appendChild(feedback);
	},

	markValid(element) {
		element.classList.remove('is-invalid');
		element.classList.add('is-valid');

		// Remover feedback
		const feedback = element.nextElementSibling;
		if (feedback && feedback.classList.contains('invalid-feedback')) {
			feedback.remove();
		}
	},

	clearFormValidations(form) {
		const inputs = form.querySelectorAll('.is-invalid, .is-valid');
		inputs.forEach(input => {
			input.classList.remove('is-invalid', 'is-valid');
		});

		const feedbacks = form.querySelectorAll('.invalid-feedback');
		feedbacks.forEach(feedback => feedback.remove());
	},

	showLoadingState(form, isLoading) {
		const submitBtn = form.querySelector('button[type="submit"]');
		if (submitBtn) {
			if (isLoading) {
				submitBtn.disabled = true;
				submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';
			} else {
				submitBtn.disabled = false;
				submitBtn.innerHTML = submitBtn.getAttribute('data-original-text') || 'Guardar';
			}
		}
	},

	/* -------------------------------
	   FUNCIONALIDADES GENERALES
	--------------------------------*/
	setupNavigation() {
		this.highlightActiveNavLink();

		document.querySelectorAll('.nav-link').forEach(link => {
			link.addEventListener('click', (e) => {
				this.handleNavigation(e);
			});
		});
	},

	highlightActiveNavLink() {
		document.querySelectorAll('.nav-link').forEach(link => {
			link.classList.remove('active');
		});

		const urlParams = new URLSearchParams(window.location.search);
		const activeSection = urlParams.get('activeSection') || 'dashboard';

		const activeLink = document.querySelector(`a[href*="activeSection=${activeSection}"]`);
		if (activeLink) {
			activeLink.classList.add('active');
		} else if (activeSection === 'dashboard') {
			const dashboardLink = document.querySelector('a[href="/admin"]');
			if (dashboardLink) {
				dashboardLink.classList.add('active');
			}
		}
	},

	handleNavigation(e) {
		const link = e.currentTarget;
		const href = link.getAttribute('href');

		if (href && href.includes('activeSection')) {
			e.preventDefault();
			window.location.href = href;
		}
	},

	setupSearchFunctionality() {
		const searchInput = document.getElementById('searchProducts');
		if (searchInput) {
			searchInput.addEventListener('input', (e) => {
				this.filterTable(e.target.value, 'products');
			});
		}
	},

	filterTable(searchTerm, tableType) {
		const table = document.querySelector('.table');
		if (!table) return;

		const rows = table.querySelectorAll('tbody tr');
		const searchLower = searchTerm.toLowerCase();

		rows.forEach(row => {
			const text = row.textContent.toLowerCase();
			row.style.display = text.includes(searchLower) ? '' : 'none';
		});
	},

	setupEventListeners() {
		const darkModeToggle = document.getElementById("darkModeToggle");
		if (darkModeToggle) {
			darkModeToggle.addEventListener("click", () => this.toggleDarkMode());
		}

		const logoutBtn = document.querySelector('.logout-btn');
		if (logoutBtn) {
			logoutBtn.addEventListener('click', () => this.logout());
		}

		// Toggle sidebar en pantallas pequeñas
		const sidebarToggle = document.getElementById('sidebarToggle');
		const sidebar = document.querySelector('.sidebar');
		if (sidebarToggle && sidebar) {
			sidebarToggle.addEventListener('click', () => {
				sidebar.classList.toggle('active');
			});
		}

		// Configurar todos los formularios
		document.querySelectorAll('form').forEach(form => {
			if (form.id !== 'productForm') {
				form.addEventListener('submit', (e) => {
					this.handleFormSubmit(e);
				});
			}
		});
	},

	handleFormSubmit(e) {
		const form = e.target;
		const submitBtn = form.querySelector('button[type="submit"]');

		if (submitBtn) {
			submitBtn.disabled = true;
			const originalText = submitBtn.innerHTML;
			submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';

            setTimeout(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            }, 1500);
		}
	},

	showNotification(message, type = "info") {
		document.querySelectorAll('.admin-notification').forEach(n => n.remove());

		const notification = document.createElement("div");
		notification.className = `admin-notification notification-${type}`;
		notification.innerHTML = `
            <div class="notification-content">
                <i class="fas fa-${this.getNotificationIcon(type)}"></i>
                <span>${message}</span>
                <button onclick="this.parentElement.parentElement.remove()">&times;</button>
            </div>
        `;

		document.body.appendChild(notification);

		setTimeout(() => {
			if (notification.parentElement) {
				notification.remove();
			}
		}, 5000);
	},

	getNotificationIcon(type) {
		const icons = {
			success: 'check-circle',
			error: 'exclamation-circle',
			warning: 'exclamation-triangle',
			info: 'info-circle'
		};
		return icons[type] || 'info-circle';
	},

	toggleDarkMode() {
		const body = document.body;
		const currentTheme = body.getAttribute("data-theme");
		const icon = document.querySelector("#darkModeToggle i");

		if (currentTheme === "dark") {
			body.removeAttribute("data-theme");
			if (icon) icon.className = "fas fa-moon";
			localStorage.setItem("theme", "light");
			this.showNotification("Modo claro activado", "success");
		} else {
			body.setAttribute("data-theme", "dark");
			if (icon) icon.className = "fas fa-sun";
			localStorage.setItem("theme", "dark");
			this.showNotification("Modo oscuro activado", "success");
		}
	},

	restoreTheme() {
		const savedTheme = localStorage.getItem("theme");
		if (savedTheme === "dark") {
			document.body.setAttribute("data-theme", "dark");
			const icon = document.querySelector("#darkModeToggle i");
			if (icon) icon.className = "fas fa-sun";
		}
	},

	logout() {
		if (confirm("¿Está seguro de que desea cerrar sesión?")) {
			this.showNotification("Cerrando sesión...", "info");
			setTimeout(() => {
				window.location.href = "/login";
			}, 1500);
		}
	},

	hidePreloader() {
		const preloader = document.getElementById("preloader");
		if (preloader) {
			setTimeout(() => {
				preloader.style.opacity = "0";
				setTimeout(() => {
					if (preloader.parentElement) {
						preloader.remove();
					}
				}, 300);
			}, 1000);
		}
	}
};

// Inicializar solo en el panel de administración (body.admin-panel)
document.addEventListener("DOMContentLoaded", () => {
	const body = document.body;
	if (body && body.classList.contains("admin-panel")) {
		adminApp.init();
	}
});
window.adminApp = adminApp;

// ===== Funcionalidades específicas de usuarios =====
document.addEventListener('DOMContentLoaded', function() {
	// Rellenar modal editar
	const editarModal = document.getElementById('editarUsuarioModal');
	if (editarModal) {
		editarModal.addEventListener('show.bs.modal', function(event) {
			// El botón que disparó el modal
			const button = event.relatedTarget;
			if (!button) return;

			// Extraer atributos data-*
			const id = button.getAttribute('data-user-id') || '';
			const nombre = button.getAttribute('data-user-nombre') || '';
			const apellido = button.getAttribute('data-user-apellido') || '';
			const correo = button.getAttribute('data-user-correo') || '';
			const telefono = button.getAttribute('data-user-telefono') || '';
			const direccion = button.getAttribute('data-user-direccion') || '';
			const estado = button.getAttribute('data-user-estado') || 'ACTIVO';
			const rolId = button.getAttribute('data-user-rol') || '';

			// Setear inputs dentro del modal
			document.getElementById('edit-user-id').value = id;
			document.getElementById('edit-user-nombre').value = nombre;
			document.getElementById('edit-user-apellido').value = apellido;
			document.getElementById('edit-user-correo').value = correo;
			document.getElementById('edit-user-telefono').value = telefono;
			document.getElementById('edit-user-direccion').value = direccion;
			document.getElementById('edit-user-estado').value = estado;

			// Seleccionar rol en el select (si existe)
			const rolSelect = document.getElementById('edit-user-rol');
			if (rolSelect) {
				rolSelect.value = rolId;
			}
		});
	}

	// Rellenar modal eliminar
	const deleteModal = document.getElementById('confirmDeleteUserModal');
	if (deleteModal) {
		deleteModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button) return;

			const id = button.getAttribute('data-user-id') || '';
			const name = button.getAttribute('data-user-nombre') || 'Usuario';

			// Mostrar nombre en modal
			const nameSpan = document.getElementById('delete-user-name');
			if (nameSpan) nameSpan.textContent = name;

			// Ajustar link de confirmación
			const confirmLink = document.getElementById('confirm-delete-link');
			if (confirmLink) {
				confirmLink.setAttribute('href', '/users/delete/' + id);
			}
		});
	}
});

// ===== Dashboard setup (date, filters, carousel, charts) =====
document.addEventListener('DOMContentLoaded', function() {
  const dashboardRoot = document.querySelector('.dashboard-container');
  if (!dashboardRoot) return;

  // Date badge with seconds
  try {
    const dateEl = document.getElementById('dashboardDate');
    const renderHeaderDate = () => {
      if (!dateEl) return;
      const now = new Date();
      const dateStr = now.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' });
      const timeStr = now.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', second: '2-digit' });
      dateEl.textContent = `${dateStr} ${timeStr}`;
    };
    renderHeaderDate();
    setInterval(renderHeaderDate, 1000);
  } catch (_) {}

  // Tabs (filters)
  const filterBtns = document.querySelectorAll('.chart-filter-btn');
  const chartContainers = document.querySelectorAll('.charts-container');
  function activateFilter(filter){
    const btn = Array.from(filterBtns).find(b => b.getAttribute('data-filter') === filter);
    if (!btn) return;
    filterBtns.forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    chartContainers.forEach(c => c.classList.remove('active'));
    const target = document.getElementById('charts-' + filter);
    if (target) target.classList.add('active');
    const grid = document.querySelector('.charts-container.active .charts-grid');
    if (grid) grid.scrollTo({ left: 0, behavior: 'auto' });
  }
  filterBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      const filter = this.getAttribute('data-filter');
      activateFilter(filter);
    });
  });

  // Carousel controls
  const prevBtn = document.getElementById('carouselPrev');
  const nextBtn = document.getElementById('carouselNext');
  function cycleFilter(dir){
    const arr = Array.from(filterBtns);
    const idx = arr.findIndex(b => b.classList.contains('active'));
    const nextIdx = (idx < 0) ? 0 : (idx + dir + arr.length) % arr.length;
    const nextFilter = arr[nextIdx].getAttribute('data-filter');
    activateFilter(nextFilter);
  }
  if (prevBtn) prevBtn.addEventListener('click', () => cycleFilter(-1));
  if (nextBtn) nextBtn.addEventListener('click', () => cycleFilter(1));

  // Charts (Chart.js must be loaded on the page)
  if (typeof Chart === 'undefined') return;

  const sanitizeLabels = (labels) => (labels || []).map((l, i) => (l && l.toString().trim().length > 0) ? l : `Ítem ${i+1}`);
  const safeNumber = (n) => Number.isFinite(n) ? n : 0;

  fetch('/api/dashboard/stats')
    .then(r => r.json())
    .then(data => {
      // Productos por categoría
      const pcRaw = data.productosPorCategoria || {labels: [], data: []};
      const pc = { labels: sanitizeLabels(pcRaw.labels), data: (pcRaw.data || []).map(safeNumber) };
      const catCfg = {
        type: 'pie',
        data: { labels: pc.labels, datasets: [{ label: 'Productos', data: pc.data, backgroundColor: ['#3498db','#9b59b6','#1abc9c','#f39c12','#e74c3c','#2ecc71'] }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Productos por categoría' }, tooltip: { callbacks: { label: (ctx) => { const arr = Array.isArray(ctx.dataset.data) ? ctx.dataset.data : []; const total = arr.reduce((a,b)=>a+safeNumber(b),0); const val = safeNumber(ctx.parsed); const pct = total ? ((val/total)*100).toFixed(1) : 0; const label = (ctx.label && ctx.label.toString().length) ? ctx.label : 'Sin etiqueta'; return `${label}: ${val} (${pct}%)`; } } } }, layout: { padding: 0 }, animation: { duration: 0 } }
      };
      if (document.getElementById('chartCategorias')) new Chart(document.getElementById('chartCategorias'), catCfg);
      if (document.getElementById('chartCategorias2')) new Chart(document.getElementById('chartCategorias2'), {...catCfg, type:'bar'});
      if (document.getElementById('chartCategorias3')) new Chart(document.getElementById('chartCategorias3'), catCfg);

      // Eventos por mes
      const meses = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
      const eventos = (data.eventosPorMes && data.eventosPorMes.length === 12) ? data.eventosPorMes.map(safeNumber) : [2,3,5,4,6,8,7,6,5,4,3,2];
      const evCfg = {
        type: 'bar',
        data: { labels: meses, datasets: [{ label: 'Eventos', data: eventos, backgroundColor: '#3498db', barThickness: 6, maxBarThickness: 8 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Eventos por mes' }, tooltip: { callbacks: { label: (ctx) => `Eventos: ${ctx.parsed.y}` } } }, scales: { x: { ticks: { font: { size: 10 } }, title: { display: true, text: 'Mes' } }, y: { ticks: { font: { size: 10 } }, title: { display: true, text: 'Cantidad' }, beginAtZero: true } }, layout: { padding: 0 }, animation: { duration: 0 } }
      };
      if (document.getElementById('chartEventos')) new Chart(document.getElementById('chartEventos'), evCfg);
      if (document.getElementById('chartEventos2')) new Chart(document.getElementById('chartEventos2'), evCfg);

      // Usuarios por rol
      const urRaw = data.usuariosPorRol || {labels: [], data: []};
      const ur = { labels: sanitizeLabels(urRaw.labels), data: (urRaw.data || []).map(safeNumber) };
      const urCfg = { type: 'pie', data: { labels: ur.labels, datasets: [{ label: 'Usuarios', data: ur.data, backgroundColor: ['#2ecc71','#9b59b6','#e74c3c'] }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Usuarios por rol' }, tooltip: { callbacks: { label: (ctx) => { const arr = Array.isArray(ctx.dataset.data) ? ctx.dataset.data : []; const total = arr.reduce((a,b)=>a+safeNumber(b),0); const val = safeNumber(ctx.parsed); const pct = total ? ((val/total)*100).toFixed(1) : 0; const label = (ctx.label && ctx.label.toString().length) ? ctx.label : 'Sin etiqueta'; return `${label}: ${val} (${pct}%)`; } } } }, layout: { padding: 0 }, animation: { duration: 0 } } };
      if (document.getElementById('chartUsuarios')) new Chart(document.getElementById('chartUsuarios'), urCfg);
      if (document.getElementById('chartUsuarios2')) new Chart(document.getElementById('chartUsuarios2'), urCfg);

      // Estado de mesas
      const emRaw = data.estadoMesas || {labels: [], data: []};
      const em = { labels: sanitizeLabels(emRaw.labels), data: (emRaw.data || []).map(safeNumber) };
      const emCfg = { type: 'doughnut', data: { labels: em.labels, datasets: [{ label: 'Mesas', data: em.data, backgroundColor: ['#2ecc71','#e74c3c'] }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Estado de mesas' }, tooltip: { callbacks: { label: (ctx) => { const label = (ctx.label && ctx.label.toString().length) ? ctx.label : 'Sin etiqueta'; const val = safeNumber(ctx.parsed); const arr = Array.isArray(ctx.dataset.data) ? ctx.dataset.data : []; const total = arr.reduce((a,b)=>a+safeNumber(b),0); const pct = total ? ((val/total)*100).toFixed(1) : 0; return `${label}: ${val} (${pct}%)`; } } } }, layout: { padding: 0 }, animation: { duration: 0 } } };
      if (document.getElementById('chartMesas')) new Chart(document.getElementById('chartMesas'), emCfg);
      if (document.getElementById('chartMesas2')) new Chart(document.getElementById('chartMesas2'), emCfg);

      // Resumen general del panel
      const resumenLabels = ['Total Ventas','Pedidos Pendientes','Productos','Categorías','Eventos','Usuarios','Mesas'];
      const resumenData = [data.totalVentas, data.pedidosPendientes, data.totalProductos, data.totalCategorias, data.totalEventos, data.totalUsuarios, data.totalMesas].map(safeNumber);
      const resumenCfg = {
        type: 'bar',
        data: { labels: resumenLabels, datasets: [{ label: 'Conteos', data: resumenData, backgroundColor: '#7b2ff7' }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Resumen general del panel' }, tooltip: { callbacks: { label: (ctx) => `${ctx.label}: ${ctx.parsed.y}` } } }, scales: { x: { ticks: { font: { size: 10 } } }, y: { beginAtZero: true } }, layout: { padding: 0 }, animation: { duration: 0 } }
      };
      if (document.getElementById('chartResumenAdmin')) new Chart(document.getElementById('chartResumenAdmin'), resumenCfg);

      // Ingresos
      const ingresosLabels = ['Ingresos hoy','Ingresos totales'];
      const ingresosData = [safeNumber(data.ingresosHoy), safeNumber(data.ingresosTotales)];
      const ingresosCfg = {
        type: 'bar',
        data: { labels: ingresosLabels, datasets: [{ label: 'Monto (COP)', data: ingresosData, backgroundColor: ['#00c6ff','#00e6b4'] }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: true, position: 'top' }, title: { display: true, text: 'Ingresos' }, tooltip: { callbacks: { label: (ctx) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(ctx.parsed.y) } } }, scales: { y: { beginAtZero: true } }, layout: { padding: 0 }, animation: { duration: 0 } }
      };
      if (document.getElementById('chartIngresosAdmin')) new Chart(document.getElementById('chartIngresosAdmin'), ingresosCfg);
    })
    .catch(err => console.error('Error cargando stats del dashboard:', err));

  // Recent activity
  const list = document.getElementById('recentActivity');
  const btn = document.getElementById('activityRefreshBtn');
  const prev = document.getElementById('activityPrev');
  const next = document.getElementById('activityNext');
  const pageLabel = document.getElementById('activityPageLabel');
  let activityPage = 0;
  const activitySize = 6;
  function fmtDate(iso){ try{ const d = new Date(iso); return d.toLocaleString(); } catch(_){ return iso || ''; } }
  function typeIcon(type){
    const t = (type||'').toUpperCase();
    if (t==='PRODUCT') return 'fa-box';
    if (t==='CATEGORY') return 'fa-tags';
    if (t==='USER') return 'fa-user';
    if (t==='PROFILE') return 'fa-id-card';
    if (t==='EVENT') return 'fa-calendar-alt';
    if (t==='TABLE') return 'fa-chair';
    return 'fa-bolt';
  }
  function renderActivity(items){
    if (!list) return;
    if (!items || items.length === 0){
      list.innerHTML = '<div class="empty-state"><i class="fas fa-inbox"></i><h4>Sin actividad</h4><p>Aquí verás los últimos movimientos del sistema.</p></div>';
      return;
    }
    const showUser = (u) => {
      if (!u) return '';
      const name = String(u).trim();
      if (!name || name.toLowerCase() === 'anonymoususer' || name.toLowerCase() === 'system') return '';
      return `<span><i class="fas fa-user"></i> ${name}</span>`;
    };
    list.innerHTML = items.map(a => `
      <div class="activity-item ${a.type ? 'activity-'+a.type.toLowerCase() : ''}">
        <div class="activity-icon"><i class="fas ${typeIcon(a.type)}"></i></div>
        <div class="activity-content">
          <div class="activity-title"><strong>${a.type || 'Evento'}</strong> — ${a.message || ''}</div>
          <div class="activity-meta">
            <span class="activity-time"><i class="far fa-clock"></i> ${fmtDate(a.timestamp)}</span>
            ${showUser(a.username)}
          </div>
        </div>
      </div>
    `).join('');
  }
  function updatePageLabel(){ if (pageLabel) pageLabel.textContent = `Página ${activityPage+1}`; }
  function loadActivity(){
    if (!list) return;
    fetch(`/api/admin/activity?page=${activityPage}&size=${activitySize}`).then(r=>r.json()).then(renderActivity).catch(()=>{});
    updatePageLabel();
  }
  loadActivity();
  if (btn) btn.addEventListener('click', (e)=>{ e.preventDefault(); loadActivity(); });
  if (prev) prev.addEventListener('click', (e)=>{ e.preventDefault(); activityPage = Math.max(0, activityPage-1); loadActivity(); });
  if (next) next.addEventListener('click', (e)=>{ e.preventDefault(); activityPage = activityPage+1; loadActivity(); });
  const modalOpen = document.getElementById('activityModalOpen');
  const modalList = document.getElementById('activityModalList');
  const modalInfo = document.getElementById('activityModalInfo');
  const modalForm = document.getElementById('activityModalForm');
  const modalReset = document.getElementById('activityModalReset');
  const typeSelect = document.getElementById('activityTypeSelect');
  const dateStart = document.getElementById('activityDateStart');
  const dateEnd = document.getElementById('activityDateEnd');
  const queryInput = document.getElementById('activityQuery');
  let activityAll = [];
  function renderModal(items){
    if (!modalList) return;
    if (!items || items.length === 0){
      modalList.innerHTML = '<div class="empty-state"><i class="fas fa-inbox"></i><h4>Sin actividad</h4><p>Aquí verás los últimos movimientos del sistema.</p></div>';
      if (modalInfo) modalInfo.textContent = 'Resultados: 0';
      return;
    }
    const rows = items.map(a => `
      <div class="activity-item ${a.type ? 'activity-'+String(a.type).toLowerCase() : ''}">
        <div class="activity-icon"><i class="fas ${typeIcon(a.type)}"></i></div>
        <div class="activity-content">
          <div class="activity-title"><strong>${a.type || 'Evento'}</strong> — ${a.message || ''}</div>
          <div class="activity-meta">
            <span class="activity-time"><i class="far fa-clock"></i> ${fmtDate(a.timestamp)}</span>
            ${a.username ? `<span><i class="fas fa-user"></i> ${a.username}</span>` : ''}
          </div>
        </div>
      </div>
    `).join('');
    modalList.innerHTML = rows;
    if (modalInfo) modalInfo.textContent = `Resultados: ${items.length}`;
  }
  function loadAllActivity(){
    if (modalInfo) modalInfo.textContent = 'Cargando...';
    return fetch('/api/admin/activity?page=0&size=120').then(r=>r.json()).then(data=>{ activityAll = Array.isArray(data)? data : []; renderModal(activityAll); }).catch(()=>{ if (modalInfo) modalInfo.textContent = 'Error al cargar'; });
  }
  function applyModalFilter(){
    const t = typeSelect ? String(typeSelect.value||'').toUpperCase() : '';
    const q = queryInput ? String(queryInput.value||'').toLowerCase() : '';
    const ds = dateStart ? String(dateStart.value||'') : '';
    const de = dateEnd ? String(dateEnd.value||'') : '';
    const filtered = activityAll.filter(a => {
      let ok = true;
      if (t) ok = ok && String(a.type||'').toUpperCase() === t;
      if (q) ok = ok && String(a.message||'').toLowerCase().includes(q);
      if (ds){ try { const ts = new Date(a.timestamp); ok = ok && (ts >= new Date(ds)); } catch(_){} }
      if (de){ try { const ts = new Date(a.timestamp); const end = new Date(de); end.setHours(23,59,59,999); ok = ok && (ts <= end); } catch(_){} }
      return ok;
    });
    renderModal(filtered);
  }
  if (modalOpen) modalOpen.addEventListener('click', () => { loadAllActivity(); });
  if (modalForm) modalForm.addEventListener('submit', (e) => { e.preventDefault(); if (modalInfo) modalInfo.textContent = 'Filtrando...'; applyModalFilter(); });
  if (modalReset) modalReset.addEventListener('click', () => { if (typeSelect) typeSelect.value=''; if (queryInput) queryInput.value=''; if (dateStart) dateStart.value=''; if (dateEnd) dateEnd.value=''; renderModal(activityAll); });
  
});

// Standalone initializer for Categories section (outside dashboard guard)
document.addEventListener('DOMContentLoaded', function(){
  try {
    const tbody = document.getElementById('categoriesTableBody');
    const filterInput = document.getElementById('categoryFilterInput');
    const pager = document.getElementById('categoriesPagination');
    if (!tbody || !pager) return;
    const pageSize = 7;
    let rows = Array.from(tbody.querySelectorAll('tr[data-row]'));
    if (rows.length === 0) rows = Array.from(tbody.querySelectorAll('tr'));
    let filtered = rows.slice();
    let page = 1;

    const norm = (s) => (s||'').toString().toLowerCase();
    const getText = (r) => {
      const tds = r.querySelectorAll('td');
      const name = tds[1] ? tds[1].innerText : '';
      const id = tds[0] ? tds[0].innerText : '';
      return norm(id + ' ' + name);
    };

    const renderPager = (pages) => {
      pager.innerHTML = '';
      const prevBtn = document.createElement('button');
      prevBtn.type = 'button'; prevBtn.className = 'btn btn-outline-secondary'; prevBtn.textContent = '«';
      prevBtn.disabled = page <= 1; prevBtn.onclick = () => { page--; render(); };
      pager.appendChild(prevBtn);
      for (let i=1;i<=pages;i++){
        const b = document.createElement('button');
        b.type = 'button'; b.className = 'btn ' + (i===page?'btn-primary':'btn-outline-secondary');
        b.textContent = i; b.onclick = () => { page = i; render(); };
        pager.appendChild(b);
      }
      const nextBtn = document.createElement('button');
      nextBtn.type = 'button'; nextBtn.className = 'btn btn-outline-secondary'; nextBtn.textContent = '»';
      nextBtn.disabled = page >= pages; nextBtn.onclick = () => { page++; render(); };
      pager.appendChild(nextBtn);
    };

    const render = () => {
      rows.forEach(r => { r.style.display = 'none'; });
      const total = filtered.length;
      const pages = Math.max(1, Math.ceil(total / pageSize));
      if (page > pages) page = pages;
      const start = (page-1)*pageSize;
      const end = start + pageSize;
      filtered.slice(start, end).forEach(r => { r.style.display = ''; });
      renderPager(pages);
    };

    const applyFilter = () => {
      const q = norm(filterInput ? filterInput.value : '');
      filtered = q ? rows.filter(r => getText(r).includes(q)) : rows.slice();
      page = 1; render();
    };

    if (filterInput) filterInput.addEventListener('input', applyFilter);
    render();
  } catch(_) {}
});

// ===== Funcionalidades de ventas =====
(function(){
  const run = function() {
    const form = document.getElementById('ventas-filtros-form');
    const fechaInicioInput = document.getElementById('filtro-fecha-inicio');
    const fechaFinInput = document.getElementById('filtro-fecha-fin');
    const estadoSelect = document.getElementById('filtro-estado');
    const metodoSelect = document.getElementById('filtro-metodo-pago');
    const numeroFacturaInput = document.getElementById('filtro-numero-factura');
    const clienteInput = document.getElementById('filtro-cliente');
    const mesaInput = document.getElementById('filtro-mesa');

    function aplicarFiltros(event) {
        if (event) event.preventDefault();

        const fechaInicio = fechaInicioInput.value;
        const fechaFin = fechaFinInput.value;
        const estado = estadoSelect.value;
        const metodo = metodoSelect.value;
        const numeroFactura = numeroFacturaInput.value.trim().toLowerCase();
        const cliente = clienteInput.value.trim().toLowerCase();
        const mesa = mesaInput.value.trim();

        const filas = document.querySelectorAll('table.table tbody tr[th\\:each], table.table tbody tr[data-fecha-dia]');

        filas.forEach(fila => {
            const fechaDia = fila.getAttribute('data-fecha-dia') || '';
            const estadoFila = (fila.getAttribute('data-estado') || '').toUpperCase();
            const metodoFila = (fila.getAttribute('data-metodo') || '').toUpperCase();
            const numFacturaFila = (fila.getAttribute('data-factura') || '').toLowerCase();
            const clienteFila = (fila.getAttribute('data-cliente') || '').toLowerCase();
            const mesaFila = (fila.getAttribute('data-mesa') || '').toString();

            let visible = true;

            if (fechaInicio && (!fechaDia || fechaDia < fechaInicio)) {
                visible = false;
            }
            if (visible && fechaFin && (!fechaDia || fechaDia > fechaFin)) {
                visible = false;
            }
            if (visible && estado && estadoFila !== estado.toUpperCase()) {
                visible = false;
            }
            if (visible && metodo && metodoFila !== metodo.toUpperCase()) {
                visible = false;
            }
            if (visible && numeroFactura && !numFacturaFila.includes(numeroFactura)) {
                visible = false;
            }
            if (visible && cliente && !clienteFila.includes(cliente)) {
                visible = false;
            }
            if (visible && mesa && mesaFila !== mesa) {
                visible = false;
            }

            fila.setAttribute('data-visible', visible ? '1' : '0');
            fila.style.display = visible ? '' : 'none';
        });

        ventasPage = 0;
        aplicarPaginacion();
    }

    if (form) {
        form.addEventListener('submit', aplicarFiltros);
    }

    // Opcional: filtrar en tiempo real al cambiar los campos
    [fechaInicioInput, fechaFinInput, estadoSelect, metodoSelect,
     numeroFacturaInput, clienteInput, mesaInput]
        .forEach(el => {
            if (el) {
                el.addEventListener('change', aplicarFiltros);
                if (el.tagName === 'INPUT') {
                    el.addEventListener('keyup', aplicarFiltros);
                }
            }
        });

    // Paginación (4 filas por página)
    let ventasPage = 0;
    const ventasSize = 4;
    const ventasPagination = document.getElementById('ventasPagination');

    function aplicarPaginacion(){
        const allRows = Array.from(document.querySelectorAll('table.table tbody tr'))
            .filter(tr => tr.querySelector('td'));
        const filteredRows = allRows.filter(tr => tr.getAttribute('data-visible') !== '0');
        const total = filteredRows.length;
        const totalPages = Math.max(1, Math.ceil(total / ventasSize));
        ventasPage = Math.max(0, Math.min(ventasPage, totalPages - 1));

        // Ocultar todas las filas filtradas y mostrar sólo el slice actual
        filteredRows.forEach(tr => { tr.style.display = 'none'; });
        const start = ventasPage * ventasSize;
        const end = start + ventasSize;
        filteredRows.slice(start, end).forEach(tr => { tr.style.display = ''; });

        renderVentasPagination(totalPages);
    }

    function renderVentasPagination(totalPages){
        if (!ventasPagination) return;
        ventasPagination.innerHTML = '';
        const createBtn = (idx) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-outline-primary btn-sm';
            btn.textContent = String(idx+1);
            if (idx === ventasPage) btn.classList.add('active');
            btn.addEventListener('click', (e)=>{ e.preventDefault(); ventasPage = idx; aplicarPaginacion(); });
            return btn;
        };
        for (let i=0; i<totalPages; i++){ ventasPagination.appendChild(createBtn(i)); }
    }

    // Inicial
    ventasPage = 0;
    aplicarPaginacion();

    // Helpers
    const fmt = (n) => {
        const x = Number(n ?? 0);
        return x.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };
    const parseMoney = (raw) => {
        if (raw == null) return 0;
        let s = String(raw);
        // quitar símbolos excepto separadores
        s = s.replace(/[^0-9.,-]/g, '');
        // si la coma parece ser el separador decimal (caso es_CO y similares)
        const lastComma = s.lastIndexOf(',');
        const lastDot = s.lastIndexOf('.');
        if (lastComma > lastDot) {
            // miles con punto, decimales con coma -> normalizar a punto
            s = s.replace(/\./g, '').replace(/,/g, '.');
        } else {
            // miles con coma, decimales con punto o solo miles con coma
            s = s.replace(/,/g, '');
        }
        const num = Number(s);
        return isNaN(num) ? 0 : num;
    };
    const parseOrden = (ordenStr) => {
        if (!ordenStr) return [];
        try {
            // Si empieza con { o [, intentar parsear directo
            let raw = ordenStr;
            const first = raw.trim().charAt(0);
            if (first !== '{' && first !== '[') {
                // intentar Base64 -> texto
                try { raw = atob(raw); } catch (_) {}
            }
            const data = JSON.parse(raw);

            // Caso 1: array directo
            if (Array.isArray(data)) return data;
            // Caso 2: objeto con items como array
            if (data && Array.isArray(data.items)) return data.items;
            // Caso 3: items es un objeto con propiedad items (anidado)
            if (data && data.items && typeof data.items === 'object' && Array.isArray(data.items.items)) {
                return data.items.items;
            }
            // Caso 4: items es string JSON
            if (data && typeof data.items === 'string') {
                try {
                    let inner = data.items;
                    const ch = inner.trim().charAt(0);
                    if (ch !== '{' && ch !== '[') {
                        try { inner = atob(inner); } catch (_) {}
                    }
                    const parsed = JSON.parse(inner);
                    if (Array.isArray(parsed)) return parsed;
                    if (parsed && Array.isArray(parsed.items)) return parsed.items;
                } catch (e) {}
            }
        } catch (e) {}
        return [];
    };

    const extraFromItem = (it) => {
        const parts = [];
        const campos = ['notas','nota','comentarios','mods','modificadores','adiciones','observaciones'];
        campos.forEach(k => { if (it && it[k]) parts.push(String(it[k])); });
        return parts.join(' · ');
    };

    // Detalle por fila
    const modalEl = document.getElementById('modalDetalleVenta');
    const modal = modalEl ? new bootstrap.Modal(modalEl) : null;
    let ultimoContexto = null; // guarda datos actuales para imprimir desde el modal

    document.querySelectorAll('.btn-detalle').forEach(btn => {
        btn.addEventListener('click', (ev) => {
            const tr = ev.currentTarget.closest('tr');
            if (!tr) return;
            const numero = tr.getAttribute('data-factura') || '';
            const fecha = tr.children[1]?.textContent?.trim() || '';
            const cliente = tr.children[2]?.textContent?.trim() || '';
            const mesa = tr.getAttribute('data-mesa') || '';
            const subtotalVal = parseMoney(tr.children[4]?.textContent || '0');
            const descuentoVal = parseMoney(tr.children[5]?.textContent || '0');
            const totalVal = parseMoney(tr.children[6]?.textContent || '0');

            const metodo = tr.getAttribute('data-metodo') || tr.children[7]?.textContent?.trim() || '';
            const estado = tr.getAttribute('data-estado') || '';
            const orden = tr.getAttribute('data-orden') || '';
            const items = parseOrden(orden);
            let comentariosGenerales = '';
            try {
                const root = JSON.parse(orden);
                if (root && !Array.isArray(root) && root.comentarios) comentariosGenerales = String(root.comentarios);
            } catch(_) {}

            // Poblar modal
            document.getElementById('det-numero').textContent = numero;
            const fechaEl = document.getElementById('det-fecha');
            if (fechaEl) fechaEl.textContent = fecha;

            document.getElementById('det-cliente').textContent = cliente || '—';
            document.getElementById('det-mesa').textContent = mesa || '—';
            document.getElementById('det-metodo').textContent = metodo || '—';
            const estEl = document.getElementById('det-estado');
            estEl.textContent = estado || '—';
            estEl.className = 'badge ' + (estado === 'PAGADO' ? 'bg-success' : estado === 'PENDIENTE' ? 'bg-warning text-dark' : 'bg-danger');
            document.getElementById('det-subtotal').textContent = `$${fmt(subtotalVal)}`;
            document.getElementById('det-descuento').textContent = `$${fmt(descuentoVal)}`;
            document.getElementById('det-total').textContent = `$${fmt(totalVal)}`;

            const ul = document.getElementById('det-items-list');
            if (ul) {
                ul.innerHTML = '';
                items.forEach(it => {
                    const nombre = it.productoNombre || it.nombre || it.producto || it.name || 'Item';
                    const cantidad = Math.max(1, Number(it.cantidad || it.quantity || it.qty || 1));
                    const precio = Number(it.precio || it.price || 0);
                    const total = cantidad * precio;
                    const li = document.createElement('li');
                    li.className = 'list-group-item px-0 py-1 d-flex justify-content-between align-items-center';
                    li.innerHTML = `<span>${nombre} <span class="text-muted">x${cantidad}</span></span><strong>$${fmt(total)}</strong>`;
                    ul.appendChild(li);
                });
            }

            const comentariosEl = document.getElementById('det-comentarios');
            if (comentariosEl) {
                if (comentariosGenerales) {
                    comentariosEl.style.display = '';
                    comentariosEl.textContent = `Comentarios generales: ${comentariosGenerales}`;
                } else {
                    comentariosEl.style.display = 'none';
                    comentariosEl.textContent = '';
                }
            }

            ultimoContexto = { numero, fecha, cliente, mesa, metodo, estado, items, subtotal: subtotalVal, descuento: descuentoVal, total: totalVal };
            if (modal) modal.show();
        });
    });

    // Imprimir por fila (térmica 58mm)
    function imprimirTicket(ctx) {
        const w = window.open('', '_blank');
        const rows = (ctx.items || []).map(it => {
            const nombre = (it.productoNombre || it.nombre || it.producto || it.name || 'Item');
            const cantidad = Number(it.cantidad || it.quantity || it.qty || 1);
            const precio = Number(it.precio || it.price || 0);
            const importe = cantidad * precio;
            const notas = extraFromItem(it);
            return `
                <tr><td colspan="4"><strong>${nombre}</strong></td></tr>
                <tr><td colspan="2" class="text-start">x${cantidad} @ $${fmt(precio)}</td><td colspan="2" class="text-end">$${fmt(importe)}</td></tr>
                ${notas ? `<tr><td colspan="4" class="text-muted small">Notas: ${notas}</td></tr>` : ''}
            `;
        }).join('');
        const html = `
        <html>
          <head>
            <title>Ticket ${ctx.numero || ''}</title>
            <meta charset="utf-8" />
            <style>
              @page { size: 58mm auto; margin: 5mm; }
              body{font-family: 'Courier New', monospace; width: 58mm; margin: 0 auto;}
              h2{margin:0 0 4px 0; font-size: 14px;}
              .muted{color:#666;font-size:10px}
              table{width:100%; border-collapse:collapse;}
              th,td{padding:3px 0; border-bottom:1px dashed #ccc; font-size:11px}
              .text-end{text-align:right}
              .text-center{text-align:center}
              hr{border:none; border-top:1px dashed #ccc; margin:6px 0}
            </style>
          </head>
          <body>
            <div style="text-align:center; margin-bottom:6px;">
              <h2>GET BACK</h2>
              <div class="muted">Ticket de Venta</div>
            </div>
            <div class="muted">Factura: ${ctx.numero || ''} | Mesa: ${ctx.mesa || '—'} | ${ctx.fecha || ''}</div>
            <div class="muted">Cliente: ${ctx.cliente || '—'} | Pago: ${ctx.metodo || '—'} | Estado: ${ctx.estado || '—'}</div>
            <hr>
            <table>
              <thead><tr><th colspan="4" class="text-start">Detalle</th></tr></thead>
              <tbody>${rows}</tbody>
            </table>
            <div class="text-end" style="margin-top:6px;">
              <div>Subtotal: <strong>$${fmt(ctx.subtotal)}</strong></div>
              <div>Descuento: <strong>$${fmt(ctx.descuento)}</strong></div>
              <div>Total: <strong>$${fmt(ctx.total)}</strong></div>
            </div>
            <p style="text-align:center; margin-top:8px;">¡Gracias por su compra!</p>
          </body>
        </html>`;
        w.document.write(html);

        w.document.close();
        w.focus();
        w.print();
    }

    document.querySelectorAll('.btn-imprimir').forEach(btn => {
        btn.addEventListener('click', (ev) => {
            const tr = ev.currentTarget.closest('tr');
            if (!tr) return;
            const ctx = {
                numero: tr.getAttribute('data-factura') || '',
                fecha: tr.children[1]?.textContent?.trim() || '',
                cliente: tr.children[2]?.textContent?.trim() || '',
                mesa: tr.getAttribute('data-mesa') || '',
                metodo: tr.getAttribute('data-metodo') || tr.children[7]?.textContent?.trim() || '',
                estado: tr.getAttribute('data-estado') || '',
                items: parseOrden(tr.getAttribute('data-orden') || ''),
                subtotal: parseMoney(tr.children[4]?.textContent || '0'),
                descuento: parseMoney(tr.children[5]?.textContent || '0'),
                total: parseMoney(tr.children[6]?.textContent || '0'),
            };

            imprimirTicket(ctx);
        });
    });

    const btnImprimirModal = document.getElementById('btn-imprimir-modal');
    if (btnImprimirModal) {
        btnImprimirModal.addEventListener('click', () => {
            if (ultimoContexto) imprimirTicket(ultimoContexto);
        });
    }

    // Descargar ticket como PDF (58mm)
    function descargarTicketPDF(ctx) {
        const { jsPDF } = window.jspdf || {};
        if (!jsPDF) {
            alert('No se pudo cargar jsPDF');
            return;
        }
        // 58mm de ancho, fuente monoespaciada
        const doc = new jsPDF({ unit: 'mm', format: [58, 200] });
        doc.setFont('courier', 'normal');
        doc.setFontSize(11);
        doc.text('GET BACK', 29, 6, { align: 'center' });
        doc.setFontSize(9);
        doc.text('Ticket de Venta', 29, 10, { align: 'center' });

        const linea1 = `Factura: ${ctx.numero || ''}  Mesa: ${ctx.mesa || '—'}`;
        const linea2 = `Cliente: ${ctx.cliente || '—'}`;
        const linea3 = `Pago: ${ctx.metodo || '—'}  Estado: ${ctx.estado || '—'}`;
        doc.text(linea1, 2, 16);
        doc.text(linea2, 2, 20);
        doc.text(linea3, 2, 24);

        let y = 28;
        const maxW = 56; // ancho útil aproximado
        const items = ctx.items || [];
        items.forEach(it => {
            const nombre = String(it.productoNombre || it.nombre || it.producto || it.name || 'Item');
            const cantidad = Number(it.cantidad || it.quantity || it.qty || 1);
            const precio = Number(it.precio || it.price || 0);
            const importe = (cantidad * precio);
            const notas = extraFromItem(it);

            // Nombre en negrilla ligera
            doc.setFontSize(9);
            doc.text(nombre.substring(0, 40), 2, y);
            y += 4;

            // Línea de cantidad, precio e importe (importe a la derecha)
            const linea = `x${cantidad} @ $${precio.toFixed(2)}`;
            doc.text(linea, 2, y);
            const importeStr = `$${importe.toFixed(2)}`;
            const iw = doc.getTextWidth(importeStr);
            doc.text(importeStr, 2 + maxW - iw, y);
            y += 4;

            if (notas) {
                doc.setFontSize(8);
                const wrapped = doc.splitTextToSize(`Notas: ${notas}`, maxW);
                doc.text(wrapped, 2, y);
                y += wrapped.length * 3.5 + 1;
            }
        });

        // Totales
        y += 2;
        doc.setFontSize(9);
        doc.text(`Subtotal: $${fmt(ctx.subtotal)}`, 2, y); y += 4;
        doc.text(`Descuento: $${fmt(ctx.descuento)}`, 2, y); y += 4;
        doc.text(`Total: $${fmt(ctx.total)}`, 2, y);

        doc.save(`ticket_${(ctx.numero||'').replace(/\W+/g,'_')}.pdf`);
    }

    const btnDescargarPdf = document.getElementById('btn-descargar-pdf');
    if (btnDescargarPdf) {
        btnDescargarPdf.addEventListener('click', () => {
            if (ultimoContexto) descargarTicketPDF(ultimoContexto);
        });
    }

    // Imprimir tabla completa
    const btnImprimirTabla = document.getElementById('btn-imprimir-tabla');
    if (btnImprimirTabla) {
        btnImprimirTabla.addEventListener('click', () => {
            const tabla = document.querySelector('.card-body .table-responsive').innerHTML;
            const w = window.open('', '_blank');
            w.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>Historial de Ventas</title><style>body{font-family:Arial,sans-serif;padding:16px} table{width:100%;border-collapse:collapse} th,td{padding:8px;border-bottom:1px solid #eee}</style></head><body>${tabla}</body></html>`);
            w.document.close();
            w.focus();
            w.print();
        });
    }

    // Exportar a PDF (Historial)
    const btnExportar = document.getElementById('btn-exportar-ventas');
    if (btnExportar) {
        btnExportar.addEventListener('click', async () => {
            const { jsPDF } = window.jspdf || {};
            if (!jsPDF || !window.jspdf || !('autoTable' in (jsPDF.API || {}))) {
                alert('No se pudo cargar el generador de PDF. Verifique su conexión.');
                return;
            }
            const doc = new jsPDF('l', 'pt', 'a4'); // horizontal para más columnas

            const rows = Array.from(document.querySelectorAll('table.table tbody tr'))
                .filter(tr => tr.querySelector('td')) // omitir fila de empty-state
                .filter(tr => tr.style.display !== 'none') // respetar filtros actuales
                .map(tr => {
                    const tds = tr.querySelectorAll('td');
                    return [
                        tds[0]?.textContent?.trim() || '', // factura
                        tds[1]?.textContent?.trim() || '', // fecha
                        tds[2]?.textContent?.trim() || '', // cliente
                        tds[3]?.textContent?.trim() || '', // mesa
                        tds[4]?.textContent?.trim() || '', // subtotal
                        tds[5]?.textContent?.trim() || '', // descuento
                        tds[6]?.textContent?.trim() || '', // total
                        tds[7]?.textContent?.trim() || '', // método
                        (tr.getAttribute('data-estado') || tds[8]?.innerText?.trim() || ''), // estado
                    ];
                });

            const fecha = new Date();
            const titulo = 'Historial de Ventas';
            doc.setFontSize(16);
            doc.text(titulo, 40, 40);
            doc.setFontSize(10);
            doc.text(`Generado: ${fecha.toLocaleDateString()} ${fecha.toLocaleTimeString()}`, 40, 58);

            doc.autoTable({
                head: [[
                    '# Factura','Fecha','Cliente','Mesa','Subtotal','Descuento','Total','Método','Estado'
                ]],
                body: rows,
                startY: 70,
            });

            doc.save(`historial_ventas_${fecha.getFullYear()}-${fecha.getMonth()+1}-${fecha.getDate()}.pdf`);
        });
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', run);
  } else {
    run();
  }
})();
