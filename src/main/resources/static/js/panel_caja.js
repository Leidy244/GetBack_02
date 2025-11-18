// panel_caja.js - Funcionalidades para el panel de caja

class PanelCaja {
    constructor() {
        this.carrito = [];
        this.total = 0;
        this.metodoPago = 'efectivo';
        this.init();
    }

    init() {
        this.setupNavegacion();
        this.setupEventListeners();
        this.cargarProductos();
        this.actualizarCarrito();
        this.restoreTheme(); // Restaurar tema al inicializar
        this.hidePreloader();

        // Inicializar orden del historial de pagos si existe
        this.setupHistorialOrden();
    }

    setupEventListeners() {
        // Punto de venta
        document.getElementById('btn-buscar')?.addEventListener('click', this.buscarProductos.bind(this));
        document.getElementById('buscar-productos')?.addEventListener('input', this.buscarProductos.bind(this));
        
        document.querySelectorAll('.filtro-categoria').forEach(filtro => {
            filtro.addEventListener('click', this.filtrarCategoria.bind(this));
        });

        document.getElementById('btn-vaciar-carrito')?.addEventListener('click', this.vaciarCarrito.bind(this));
        document.getElementById('btn-finalizar-venta')?.addEventListener('click', this.finalizarVenta.bind(this));
        document.getElementById('btn-imprimir')?.addEventListener('click', this.imprimirTicket.bind(this));

        // Métodos de pago
        document.querySelectorAll('.pestana-pago').forEach(pestana => {
            pestana.addEventListener('click', this.cambiarMetodoPago.bind(this));
        });

        // Inicio de caja
        document.getElementById('form-inicio-caja')?.addEventListener('submit', this.iniciarCaja.bind(this));

        // Modo oscuro
        const darkModeToggle = document.getElementById("darkModeToggle");
        if (darkModeToggle) {
            darkModeToggle.addEventListener("click", () => this.toggleDarkMode());
        };

        // Configuración del tema desde el formulario
        const temaSelect = document.getElementById('tema');
        if (temaSelect) {
            temaSelect.addEventListener('change', (e) => this.cambiarTemaDesdeConfiguracion(e.target.value));
        }

        // Pagos de pedidos (sección Pagos)
        this.setupPagosSection();
    }

    // Ordenar historial de pagos (más reciente / más viejo)
    setupHistorialOrden() {
        const selectOrden = document.getElementById('historial-orden');
        const tabla = document.getElementById('tabla-historial-pagos');
        if (!selectOrden || !tabla) return;

        const cuerpo = tabla.querySelector('tbody');
        if (!cuerpo) return;

        const ordenar = () => {
            const filas = Array.from(cuerpo.querySelectorAll('tr'));
            const modo = selectOrden.value; // 'reciente' o 'viejo'

            filas.sort((a, b) => {
                const fa = new Date(a.getAttribute('data-fecha')).getTime() || 0;
                const fb = new Date(b.getAttribute('data-fecha')).getTime() || 0;
                return modo === 'reciente' ? fb - fa : fa - fb;
            });

            // Reinsertar filas en el nuevo orden y actualizar el índice #
            filas.forEach((tr, idx) => {
                const celdaIndice = tr.querySelector('td');
                if (celdaIndice) celdaIndice.textContent = (idx + 1).toString();
                cuerpo.appendChild(tr);
            });
        };

        // Orden inicial
        ordenar();
        // Orden al cambiar el select
        selectOrden.addEventListener('change', ordenar);
    }

