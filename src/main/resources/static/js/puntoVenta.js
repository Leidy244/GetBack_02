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
            const idsAttr = this.getAttribute('data-pedido-ids');
            const pedidoId = this.getAttribute('data-pedido-id');
            const mesa = this.getAttribute('data-mesa');
            const total = parseFloat(this.getAttribute('data-total'));
            const detalle = this.getAttribute('data-detalle');

            if (idsAttr && idsAttr.length > 0) {
                const ids = idsAttr.split(',').map(s => s.trim()).filter(Boolean);
                abrirModalPagoGrupo(ids, mesa, total, detalle);
            } else {
                abrirModalPago(pedidoId, mesa, total, detalle);
            }
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
	    
    // Llenar campos del formulario (solo los existentes)
    const hiddenId = document.getElementById('input-pedido-id');
    if (hiddenId) hiddenId.value = String(pedidoId);
	    
    	// Parsear y mostrar detalles del pedido
    	mostrarDetallesPedido(detalle);
    	try {
    	    const parsed = JSON.parse(detalle);
    	    pedidoActual.items = Array.isArray(parsed) ? parsed.map(it => ({
    	        nombre: it.nombre || it.productoNombre || 'Item',
    	        cantidad: Number(it.cantidad || 1),
    	        precio: Number(it.precio || 0)
    	    })) : [];
    	} catch(_) { pedidoActual.items = []; }
	    
	    // Resetear campos
	    modalRecibido.value = '';
	    modalCambio.value = '$0';
	    document.getElementById('modal-error').style.display = 'none';
	    document.getElementById('modal-info').style.display = 'block';
	    
	    // Mostrar modal
	    modalPagoInstance = new bootstrap.Modal(modalPago);
	    modalPagoInstance.show();
}

    // ========== PAGOS: ABRIR MODAL DE PAGO AGRUPADO ==========
    function abrirModalPagoGrupo(pedidoIds, mesa, total, detalleJson) {
        pedidoActual = {
            ids: pedidoIds,
            mesa: mesa,
            total: total,
            detalle: null
        };

        const pedidoIdLabel = document.getElementById('modal-pedido-id');
        if (pedidoIdLabel) pedidoIdLabel.textContent = `Grupo (${pedidoIds.length})`;
        document.getElementById('modal-mesa-info').textContent = mesa;
        modalTotalPago.textContent = '$' + total.toLocaleString('es-CO');
        const idsSpan = document.getElementById('modal-ids');
        if (idsSpan) idsSpan.textContent = pedidoIds.join(', ');

        const hiddenIds = document.getElementById('input-pedido-ids');
        if (hiddenIds) hiddenIds.value = pedidoIds.join(',');
        const hiddenId = document.getElementById('input-pedido-id');
        if (hiddenId) hiddenId.value = pedidoIds[0] || '';

        const contenedor = detalleCarritoModal;
        if (detalleJson) {
            try {
                const safeJson = detalleJson.replace(/&quot;/g, '"').replace(/&amp;/g, '&');
                const d = JSON.parse(safeJson);
                const items = Array.isArray(d.items) ? d.items : [];
                pedidoActual.items = items.map(it => ({
                    nombre: it.nombre || it.productoNombre || 'Item',
                    cantidad: Number(it.cantidad || 1),
                    precio: Number(it.precio || 0)
                }));
                const comentarios = Array.isArray(d.comentarios) ? d.comentarios : [];
                let html = `
                    <div class="d-flex justify-content-between border-bottom pb-2 mb-2">
                        <div><strong>${pedidoIds.length}</strong> pedidos agrupados</div>
                        <div>IDs: ${pedidoIds.join(', ')}</div>
                    </div>
                `;
                if (items.length > 0) {
                    html += `<div class="mb-2"><strong>Productos:</strong></div>`;
                    items.forEach(it => {
                        const nom = it.nombre || '-';
                        const cant = Number(it.cantidad || 0);
                        const precio = Number(it.precio || 0);
                        const subtotal = Number(it.subtotal || (precio * cant));
                        html += `
                            <div class="d-flex justify-content-between border-bottom pb-1 mb-1">
                                <div><strong>${cant}x</strong> ${nom}</div>
                                <div>$ ${subtotal.toLocaleString('es-CO')}</div>
                            </div>
                        `;
                    });
                    html += `<div class="text-end fw-bold mt-2">Total combinado: $ ${Number(total).toLocaleString('es-CO')}</div>`;
                }
                if (comentarios.length > 0) {
                    html += `<hr><div class="mb-2"><strong>Comentarios:</strong></div>`;
                    comentarios.forEach((c, idx) => {
                        html += `<div class="text-muted">• ${String(c)}</div>`;
                    });
                }
                contenedor.innerHTML = html;
                // Si no hay items en d, intentar obtenerlos vía API
                if (!items || items.length === 0) {
                    fetch(`/caja/pagos/detalle?ids=${encodeURIComponent(pedidoIds.join(','))}`)
                        .then(r => r.ok ? r.json() : null)
                        .then(data => {
                            if (!data || !Array.isArray(data.items)) return;
                            let html2 = `
                                <div class="d-flex justify-content-between border-bottom pb-2 mb-2">
                                    <div><strong>${pedidoIds.length}</strong> pedidos agrupados</div>
                                    <div>IDs: ${pedidoIds.join(', ')}</div>
                                </div>
                                <div class="mb-2"><strong>Productos:</strong></div>
                            `;
                            data.items.forEach(it => {
                                const nom = it.nombre || '-';
                                const cant = Number(it.cantidad || 0);
                                const subtotal = Number(it.subtotal || 0);
                                html2 += `
                                    <div class="d-flex justify-content-between border-bottom pb-1 mb-1">
                                        <div><strong>${cant}x</strong> ${nom}</div>
                                        <div>$ ${subtotal.toLocaleString('es-CO')}</div>
                                    </div>
                                `;
                            });
                            html2 += `<div class="text-end fw-bold mt-2">Total combinado: $ ${Number(data.total || total).toLocaleString('es-CO')}</div>`;
                            if (Array.isArray(data.comentarios) && data.comentarios.length > 0) {
                                html2 += `<hr><div class="mb-2"><strong>Comentarios:</strong></div>`;
                                data.comentarios.forEach(c => { html2 += `<div class="text-muted">• ${String(c)}</div>`; });
                            }
                            contenedor.innerHTML = html2;
                        }).catch(() => {});
                }
            } catch (e) {
                contenedor.innerHTML = `<div class="text-muted">No se pudo cargar el detalle agrupado</div>`;
            }
        } else {
            contenedor.innerHTML = `
                <div class="d-flex justify-content-between border-bottom pb-2 mb-2">
                    <div><strong>${pedidoIds.length}</strong> pedidos agrupados</div>
                    <div>IDs: ${pedidoIds.join(', ')}</div>
                </div>
            `;
        }

        modalRecibido.value = '';
        modalCambio.value = '$0';
        document.getElementById('modal-error').style.display = 'none';
        document.getElementById('modal-info').style.display = 'block';

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
    const metodo = (document.getElementById('input-metodo-pago')?.value || 'EFECTIVO').toUpperCase();
	    
	    // Validaciones
    if (metodo === 'EFECTIVO' || metodo === 'MIXTO') {
    if (metodo === 'EFECTIVO' || metodo === 'MIXTO') {
        if (recibido <= 0) {
            alert('Ingrese el monto recibido');
            return;
        }
        
        if (recibido < total) {
            alert('El monto recibido es insuficiente');
            return;
        }
    }
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
    const metodo = (document.getElementById('input-metodo-pago')?.value || 'EFECTIVO').toUpperCase();
    if (metodo !== 'EFECTIVO' && metodo !== 'MIXTO') {
        return;
    }
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

    // ========== PAGOS: IMPRIMIR FACTURA ==========
	document.getElementById('btn-imprimir-pago')?.addEventListener('click', () => {
	    if (!pedidoActual) return;
	    const items = Array.isArray(pedidoActual.items) ? pedidoActual.items : [];
	    const ctx = {
	        fecha: new Date().toLocaleString('es-CO'),
	        mesa: document.getElementById('modal-mesa-info')?.textContent || '',
	        total: pedidoActual.total || 0,
	        items: items
	    };
	    imprimirTicketPago(ctx);
	});

	function imprimirTicketPago(ctx) {
	    // Crear ventana de impresión
	    const w = window.open('', '_blank');
	    if (!w) {
	        console.error('No se pudo abrir ventana de impresión');
	        return;
	    }
	    
	    // Construir HTML del ticket
	    const html = construirHTMLTicket(ctx);
	    
	    // Escribir en la ventana
	    w.document.open();
	    w.document.write(html);
	    w.document.close();
	}

	function construirHTMLTicket(ctx) {
	    // Generar filas de productos
	    const filas = (ctx.items || []).map(it => {
	        const nombre = String(it.nombre || 'Item').trim();
	        const cant = Number(it.cantidad || 1);
	        const precioUnitario = Number(it.precio || 0);
	        const subtotal = precioUnitario * cant;
	        
	        // Formato: cantidad y nombre juntos en una celda
	        return `<tr>
	            <td class="item-detalle">
	                <span class="cantidad">${cant}x</span> ${nombre}
	            </td>
	            <td class="item-precio text-end">$${subtotal.toLocaleString('es-CO')}</td>
	        </tr>`;
	    }).join('');
	    
	    // Obtener total formateado
	    const totalFormateado = Number(ctx.total || 0).toLocaleString('es-CO');
	    
	    // Devolver HTML completo con CSS embebido
	    return `<!doctype html>
	<html lang="es">
	<head>
	    ${getTicketStyles()}
	</head>
	<body>
	    ${getTicketHeader(ctx)}
	    ${getTicketItems(filas)}
	    ${getTicketFooter(totalFormateado)}
	    ${getTicketScripts()}
	</body>
	</html>`;
	}

	function getTicketStyles() {
	    return `
	    <meta charset="utf-8">
	    <title>Factura GET BACK</title>
	    <style>
	        /* Configuración de página para impresión térmica */
	        @page {
	            size: 58mm auto;
	            margin: 0;
	        }
	        
	        /* Estilos generales */
	        body {
	            font-family: 'Courier New', monospace;
	            width: 58mm;
	            margin: 4mm auto;
	            font-size: 20px;
	            font-weight: 700;
	            line-height: 1.2;
	        }
	        
	        /* Encabezado */
	        .header {
	            text-align: center;
	            margin-bottom: 6px;
	        }
	        
	        .titulo {
	            margin: 0 0 6px 0;
	            font-size: 22px;
	            text-align: center;
	        }
	        
	        .subtitulo {
	            color: #666;
	            font-size: 16px;
	            font-weight: 700;
	            margin-bottom: 8px;
	        }
	        
	        /* Información de mesa y fecha */
	        .info-mesa {
	            color: #666;
	            font-size: 16px;
	            font-weight: 700;
	            margin-bottom: 10px;
	        }
	        
	        /* Tabla de productos */
	        table {
	            width: 100%;
	            border-collapse: collapse;
	            margin: 8px 0;
	        }
	        
	        th, td {
	            padding: 6px 0;
	            border-bottom: 1px dashed #ccc;
	            font-size: 20px;
	            font-weight: 700;
	            vertical-align: top;
	        }
	        
	        .text-end {
	            text-align: right;
	        }
	        
	        /* Detalles del producto */
	        .item-detalle {
	            width: 70%;
	            padding-right: 5px;
	        }
	        
	        .item-precio {
	            width: 30%;
	            padding-left: 5px;
	        }
	        
	        .cantidad {
	            font-weight: 700;
	            margin-right: 8px;
	            display: inline-block;
	            min-width: 25px;
	        }
	        
	        /* Separadores */
	        hr {
	            border: none;
	            border-top: 1px dashed #ccc;
	            margin: 10px 0;
	        }
	        
	        /* Total */
	        .total {
	            text-align: right;
	            margin: 10px 0;
	            font-size: 22px;
	        }
	        
	        /* Pie de página */
	        .footer {
	            text-align: center;
	            color: #666;
	            font-size: 16px;
	            font-weight: 700;
	            margin-top: 15px;
	            padding-top: 10px;
	            border-top: 1px dashed #ccc;
	        }
	    </style>
	    `;
	}

	function getTicketHeader(ctx) {
	    return `
	    <div class="header">
	        <h1 class="titulo">GET BACK</h1>
	        <div class="subtitulo">Factura</div>
	    </div>
	    <div class="info-mesa">
	        <div>Mesa: ${ctx.mesa || '—'}</div>
	        <div>${ctx.fecha}</div>
	    </div>
	    <hr/>
	    `;
	}

	function getTicketItems(filas) {
	    return `
	    <table>
	        <thead>
	            <tr>
	                <th>Detalle</th>
	                <th class="text-end">Subtotal</th>
	            </tr>
	        </thead>
	        <tbody>
	            ${filas}
	        </tbody>
	    </table>
	    <hr/>
	    `;
	}

	function getTicketFooter(totalFormateado) {
	    return `
	    <div class="total">
	        <strong>Total: $${totalFormateado}</strong>
	    </div>
	    <div class="footer">
	        Gracias por su compra
	    </div>
	    `;
	}

	function getTicketScripts() {
	    return `
	    <script>
	        // Configurar para impresión automática
	        document.title = 'Factura GET BACK';
	        
	        window.onload = function() {
	            try {
	                // Pequeño delay para asegurar que todo esté cargado
	                setTimeout(() => {
	                    window.print();
	                }, 200);
	            } catch(e) {
	                console.error('Error al imprimir:', e);
	            }
	        };
	        
	        window.onafterprint = function() {
	            try {
	                // Cerrar ventana después de imprimir
	                setTimeout(() => {
	                    window.close();
	                }, 500);
	            } catch(e) {
	                console.error('Error al cerrar ventana:', e);
	            }
	        };
	    </script>
	    `;
	}

	// Función auxiliar para formatear moneda (opcional)
	function formatearMoneda(valor) {
	    return new Intl.NumberFormat('es-CO', {
	        minimumFractionDigits: 0,
	        maximumFractionDigits: 0
	    }).format(valor);
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

        const metodoSel = document.getElementById('metodo-pago');
        const metodo = (metodoSel ? metodoSel.value : 'EFECTIVO').toUpperCase();

        if (metodo === 'EFECTIVO' || metodo === 'MIXTO') {
            if (recibido <= 0) {
                alert('Ingrese el monto recibido');
                return;
            }
            if (recibido < total) {
                alert('El monto recibido es insuficiente');
                return;
            }
        }

        document.getElementById('input-monto-recibido').value = recibido;
        document.getElementById('input-metodo-pago').value = metodo;

        if (pedidoActual.ids) {
            const hiddenIds = document.getElementById('input-pedido-ids');
            if (hiddenIds) hiddenIds.value = pedidoActual.ids.join(',');
        }

        const submitBtn = document.getElementById('btn-confirmar-pago');
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Procesando...';
        submitBtn.disabled = true;

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



// ========== FUNCIONALIDAD PARA MOSTRAR MONTOS DEL ADMIN ==========
document.addEventListener('DOMContentLoaded', function() {
    // Agregar botones de revelar monto para los pedidos del admin
    function agregarBotonesRevelarMonto() {
        document.querySelectorAll('.monto-oculto-admin').forEach(montoElement => {
            // Verificar si ya tiene un botón
            if (!montoElement.nextElementSibling || !montoElement.nextElementSibling.classList.contains('btn-revelar-admin')) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn btn-sm btn-outline-secondary ms-1 btn-revelar-admin';
                btn.title = "Mostrar monto real";
                btn.innerHTML = '<i class="fas fa-eye"></i>';
                
                montoElement.parentNode.appendChild(btn);
            }
        });
    }

    // Manejar clic en botones de revelar monto del admin
    document.addEventListener('click', function(e) {
        if (e.target.closest('.btn-revelar-admin')) {
            const btn = e.target.closest('.btn-revelar-admin');
            const montoElement = btn.previousElementSibling;
            const montoReal = montoElement.getAttribute('data-monto-real');
            
            if (montoElement.textContent === '****') {
                // Mostrar monto real
                montoElement.textContent = '$' + parseFloat(montoReal).toLocaleString('es-CO');
                btn.innerHTML = '<i class="fas fa-eye-slash"></i>';
                btn.title = "Ocultar monto";
                
                // Ocultar después de 5 segundos
                setTimeout(() => {
                    if (montoElement.textContent !== '****') {
                        montoElement.textContent = '****';
                        btn.innerHTML = '<i class="fas fa-eye"></i>';
                        btn.title = "Mostrar monto real";
                    }
                }, 5000);
            } else {
                // Ocultar monto
                montoElement.textContent = '****';
                btn.innerHTML = '<i class="fas fa-eye"></i>';
                btn.title = "Mostrar monto real";
            }
        }
    });

    // Agregar estilos
    const style = document.createElement('style');
    style.textContent = `
        .monto-oculto-admin {
            font-family: 'Courier New', monospace;
            letter-spacing: 2px;
            background-color: #fff3cd;
            padding: 2px 6px;
            border-radius: 3px;
            border: 1px solid #ffeaa7;
        }
        .btn-revelar-admin {
            padding: 0.1rem 0.3rem;
            font-size: 0.75rem;
        }
    `;
    document.head.appendChild(style);

    // Ejecutar después de que la página cargue
    setTimeout(agregarBotonesRevelarMonto, 100);
});
