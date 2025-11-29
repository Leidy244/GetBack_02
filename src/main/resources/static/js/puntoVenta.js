document.addEventListener('DOMContentLoaded', function () {
    // ========== VARIABLES GLOBALES ==========
    let carrito = JSON.parse(localStorage.getItem('carritoPOS') || '[]');
    let totalGlobal = 0;
    let pedidoActual = null;

    // Elementos importantes - PUNTO DE VENTA
    const itemsCarritoEl = document.getElementById('items-carrito');
    const totalCarritoEl = document.getElementById('total-carrito');
    const btnFinalizarVenta = document.getElementById('btn-finalizar-venta');
    const btnVaciarCarrito = document.getElementById('btn-vaciar-carrito');

    // Modal Punto de Venta y sus elementos
    const modalConfirmarVenta = document.getElementById('modalConfirmarVenta');
    const tbodyDetalle = document.getElementById('detalle-venta-modal');
    const totalModal = document.getElementById('total-modal');
    const btnConfirmarVentaReal = document.getElementById('btn-confirmar-venta-real');
    const selectMesaVenta = document.getElementById('select-mesa-venta');

    // Modal PAGOS y sus elementos
    const modalPago = document.getElementById('modalPago');
    const modalTotalPago = document.getElementById('modal-total');
    const modalRecibido = document.getElementById('modal-recibido');
    const modalCambio = document.getElementById('modal-cambio');
    const detalleCarritoModal = document.getElementById('detalle-carrito-modal');
    const formConfirmarPago = document.getElementById('form-confirmar-pago');

    // Instancias de modales
    let modalPuntoVentaInstance = null;
    let modalPagoInstance = null;

    // ========== RENDERIZAR CARRITO ==========
    function renderCarrito() {
        if (!itemsCarritoEl) return;

        itemsCarritoEl.innerHTML = '';
        totalGlobal = 0;

        if (carrito.length === 0) {
            itemsCarritoEl.innerHTML = '<div class="text-center text-muted p-4">Carrito vacío</div>';
            if (totalCarritoEl) totalCarritoEl.textContent = '$0';
            localStorage.setItem('carritoPOS', JSON.stringify(carrito));
            return;
        }

        carrito.forEach((item, index) => {
            const subtotal = item.precio * item.cantidad;
            totalGlobal += subtotal;

            const div = document.createElement('div');
            div.className = 'item-carrito d-flex justify-content-between align-items-center p-2 border-bottom';
            div.innerHTML = `
                <div><strong>${item.nombre}</strong></div>
                <div class="d-flex align-items-center gap-2">
                    <button class="btn btn-sm btn-outline-secondary btn-restar" data-index="${index}">−</button>
                    <span class="fw-bold">${item.cantidad}</span>
                    <button class="btn btn-sm btn-outline-secondary btn-sumar" data-index="${index}">+</button>
                    <strong class="text-success ms-2">$ ${subtotal.toLocaleString('es-CO')}</strong>
                    <button class="btn btn-danger btn-sm ms-2 btn-eliminar" data-index="${index}">×</button>
                </div>
            `;
            itemsCarritoEl.appendChild(div);
        });

        if (totalCarritoEl) {
            totalCarritoEl.textContent = '$' + totalGlobal.toLocaleString('es-CO');
        }
        localStorage.setItem('carritoPOS', JSON.stringify(carrito));
    }

    // ========== MODIFICAR CANTIDAD ==========
    itemsCarritoEl?.addEventListener('click', e => {
        const btn = e.target;
        const index = parseInt(btn.dataset.index);
        if (isNaN(index)) return;

        if (btn.classList.contains('btn-eliminar')) {
            carrito.splice(index, 1);
        } else if (btn.classList.contains('btn-sumar')) {
            carrito[index].cantidad++;
        } else if (btn.classList.contains('btn-restar')) {
            carrito[index].cantidad--;
            if (carrito[index].cantidad <= 0) {
                carrito.splice(index, 1);
            }
        }
        renderCarrito();
    });

    // ========== VACIAR CARRITO ==========
    btnVaciarCarrito?.addEventListener('click', () => {
        if (carrito.length === 0) return;
        if (confirm('¿Vaciar el carrito?')) {
            carrito = [];
            totalGlobal = 0;
            localStorage.removeItem('carritoPOS');
            renderCarrito();
        }
    });

    // ========== AGREGAR PRODUCTO ==========
    document.querySelectorAll('.btn-agregar').forEach(btn => {
        btn.addEventListener('click', function () {
            const card = this.closest('.producto-card');
            const id = card.dataset.id;
            const nombre = card.dataset.nombre;
            const precio = parseFloat(card.dataset.precio);

            if (!id || !nombre || isNaN(precio) || precio <= 0) {
                alert('Error al agregar el producto');
                return;
            }

            const existe = carrito.find(p => p.id === id);
            if (existe) {
                existe.cantidad++;
            } else {
                carrito.push({ id, nombre, precio, cantidad: 1 });
            }
            renderCarrito();
        });
    });

    // ========== PUNTO DE VENTA: FINALIZAR VENTA → ABRIR MODAL ==========
    btnFinalizarVenta?.addEventListener('click', function (e) {
        // Evitar conflicto con otros listeners que también están enlazados al mismo botón
        // (por ejemplo, panel_caja.js). Asegura que solo este flujo de Punto de Venta se ejecute.
        try { e.preventDefault(); e.stopImmediatePropagation(); } catch (ignored) {}

        if (carrito.length === 0) {
            alert('El carrito está vacío');
            return;
        }

        totalGlobal = 0;
        carrito.forEach(item => totalGlobal += item.precio * item.cantidad);

        // Actualizar detalles del pedido
        tbodyDetalle.innerHTML = '';
        carrito.forEach(item => {
            const subtotal = item.precio * item.cantidad;
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center fw-bold">${item.cantidad}</td>
                <td>${item.nombre}</td>
                <td class="text-end">$ ${item.precio.toLocaleString('es-CO')}</td>
                <td class="text-end text-success fw-bold">$ ${subtotal.toLocaleString('es-CO')}</td>
            `;
            tbodyDetalle.appendChild(tr);
        });

        totalModal.textContent = '$' + totalGlobal.toLocaleString('es-CO');
        // Verificar que Bootstrap esté disponible y el modal exista
        if (typeof bootstrap !== 'undefined' && modalConfirmarVenta) {
            modalPuntoVentaInstance = new bootstrap.Modal(modalConfirmarVenta);
            modalPuntoVentaInstance.show();
        } else {
            alert('No se pudo abrir el modal de confirmación. Verifica la carga de Bootstrap.');
        }
    });

    // ========== ENVIAR VENTA A PENDIENTES ==========
    btnConfirmarVentaReal?.addEventListener('click', function () {
        if (carrito.length === 0) {
            alert('El carrito está vacío');
            return;
        }

        if (!confirm(`¿Enviar esta venta por $${totalGlobal.toLocaleString('es-CO')} a la lista de pendientes?`)) {
            return;
        }

        // Obtener mesa seleccionada
        const mesaId = selectMesaVenta ? selectMesaVenta.value : '';

        // Crear formulario dinámicamente
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/caja/crear-pedido-pendiente-form';
        form.style.display = 'none';

        // Campo items
        const itemsInput = document.createElement('input');
        itemsInput.type = 'hidden';
        itemsInput.name = 'items';
        itemsInput.value = JSON.stringify(carrito);

        // Campo total
        const totalInput = document.createElement('input');
        totalInput.type = 'hidden';
        totalInput.name = 'total';
        totalInput.value = totalGlobal;

        // Campo mesa (opcional)
        if (mesaId) {
            const mesaInput = document.createElement('input');
            mesaInput.type = 'hidden';
            mesaInput.name = 'mesaId';
            mesaInput.value = mesaId;
            form.appendChild(mesaInput);
        }

        // Agregar campos al formulario
        form.appendChild(itemsInput);
        form.appendChild(totalInput);

        // Mostrar loading en el botón
        const originalText = this.innerHTML;
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creando pedido...';
        this.disabled = true;

        // Agregar formulario al body y enviar después de un breve delay para mostrar el loading
        setTimeout(() => {
            document.body.appendChild(form);
            form.submit();
        }, 500);
    });

    // ========== PAGOS: CONECTAR BOTONES "COBRAR" ==========
    document.querySelectorAll('.btn-abrir-modal-pago').forEach(button => {
        button.addEventListener('click', function() {
            const pedidoId = this.getAttribute('data-pedido-id');
            const mesa = this.getAttribute('data-mesa');
            const total = parseFloat(this.getAttribute('data-total'));
            const detalle = this.getAttribute('data-detalle');
            
            abrirModalPago(pedidoId, mesa, total, detalle);
        });
    });

	// ========== PAGOS: ABRIR MODAL DE PAGO ==========
	function abrirModalPago(pedidoId, mesa, total, detalle) {
	    pedidoActual = {
	        id: pedidoId,
	        mesa: mesa,
	        total: total,
	        detalle: detalle
	    };
	    
	    console.log("💰 Abriendo modal de pago para pedido:", pedidoId);
	    
	    // Llenar datos en el modal
	    document.getElementById('modal-pedido-id').textContent = pedidoId;
	    document.getElementById('modal-mesa-info').textContent = mesa;
	    modalTotalPago.textContent = '$' + total.toLocaleString('es-CO');
	    
	    // Llenar campos del formulario
	    document.getElementById('input-pedido-id').value = pedidoId;
	    document.getElementById('input-total').value = total;
	    document.getElementById('input-carrito').value = detalle;
	    
	    // Parsear y mostrar detalles del pedido
	    mostrarDetallesPedido(detalle);
	    
	    // Resetear campos
	    modalRecibido.value = '';
	    modalCambio.value = '$0';
	    document.getElementById('modal-error').style.display = 'none';
	    document.getElementById('modal-info').style.display = 'block';
	    
	    // Mostrar modal
	    modalPagoInstance = new bootstrap.Modal(modalPago);
	    modalPagoInstance.show();
	}

	// ========== PAGOS: MANEJAR ENVÍO DEL FORMULARIO ==========
	formConfirmarPago?.addEventListener('submit', function(e) {
	    e.preventDefault();
	    
	    if (!pedidoActual) {
	        alert('Error: No hay pedido seleccionado');
	        return;
	    }
	    
	    const recibido = parseFloat(modalRecibido.value) || 0;
	    const total = pedidoActual.total;
	    
	    // Validaciones
	    if (recibido <= 0) {
	        alert('Ingrese el monto recibido');
	        return;
	    }
	    
	    if (recibido < total) {
	        alert('El monto recibido es insuficiente');
	        return;
	    }
	    
	    // Mostrar loading
	    const submitBtn = document.getElementById('btn-confirmar-pago');
	    const originalText = submitBtn.innerHTML;
	    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Procesando...';
	    submitBtn.disabled = true;
	    
	    console.log("✅ Enviando pago para pedido:", pedidoActual.id);
	    
	    // Enviar formulario
	    this.submit();
	});

    // ========== PAGOS: MOSTRAR DETALLES DEL PEDIDO ==========
    function mostrarDetallesPedido(detalleJson) {
        const contenedor = detalleCarritoModal;
        contenedor.innerHTML = '';
        
        try {
            const items = JSON.parse(detalleJson);
            let html = '';
            
            items.forEach(item => {
                const subtotal = item.precio * item.cantidad;
                html += `
                    <div class="d-flex justify-content-between border-bottom pb-2 mb-2">
                        <div>
                            <strong>${item.cantidad}x</strong> ${item.nombre}
                        </div>
                        <div>$ ${subtotal.toLocaleString('es-CO')}</div>
                    </div>
                `;
            });
            
            contenedor.innerHTML = html;
        } catch (error) {
            console.error('Error al parsear detalles:', error);
            contenedor.innerHTML = '<p class="text-muted">Error al cargar detalles</p>';
        }
    }

    // ========== PAGOS: CALCULAR CAMBIO EN TIEMPO REAL ==========
    modalRecibido?.addEventListener('input', function() {
        const total = pedidoActual ? pedidoActual.total : 0;
        const recibido = parseFloat(this.value) || 0;
        const cambio = recibido - total;
        
        if (cambio >= 0) {
            modalCambio.value = '$' + cambio.toLocaleString('es-CO');
            document.getElementById('input-monto-recibido').value = recibido;
            document.getElementById('modal-error').style.display = 'none';
            document.getElementById('modal-info').style.display = 'block';
        } else {
            modalCambio.value = '$0';
            document.getElementById('input-monto-recibido').value = '';
            document.getElementById('modal-error').style.display = 'block';
            document.getElementById('modal-info').style.display = 'none';
        }
    });

    // ========== PAGOS: MANEJAR ENVÍO DEL FORMULARIO ==========
    formConfirmarPago?.addEventListener('submit', function(e) {
        e.preventDefault();
        
        if (!pedidoActual) {
            alert('Error: No hay pedido seleccionado');
            return;
        }
        
        const recibido = parseFloat(modalRecibido.value) || 0;
        const total = pedidoActual.total;
        
        // Validaciones
        if (recibido <= 0) {
            alert('Ingrese el monto recibido');
            return;
        }
        
        if (recibido < total) {
            alert('El monto recibido es insuficiente');
            return;
        }
        
        // Mostrar loading
        const submitBtn = document.getElementById('btn-confirmar-pago');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Procesando...';
        submitBtn.disabled = true;
        
        // Enviar formulario
        this.submit();
    });

    // ========== BÚSQUEDA DE PRODUCTOS ==========
    document.getElementById('buscar-productos')?.addEventListener('input', function () {
        const texto = this.value.toLowerCase().trim();
        document.querySelectorAll('.producto-card').forEach(card => {
            const nombre = (card.dataset.nombre || '').toLowerCase();
            const desc = (card.querySelector('p')?.textContent || '').toLowerCase();
            card.style.display = (nombre.includes(texto) || desc.includes(texto)) ? 'block' : 'none';
        });
    });

    // ========== FILTRAR TABLA DE PAGOS ==========
    document.getElementById('busqueda-pagos')?.addEventListener('input', function() {
        const filtro = this.value.toLowerCase();
        const filas = document.querySelectorAll('#tabla-pagos-pendientes tbody tr');
        
        filas.forEach(fila => {
            const textoFila = fila.textContent.toLowerCase();
            if (textoFila.includes(filtro)) {
                fila.style.display = '';
            } else {
                fila.style.display = 'none';
            }
        });
    });

    // ========== INICIALIZAR ==========
    renderCarrito();
    console.log("Sistema de Punto de Venta y Pagos cargado y listo");
});




// Agrega esto al final de tu puntoVenta.js
document.addEventListener('DOMContentLoaded', function() {
    console.log("🔍 Forzando visibilidad de la tabla de pagos...");
    
    // Función para mostrar todas las filas
    function mostrarTodasLasFilasPagos() {
        const tabla = document.getElementById('tabla-pagos-pendientes');
        if (!tabla) {
            console.log("❌ No se encontró la tabla de pagos");
            return;
        }
        
        const filas = tabla.querySelectorAll('tbody tr');
        console.log(`📊 Filas encontradas en la tabla: ${filas.length}`);
        
        // Forzar mostrar todas las filas
        filas.forEach((fila, index) => {
            fila.style.display = 'table-row';
            fila.style.visibility = 'visible';
            fila.style.opacity = '1';
            fila.style.height = 'auto';
            
            // Debug: mostrar información de cada fila
            const celdas = fila.querySelectorAll('td');
            console.log(`   Fila ${index + 1}:`, {
                id: celdas[0]?.textContent,
                mesa: celdas[1]?.textContent,
                total: celdas[3]?.textContent
            });
        });
        
        // Remover cualquier estilo que oculte el tbody
        const tbody = tabla.querySelector('tbody');
        if (tbody) {
            tbody.style.display = 'table-row-group';
            tbody.style.visibility = 'visible';
            tbody.style.opacity = '1';
        }
    }
    
    // Ejecutar inmediatamente
    mostrarTodasLasFilasPagos();
    
    // Ejecutar después de que la página cargue completamente
    window.addEventListener('load', mostrarTodasLasFilasPagos);
    
    // Ejecutar después de un breve delay por si hay scripts que se ejecutan después
    setTimeout(mostrarTodasLasFilasPagos, 1000);
    setTimeout(mostrarTodasLasFilasPagos, 2000);
});