    // ===== PAGOS DE PEDIDOS (SECCIÓN PAGOS) =====
    setupPagosSection() {
        const botonesAbrirModal = document.querySelectorAll('.btn-abrir-modal-pago');
        const botonesVerDetalle = document.querySelectorAll('.ver-detalle-pago');
        const modalRecibido = document.getElementById('modal-recibido');
        const modalCambio = document.getElementById('modal-cambio');
        const btnConfirmarPago = document.getElementById('btn-confirmar-pago');
        const modalInfo = document.getElementById('modal-info');
        const modalError = document.getElementById('modal-error');

        // Helper: formatear montos sin decimales, con separador de miles usando punto (7.000, 70.000)
        const formatearMonto = (valor) => {
            const numero = Number(valor);
            if (!Number.isFinite(numero)) return valor;
            const entero = Math.round(numero);
            return entero.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        };

        // Helper: formatear detalle legible a partir del JSON de orden
        const formatearDetalle = (detalleJson) => {
            if (!detalleJson) return 'Sin detalle';
            try {
                const data = JSON.parse(detalleJson);
                const items = Array.isArray(data.items) ? data.items : [];
                if (items.length === 0) return 'Sin items en el pedido';

                return items.map(item => {
                    const cantidad = item.cantidad ?? 0;
                    const nombre = item.productoNombre ?? 'Producto';
                    const subtotal = item.subtotal ?? 0;

                    // Quitar los decimales (.00) en la visualización del subtotal
                    const subtotalNumber = Number(subtotal);
                    const subtotalFormateado = Number.isFinite(subtotalNumber)
                        ? subtotalNumber.toString()
                        : subtotal;

                    return `${cantidad}x ${nombre} - $${subtotalFormateado}`;
                }).join('\n');
            } catch (e) {
                console.error('No se pudo parsear el detalle del pedido:', e);
                return detalleJson;
            }
        };

        // Aplicar resumen legible en la tabla para cada botón de detalle
        botonesVerDetalle.forEach(btn => {
            const detalle = btn.getAttribute('data-detalle');
            const resumenCompleto = formatearDetalle(detalle);
            const resumenLinea = resumenCompleto.split('\n').join(' • ');

            // Tooltip al pasar el mouse
            btn.title = resumenCompleto;

            // Texto visible en la tabla (resumen corto)
            const spanTexto = btn.querySelector('.detalle-resumen-text');
            if (spanTexto) {
                // Limitar longitud para que no rompa el diseño
                spanTexto.textContent = resumenLinea.length > 80
                    ? resumenLinea.substring(0, 77) + '...'
                    : resumenLinea;
            }
        });

        // Abrir modal de pago (desde la sección Pagos - pendientes)
        botonesAbrirModal.forEach(btn => {
            btn.addEventListener('click', () => {
                const pedidoId = btn.getAttribute('data-pedido-id');
                const mesa = btn.getAttribute('data-mesa');
                const total = parseFloat(btn.getAttribute('data-total'));

                const detalle = btn.getAttribute('data-detalle');

                // Llenar datos del modal
                document.getElementById('modal-mesa').textContent = mesa;
                const detalleFormateado = formatearDetalle(detalle);
                // Usar <br> para que cada producto se muestre en una línea separada
                document.getElementById('modal-detalle').innerHTML = detalleFormateado.replace(/\n/g, '<br>');

                // Total sin .00 si es entero
                document.getElementById('modal-total').textContent = `$${formatearMonto(total)}`;

                document.getElementById('modal-pedido-id').value = pedidoId;

                // Limpiar campos
                modalRecibido.value = '';
                modalRecibido.readOnly = false;
                modalCambio.value = '$0';
                modalCambio.readOnly = true;
                btnConfirmarPago.disabled = true;
                modalInfo.style.display = 'block';
                modalError.style.display = 'none';

                // Asegurar que el botón de confirmar sea visible en modo cobro
                btnConfirmarPago.style.display = 'inline-block';

                // Abrir modal
                const modal = new bootstrap.Modal(document.getElementById('modalPago'));
                modal.show();
            });
        });

        // Abrir modal de detalle en modo solo lectura desde el historial de pagos
        botonesVerDetalle.forEach(btn => {
            const origen = btn.getAttribute('data-origen');
            if (origen === 'historial') {
                btn.addEventListener('click', () => {
                    const mesa = btn.getAttribute('data-mesa');
                    const total = parseFloat(btn.getAttribute('data-total')) || 0;
                    const detalle = btn.getAttribute('data-detalle');
                    const recibidoAttr = btn.getAttribute('data-recibido');
                    const cambioAttr = btn.getAttribute('data-cambio');

                    let recibido = recibidoAttr ? parseFloat(recibidoAttr) : NaN;
                    let cambio = cambioAttr ? parseFloat(cambioAttr) : NaN;

                    // Si no tenemos monto recibido pero sí cambio, intentar reconstruirlo
                    if (!Number.isFinite(recibido) && Number.isFinite(cambio)) {
                        recibido = total + cambio;
                    }

                    // Si no tenemos cambio pero sí monto recibido, calcular cambio
                    if (!Number.isFinite(cambio) && Number.isFinite(recibido)) {
                        cambio = recibido - total;
                    }

                    document.getElementById('modal-mesa').textContent = mesa;
                    const detalleFormateado = formatearDetalle(detalle);
                    document.getElementById('modal-detalle').innerHTML = detalleFormateado.replace(/\n/g, '<br>');

                    document.getElementById('modal-total').textContent = `$${formatearMonto(total)}`;

                    // Mostrar montos en modo solo lectura (si existen)
                    if (Number.isFinite(recibido)) {
                        modalRecibido.value = formatearMonto(recibido);
                    } else {
                        modalRecibido.value = '';
                    }
                    modalRecibido.readOnly = true;
                    if (Number.isFinite(cambio)) {
                        modalCambio.value = `$${formatearMonto(cambio)}`;
                    } else {
                        modalCambio.value = '$0';
                    }

                    modalCambio.readOnly = true;

                    modalInfo.style.display = 'none';
                    modalError.style.display = 'none';

                    // Ocultar botón de confirmar en modo historial
                    btnConfirmarPago.disabled = true;
                    btnConfirmarPago.style.display = 'none';

                    const modal = new bootstrap.Modal(document.getElementById('modalPago'));
                    modal.show();
                });
            }
        });

        // Calcular cambio en tiempo real en el modal
        if (modalRecibido) {
            modalRecibido.addEventListener('input', () => {
                const totalElement = document.getElementById('modal-total');
                // El texto puede venir como "$7.000", eliminamos $ y puntos antes de parsear
                const total = parseFloat(totalElement.textContent.replace('$', '').replace(/\./g, '')) || 0;

                const recibido = parseFloat(modalRecibido.value) || 0;

                if (recibido > 0) {
                    modalInfo.style.display = 'none';
                    
                    if (recibido >= total) {
                        const cambio = recibido - total;
                        modalCambio.value = `$${formatearMonto(cambio)}`;

                        modalError.style.display = 'none';
                        btnConfirmarPago.disabled = false;
                        
                        // Actualizar campo oculto
                        document.getElementById('modal-monto-recibido-hidden').value = recibido.toFixed(2);
                    } else {
                        modalCambio.value = '$0.00';
                        modalError.style.display = 'block';
                        btnConfirmarPago.disabled = true;
                    }
                } else {
                    modalCambio.value = '$0.00';
                    modalInfo.style.display = 'block';
                    modalError.style.display = 'none';
                    btnConfirmarPago.disabled = true;
                }
            });
        }
    }

