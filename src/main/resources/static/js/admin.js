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
		console.log("GETBACK Admin Panel inicializado ✅");
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
			}, 5000);
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

// Inicializar
document.addEventListener("DOMContentLoaded", () => adminApp.init());
window.adminApp = adminApp;


document.addEventListener('DOMContentLoaded', function() {

	// ---- Rellenar modal editar ----
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

	// ---- Rellenar modal eliminar ----
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
				// Ajusta esta URL si tu endpoint es /admin/users/delete/{id} en lugar de /users/delete/{id}
				confirmLink.setAttribute('href', /*[[*/ /* th:remove="tag" */ '' + '/users/delete/' + id);
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
  filterBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      const filter = this.getAttribute('data-filter');
      filterBtns.forEach(b => b.classList.remove('active'));
      this.classList.add('active');
      chartContainers.forEach(c => c.classList.remove('active'));
      const target = document.getElementById('charts-' + filter);
      if (target) target.classList.add('active');
    });
  });

  // Carousel controls
  const prevBtn = document.getElementById('carouselPrev');
  const nextBtn = document.getElementById('carouselNext');
  function getActiveGrid() { return document.querySelector('.charts-container.active .charts-grid'); }
  function scrollByAmount(dir) {
    const grid = getActiveGrid();
    if (!grid) return;
    const amount = 220 * dir; // approx one card + gap
    grid.scrollBy({ left: amount, behavior: 'smooth' });
  }
  if (prevBtn) prevBtn.addEventListener('click', () => scrollByAmount(-1));
  if (nextBtn) nextBtn.addEventListener('click', () => scrollByAmount(1));

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
    })
    .catch(err => console.error('Error cargando stats del dashboard:', err));

  // Recent activity
  const list = document.getElementById('recentActivity');
  const btn = document.getElementById('activityRefreshBtn');
  const prev = document.getElementById('activityPrev');
  const next = document.getElementById('activityNext');
  const pageLabel = document.getElementById('activityPageLabel');
  let activityPage = 0;
  const activitySize = 5;
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

