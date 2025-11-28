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

// Inicializar
document.addEventListener("DOMContentLoaded", () => adminApp.init());
window.adminApp = adminApp;