    /* ===== FUNCIONALIDADES MODO OSCURO ===== */
    toggleDarkMode() {
        const body = document.body;
        const currentTheme = body.getAttribute("data-theme");
        const icon = document.querySelector("#darkModeToggle i");
        
        if (currentTheme === "dark") {
            body.removeAttribute("data-theme");
            if (icon) icon.className = "fas fa-moon";
            localStorage.setItem("caja-theme", "light");
            this.showNotification("Modo claro activado", "success");
            this.actualizarSelectTema('claro');
        } else {
            body.setAttribute("data-theme", "dark");
            if (icon) icon.className = "fas fa-sun";
            localStorage.setItem("caja-theme", "dark");
            this.showNotification("Modo oscuro activado", "success");
            this.actualizarSelectTema('oscuro');
        }
    }

    cambiarTemaDesdeConfiguracion(tema) {
        const body = document.body;
        const icon = document.querySelector("#darkModeToggle i");
        
        if (tema === "oscuro") {
            body.setAttribute("data-theme", "dark");
            if (icon) icon.className = "fas fa-sun";
            localStorage.setItem("caja-theme", "dark");
            this.showNotification("Modo oscuro activado", "success");
        } else {
            body.removeAttribute("data-theme");
            if (icon) icon.className = "fas fa-moon";
            localStorage.setItem("caja-theme", "light");
            this.showNotification("Modo claro activado", "success");
        }
    }

    actualizarSelectTema(tema) {
        const temaSelect = document.getElementById('tema');
        if (temaSelect) {
            temaSelect.value = tema;
        }
    }

    restoreTheme() {
        const savedTheme = localStorage.getItem("caja-theme");
        if (savedTheme === "dark") {
            document.body.setAttribute("data-theme", "dark");
            const icon = document.querySelector("#darkModeToggle i");
            if (icon) icon.className = "fas fa-sun";
            this.actualizarSelectTema('oscuro');
        } else {
            this.actualizarSelectTema('claro');
        }
    }

    showNotification(message, type = "info") {
        // Crear notificación temporal
        const notification = document.createElement("div");
        notification.className = `alert alert-${type === 'success' ? 'success' : type === 'error' ? 'danger' : 'info'} alert-dismissible fade show`;
        notification.style.cssText = `
            position: fixed;
            top: 100px;
            right: 20px;
            z-index: 9999;
            min-width: 300px;
            animation: slideInRight 0.3s ease;
        `;
        
        notification.innerHTML = `
            <strong>${type === 'success' ? 'Éxito' : type === 'error' ? 'Error' : 'Info'}</strong> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(notification);
        
        // Auto-remover después de 3 segundos
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, 3000);
    }

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

    /* ===== FUNCIONALIDADES EXISTENTES ===== */
    setupNavegacion() {
        // Navegación del sidebar
        document.querySelectorAll('.caja-nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                
                // Quitar clase active de todos los links
                document.querySelectorAll('.caja-nav-link').forEach(l => {
                    l.classList.remove('active');
                });
                
                // Agregar clase active al link clickeado
                link.classList.add('active');
                
                // Ocultar todas las secciones
                document.querySelectorAll('.seccion-contenido').forEach(seccion => {
                    seccion.classList.remove('visible');
                });
                
                // Mostrar la sección correspondiente
                const sectionId = link.getAttribute('data-section');
                document.getElementById(sectionId).classList.add('visible');
            });
        });
    }

    cargarProductos() {
        // Simulación de carga de productos
        const productos = [
            { id: 1, nombre: 'Combo Rockero', precio: 250, categoria: 'comidas', stock: 15 },
            { id: 2, nombre: 'Cerveza Artesanal', precio: 80, categoria: 'bebidas', stock: 24 },
            { id: 3, nombre: 'Pizza Familiar', precio: 320, categoria: 'comidas', stock: 8 },
            { id: 4, nombre: 'Refresco Grande', precio: 35, categoria: 'bebidas', stock: 30 },
            { id: 5, nombre: 'Helado de Vainilla', precio: 45, categoria: 'postres', stock: 12 },
            { id: 6, nombre: 'Café Americano', precio: 25, categoria: 'bebidas', stock: 20 },
        ];

        const grid = document.getElementById('grid-productos');
        if (grid) {
            grid.innerHTML = productos.map(producto => `
                <div class="tarjeta-producto" data-id="${producto.id}" data-categoria="${producto.categoria}">
                    <div class="nombre-producto">${producto.nombre}</div>
                    <div class="precio-producto">$${producto.precio.toFixed(2)}</div>
                    <div class="stock-producto">Disponible: ${producto.stock}</div>
                </div>
            `).join('');

            // Agregar event listeners a los productos
            grid.querySelectorAll('.tarjeta-producto').forEach(producto => {
                producto.addEventListener('click', this.agregarAlCarrito.bind(this));
            });
        }
    }

    agregarAlCarrito(event) {
        const productoElement = event.currentTarget;
        const productoId = productoElement.getAttribute('data-id');
        const productoNombre = productoElement.querySelector('.nombre-producto').textContent;
        const productoPrecio = parseFloat(productoElement.querySelector('.precio-producto').textContent.replace('$', ''));

        const productoExistente = this.carrito.find(item => item.id === productoId);

        if (productoExistente) {
            productoExistente.cantidad++;
        } else {
            this.carrito.push({
                id: productoId,
                nombre: productoNombre,
                precio: productoPrecio,
                cantidad: 1
            });
        }

        this.actualizarCarrito();
    }

    actualizarCarrito() {
        const itemsContainer = document.getElementById('items-carrito');
        const totalElement = document.getElementById('total-carrito');

        if (itemsContainer && totalElement) {
            this.total = this.carrito.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
            
            itemsContainer.innerHTML = this.carrito.map(item => `
                <div class="item-carrito" data-id="${item.id}">
                    <div class="nombre-item">${item.nombre}</div>
                    <div class="cantidad-item">
                        <button class="boton-cantidad" data-action="decrement">-</button>
                        <span>${item.cantidad}</span>
                        <button class="boton-cantidad" data-action="increment">+</button>
                    </div>
                    <div class="precio-item">$${(item.precio * item.cantidad).toFixed(2)}</div>
                    <button class="boton-eliminar"><i class="fas fa-trash"></i></button>
                </div>
            `).join('');

            totalElement.textContent = `$${this.total.toFixed(2)}`;

            // Agregar event listeners a los botones del carrito
            itemsContainer.querySelectorAll('.boton-cantidad').forEach(boton => {
                boton.addEventListener('click', this.ajustarCantidad.bind(this));
            });

            itemsContainer.querySelectorAll('.boton-eliminar').forEach(boton => {
                boton.addEventListener('click', this.eliminarDelCarrito.bind(this));
            });
        }
    }

    ajustarCantidad(event) {
        const boton = event.currentTarget;
        const itemElement = boton.closest('.item-carrito');
        const itemId = itemElement.getAttribute('data-id');
        const accion = boton.getAttribute('data-action');
        
        const item = this.carrito.find(item => item.id === itemId);
        
        if (item) {
            if (accion === 'increment') {
                item.cantidad++;
            } else if (accion === 'decrement' && item.cantidad > 1) {
                item.cantidad--;
            }
            
            this.actualizarCarrito();
        }
    }

    eliminarDelCarrito(event) {
        const boton = event.currentTarget;
        const itemElement = boton.closest('.item-carrito');
        const itemId = itemElement.getAttribute('data-id');
        
        this.carrito = this.carrito.filter(item => item.id !== itemId);
        this.actualizarCarrito();
    }

    vaciarCarrito() {
        if (confirm('¿Estás seguro de que quieres vaciar el carrito?')) {
            this.carrito = [];
            this.actualizarCarrito();
        }
    }

    finalizarVenta() {
        if (this.carrito.length === 0) {
            alert('El carrito está vacío');
            return;
        }

        if (this.metodoPago === 'efectivo') {
            const montoRecibido = parseFloat(document.getElementById('monto-recibido').value);
            if (isNaN(montoRecibido) || montoRecibido < this.total) {
                alert('El monto recibido debe ser igual o mayor al total');
                return;
            }
        }

        // Simular proceso de venta
        alert(`Venta realizada exitosamente!\nTotal: $${this.total.toFixed(2)}\nMétodo de pago: ${this.metodoPago}`);
        this.carrito = [];
        this.actualizarCarrito();
        document.getElementById('monto-recibido').value = '';
        this.calcularCambio();
    }

    imprimirTicket() {
        if (this.carrito.length === 0) {
            alert('No hay productos en el carrito para imprimir');
            return;
        }

        // Simular impresión
        const ticketContent = this.generarTicket();
        const ventanaImpresion = window.open('', '_blank');
        ventanaImpresion.document.write(`
            <html>
                <head>
                    <title>Ticket de Venta</title>
                    <style>
                        body { font-family: monospace; margin: 20px; }
                        .ticket-header { text-align: center; margin-bottom: 20px; }
                        .ticket-item { display: flex; justify-content: space-between; margin: 5px 0; }
                        .ticket-total { border-top: 1px solid #000; margin-top: 10px; padding-top: 10px; font-weight: bold; }
                    </style>
                </head>
                <body>
                    ${ticketContent}
                </body>
            </html>
        `);
        ventanaImpresion.document.close();
        ventanaImpresion.print();
    }

    generarTicket() {
        let ticket = `
            <div class="ticket-header">
                <h2>Get Back - Ticket de Venta</h2>
                <p>Fecha: ${new Date().toLocaleDateString()}</p>
                <p>Hora: ${new Date().toLocaleTimeString()}</p>
            </div>
        `;

        this.carrito.forEach(item => {
            ticket += `
                <div class="ticket-item">
                    <span>${item.nombre} x${item.cantidad}</span>
                    <span>$${(item.precio * item.cantidad).toFixed(2)}</span>
                </div>
            `;
        });

        ticket += `
            <div class="ticket-total">
                <span>Total:</span>
                <span>$${this.total.toFixed(2)}</span>
            </div>
            <div style="text-align: center; margin-top: 20px;">
                <p>¡Gracias por su compra!</p>
            </div>
        `;

        return ticket;
    }

    buscarProductos() {
        const termino = document.getElementById('buscar-productos').value.toLowerCase();
        const productos = document.querySelectorAll('.tarjeta-producto');
        
        productos.forEach(producto => {
            const nombre = producto.querySelector('.nombre-producto').textContent.toLowerCase();
            if (nombre.includes(termino)) {
                producto.style.display = 'block';
            } else {
                producto.style.display = 'none';
            }
        });
    }

    filtrarCategoria(event) {
        const categoria = event.currentTarget.getAttribute('data-categoria');
        
        // Actualizar botones de filtro
        document.querySelectorAll('.filtro-categoria').forEach(filtro => {
            filtro.classList.remove('activo');
        });
        event.currentTarget.classList.add('activo');
        
        // Filtrar productos
        const productos = document.querySelectorAll('.tarjeta-producto');
        
        productos.forEach(producto => {
            const productoCategoria = producto.getAttribute('data-categoria');
            
            if (categoria === 'todos' || productoCategoria === categoria) {
                producto.style.display = 'block';
            } else {
                producto.style.display = 'none';
            }
        });
    }

    cambiarMetodoPago(event) {
        const metodo = event.currentTarget.getAttribute('data-metodo');
        
        // Actualizar pestañas
        document.querySelectorAll('.pestana-pago').forEach(pestana => {
            pestana.classList.remove('activo');
        });
        event.currentTarget.classList.add('activo');
        
        // Actualizar contenido
        document.querySelectorAll('.contenido-pago').forEach(contenido => {
            contenido.classList.remove('visible');
        });
        document.getElementById(`pago-${metodo}`).classList.add('visible');
        
        this.metodoPago = metodo;
    }

    calcularCambio() {
        const montoRecibido = parseFloat(document.getElementById('monto-recibido').value);
        const cambioElement = document.getElementById('cambio');
        
        if (!isNaN(montoRecibido) && montoRecibido >= this.total) {
            const cambio = montoRecibido - this.total;
            cambioElement.value = `$${cambio.toFixed(2)}`;
        } else {
            cambioElement.value = '$0.00';
        }
    }

    iniciarCaja(event) {
        event.preventDefault();
        const efectivoInicial = parseFloat(document.getElementById('efectivo-inicial').value);
        
        if (isNaN(efectivoInicial) || efectivoInicial < 0) {
            alert('Por favor ingresa un monto válido para el efectivo inicial');
            return;
        }
        
        // Simular inicio de caja
        alert(`Caja iniciada exitosamente\nEfectivo inicial: $${efectivoInicial.toFixed(2)}`);
        document.getElementById('form-inicio-caja').reset();
    }
}

// Inicializar la aplicación cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    window.panelCaja = new PanelCaja();
});

// Manejo del logout
window.adminApp = {
    logout: function() {
        if (confirm('¿Estás seguro de que quieres cerrar sesión?')) {
            // Aquí iría la lógica real de logout
            window.location.href = '/login';
        }
    }
};