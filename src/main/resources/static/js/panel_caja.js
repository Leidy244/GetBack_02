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

        // Inicializar formulario de gastos (auto fecha)
        this.setupGastosForm();
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

        // Botón modo oscuro
        document.getElementById('darkModeToggle')?.addEventListener('click', this.toggleDarkMode.bind(this));

        // Configuración del tema desde el formulario
        const temaSelect = document.getElementById('tema');
        if (temaSelect) {
            temaSelect.addEventListener('change', (e) => this.cambiarTemaDesdeConfiguracion(e.target.value));
        }

        // Pagos de pedidos (sección Pagos)
        this.setupPagosSection();

        // Efectivo - calcular cambio en tiempo real
        document.getElementById('monto-recibido')?.addEventListener('input', this.calcularCambio.bind(this));

        // Cards de pedidos (inicio caja)
        this.setupPedidosCards();
        // Corte de caja (inicio/cierre de caja)
        this.setupCorteCaja();

        this.setupGestionPedidos();

        // Navegación condicional eliminada junto con el botón del header
    }

    // Auto fecha para gastos del día de trabajo
    setupGastosForm() {
        const hiddenFecha = document.getElementById('gastoFechaHidden');
        const fechaText = document.getElementById('fechaTrabajoText');
        if (!hiddenFecha) return;

        const hoy = new Date();
        const yyyy = hoy.getFullYear();
        const mm = String(hoy.getMonth() + 1).padStart(2, '0');
        const dd = String(hoy.getDate()).padStart(2, '0');
        hiddenFecha.value = `${yyyy}-${mm}-${dd}`;
        if (fechaText) {
            fechaText.textContent = hoy.toLocaleDateString();
        }

        const form = document.getElementById('gastosForm');
        const bloquearFormSiCerrada = async () => {
            try {
                const res = await fetch('/caja/estado', { credentials: 'same-origin' });
                const data = await res.json();
                const abierta = !!(data && data.abierta);
                if (form && !abierta) {
                    form.querySelectorAll('input, select, button').forEach(el => {
                        if (el.id !== 'btnGastosHistorial') {
                            el.disabled = true;
                        }
                    });
                }
            } catch (e) {}
        };
        bloquearFormSiCerrada();

        const btnHistorial = document.getElementById('btnGastosHistorial');
        const monthInput = document.getElementById('gastosHistorialMes');
        const applyBtn = document.getElementById('gastosHistorialAplicar');
        const tbody = document.getElementById('gastosHistorialTablaBody');
        const totalEl = document.getElementById('gastosHistorialTotal');
        const exportBtn = document.getElementById('gastosHistorialExport');

        const fmt = (v) => '$' + Number(v || 0).toFixed(2);
        const render = async () => {
            const mes = monthInput && monthInput.value ? monthInput.value : '';
            const url = '/caja/gastos/historial' + (mes ? ('?mes=' + encodeURIComponent(mes)) : '');
            try {
                const res = await fetch(url, { credentials: 'same-origin' });
                const lista = await res.json();
                if (Array.isArray(lista) && tbody) {
                    tbody.innerHTML = lista.map(x => `<tr>
                        <td>${x.fecha || ''}</td>
                        <td>${x.concepto || ''}</td>
                        <td>${x.metodo || ''}</td>
                        <td>${x.nota || ''}</td>
                        <td class="text-end">${fmt(x.monto)}</td>
                    </tr>`).join('');
                    const total = lista.reduce((a,b)=>a+Number(b.monto||0),0);
                    if (totalEl) totalEl.textContent = fmt(total);
                    window.__gastosHistorialLista__ = lista;
                }
            } catch (e) {
                if (tbody) tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No se pudo cargar el historial</td></tr>';
                if (totalEl) totalEl.textContent = fmt(0);
                window.__gastosHistorialLista__ = [];
            }
        };

        if (btnHistorial) {
            btnHistorial.addEventListener('click', () => {
                if (monthInput && !monthInput.value) {
                    const hoy = new Date();
                    hoy.setMonth(hoy.getMonth() - 1);
                    const y = hoy.getFullYear();
                    const m = String(hoy.getMonth() + 1).padStart(2, '0');
                    monthInput.value = `${y}-${m}`;
                }
                render();
                const modal = new bootstrap.Modal(document.getElementById('modalGastosHistorial'));
                modal.show();
            });
        }
        if (applyBtn) applyBtn.addEventListener('click', render);
        if (exportBtn) {
            exportBtn.addEventListener('click', () => {
                const lista = window.__gastosHistorialLista__ || [];
                const header = ['Fecha','Concepto','Metodo','Nota','Monto'];
                const rows = lista.map(x => [x.fecha||'',x.concepto||'',x.metodo||'',x.nota||'',Number(x.monto||0).toFixed(2)]);
                const csv = [header].concat(rows).map(r => r.join(',')).join('\n');
                const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                const mes = monthInput && monthInput.value ? monthInput.value : '';
                a.href = url;
                a.download = 'historial_gastos' + (mes ? ('_'+mes) : '') + '.csv';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(url);
            });
        }
    }

    // Ordenar historial de pagos (más reciente / más viejo)
    setupHistorialOrden() {
        const selectOrden = document.getElementById('historial-orden');
        const tabla = document.getElementById('tabla-historial-pagos');
        const inputBusqueda = document.getElementById('historialBusqueda');
        const pagerTop = document.getElementById('historialPagination');
        const info = document.getElementById('historialInfo');
        if (!selectOrden || !tabla) return;

        const cuerpo = tabla.querySelector('tbody');
        if (!cuerpo) return;

        const pageSize = 7;
        let rows = Array.from(cuerpo.querySelectorAll('tr[data-row]'));
        if (rows.length === 0) rows = Array.from(cuerpo.querySelectorAll('tr')).filter(r => !r.querySelector('td[colspan]'));
        let filtered = rows.slice();
        let page = 1;

        const renderPager = (pages) => {
            if (!pagerTop) return;
            pagerTop.innerHTML = '';
            const prevBtn = document.createElement('button');
            prevBtn.type = 'button'; prevBtn.className = 'btn btn-outline-secondary rounded-pill'; prevBtn.textContent = '«';
            prevBtn.disabled = page <= 1; prevBtn.onclick = () => { page = Math.max(1, page-1); render(); };
            pagerTop.appendChild(prevBtn);
            for (let i=1;i<=pages;i++){
                const b = document.createElement('button');
                b.type = 'button'; b.className = 'btn ' + (i===page?'btn-primary':'btn-outline-secondary') + ' rounded-pill';
                b.textContent = i; b.onclick = () => { page = i; render(); };
                pagerTop.appendChild(b);
            }
            const nextBtn = document.createElement('button');
            nextBtn.type = 'button'; nextBtn.className = 'btn btn-outline-secondary rounded-pill'; nextBtn.textContent = '»';
            nextBtn.disabled = page >= pages; nextBtn.onclick = () => { page = Math.min(pages, page+1); render(); };
            pagerTop.appendChild(nextBtn);
        };

        const render = () => {
            rows.forEach(r => { r.style.display = 'none'; });
            const total = filtered.length;
            const pages = Math.max(1, Math.ceil(total / pageSize));
            if (page > pages) page = pages;
            const start = (page - 1) * pageSize;
            const visible = filtered.slice(start, start + pageSize);
            visible.forEach(r => { r.style.display = ''; });
            if (info) info.textContent = `Resultados: ${total} • Página ${page} de ${pages}`;
            renderPager(pages);
        };

        const ordenar = () => {
            const filas = Array.from(cuerpo.querySelectorAll('tr[data-row]'));
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

            rows = Array.from(cuerpo.querySelectorAll('tr[data-row]'));
            filtered = rows.slice();
            page = 1;
            render();
        };

        // Orden inicial
        ordenar();
        // Paginación inicial si no hay orden
        if (!selectOrden) render();
        // Orden al cambiar el select
        selectOrden.addEventListener('change', ordenar);

        if (inputBusqueda) {
            inputBusqueda.addEventListener('input', () => {
                const termino = inputBusqueda.value.trim().toLowerCase();
                filtered = rows.filter(r => r.textContent.toLowerCase().includes(termino));
                page = 1;
                render();
            });
        }
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
        const metodoPagoSelect = document.getElementById('metodo-pago');
        const grupoReferencia = document.getElementById('grupo-referencia');
        const referenciaInput = document.getElementById('referencia-pago');
        const grupoCliente = document.getElementById('grupo-cliente');
        const grupoRecibido = document.getElementById('grupo-recibido');
        const grupoCambio = document.getElementById('grupo-cambio');
        const grupoElectronico = document.getElementById('grupo-electronico');
        const modalElectronico = document.getElementById('modal-electronico');
        const clienteSelect = document.getElementById('cliente-frecuente');
        const inputMetodoPagoHidden = document.getElementById('input-metodo-pago');
        const inputReferenciaHidden = document.getElementById('input-referencia-pago');
        const inputClienteHidden = document.getElementById('input-cliente-id');
        const inputMontoEfectivoHidden = document.getElementById('input-monto-efectivo');
        const inputMontoElectronicoHidden = document.getElementById('input-monto-electronico');
        const inputBusquedaPagos = document.getElementById('busqueda-pagos');
        const tablaPagosPendientes = document.getElementById('tabla-pagos-pendientes');

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

            // Si ya viene como texto plano legible (sin estructura JSON clara), lo mostramos tal cual
            const trimmed = String(detalleJson).trim();
            if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
                return trimmed;
            }

            try {
                const data = JSON.parse(detalleJson);

                // Soportar formatos:
                // 1) Array directo de items: [{...}]
                // 2) Objeto con propiedad items: { items: [...] }
                // 3) Objeto legacy con solo comentarios u otros campos
                let items = [];
                let comentarios = '';

                if (Array.isArray(data)) {
                    // Caso: la orden es directamente un array de items
                    items = data;
                } else if (data && typeof data === 'object') {
                    let rawItems = data.items;

                    // Caso normal: { items: [...] }
                    if (Array.isArray(rawItems)) {
                        items = rawItems;
                    }

                    // Caso anidado: { items: { items:[...], total:... } }
                    if (!items.length && rawItems && typeof rawItems === 'object' && Array.isArray(rawItems.items)) {
                        items = rawItems.items;
                    }

                    // Caso donde items viene como string JSON
                    if (!items.length && typeof rawItems === 'string') {
                        try {
                            const parsed = JSON.parse(rawItems);
                            if (Array.isArray(parsed)) {
                                items = parsed;
                            } else if (parsed && typeof parsed === 'object' && Array.isArray(parsed.items)) {
                                items = parsed.items;
                            }
                        } catch (ignored) {}
                    }

                    if (typeof data.comentarios === 'string') {
                        comentarios = data.comentarios.trim();
                    }
                }

                // Si hay items, formatearlos
                if (items.length > 0) {
                    const lineas = items.map(item => {
                        const cantidad = item.cantidad ?? item.quantity ?? 0;
                        const nombre = item.productoNombre ?? item.nombre ?? item.name ?? 'Producto';
                        const subtotal = item.subtotal ?? (item.precio ?? item.price ?? 0) * (cantidad || 1);

                        const subtotalNumber = Number(subtotal);
                        const subtotalFormateado = Number.isFinite(subtotalNumber)
                            ? subtotalNumber.toString()
                            : String(subtotal);

                        return `${cantidad}x ${nombre} - $${subtotalFormateado}`;
                    });

                    // Si también hay comentarios, agregarlos como última línea
                    if (comentarios) {
                        lineas.push(`Notas: ${comentarios}`);
                    }

                    return lineas.join('\n');
                }

                // Sin items pero con comentarios: mostrar al menos las notas
                if (comentarios) {
                    return `Notas: ${comentarios}`;
                }

                // Último recurso: devolver el JSON "bonito" para no perder información
                return JSON.stringify(data, null, 2);
            } catch (e) {
                console.error('No se pudo parsear el detalle del pedido:', e);
                return trimmed || 'Sin detalle';
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
                const mesaInfoEl = document.getElementById('modal-mesa-info');
                if (mesaInfoEl) mesaInfoEl.textContent = mesa;
                const detalleFormateado = formatearDetalle(detalle);
                const detalleEl = document.getElementById('detalle-carrito-modal');
                if (detalleEl) detalleEl.innerHTML = detalleFormateado.replace(/\n/g, '<br>');

                // Total sin .00 si es entero
                document.getElementById('modal-total').textContent = `$${formatearMonto(total)}`;

                const spanId = document.getElementById('modal-pedido-id');
                if (spanId) spanId.textContent = String(pedidoId);
                const hiddenId = document.getElementById('input-pedido-id');
                if (hiddenId) hiddenId.value = String(pedidoId);

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

            if (metodoPagoSelect) {
                metodoPagoSelect.value = 'EFECTIVO';
                actualizarUIMetodoPago();
            }
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

                const metodo = (inputMetodoPagoHidden.value || 'EFECTIVO').toUpperCase();
                if (metodo === 'MIXTO') {
                    const electronico = modalElectronico ? (parseFloat(modalElectronico.value) || 0) : 0;
                    const sum = recibido + electronico;
                    const cambio = sum - total;
                    const hiddenRecibido = document.getElementById('input-monto-recibido');
                    if (inputMontoEfectivoHidden) inputMontoEfectivoHidden.value = recibido.toFixed(2);
                    if (inputMontoElectronicoHidden) inputMontoElectronicoHidden.value = electronico.toFixed(2);
                    if (hiddenRecibido) hiddenRecibido.value = sum.toFixed(2);
                    if (sum > 0) {
                        modalInfo.style.display = 'none';
                        if (cambio >= 0) {
                            modalCambio.value = `$${formatearMonto(cambio)}`;
                            modalError.style.display = 'none';
                            btnConfirmarPago.disabled = false;
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
                    return;
                }

                if (recibido > 0) {
                    modalInfo.style.display = 'none';
                    
                    if (recibido >= total) {
                        const cambio = recibido - total;
                        modalCambio.value = `$${formatearMonto(cambio)}`;

                        modalError.style.display = 'none';
                        btnConfirmarPago.disabled = false;
                        
                        const hiddenRecibido = document.getElementById('input-monto-recibido');
                        if (hiddenRecibido) hiddenRecibido.value = recibido.toFixed(2);
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

        if (modalElectronico) {
            modalElectronico.addEventListener('input', () => {
                const metodo = (inputMetodoPagoHidden.value || 'EFECTIVO').toUpperCase();
                if (metodo === 'MIXTO') {
                    // Replicar cálculo mixto
                    const totalElement = document.getElementById('modal-total');
                    const total = parseFloat(totalElement.textContent.replace('$', '').replace(/\./g, '')) || 0;
                    const efectivo = parseFloat(modalRecibido.value) || 0;
                    const electronico = parseFloat(modalElectronico.value) || 0;
                    const sum = efectivo + electronico;
                    const cambio = sum - total;
                    const hiddenRecibido = document.getElementById('input-monto-recibido');
                    if (inputMontoEfectivoHidden) inputMontoEfectivoHidden.value = efectivo.toFixed(2);
                    if (inputMontoElectronicoHidden) inputMontoElectronicoHidden.value = electronico.toFixed(2);
                    if (hiddenRecibido) hiddenRecibido.value = sum.toFixed(2);
                    if (sum > 0) {
                        modalInfo.style.display = 'none';
                        if (cambio >= 0) {
                            modalCambio.value = `$${formatearMonto(cambio)}`;
                            modalError.style.display = 'none';
                            btnConfirmarPago.disabled = false;
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
                }
            });
        }

        function actualizarUIMetodoPago() {
            const metodo = metodoPagoSelect ? metodoPagoSelect.value : 'EFECTIVO';
            inputMetodoPagoHidden.value = metodo;
            referenciaInput.value = '';
            clienteSelect && (clienteSelect.value = '');
            inputReferenciaHidden.value = '';
            inputClienteHidden.value = '';
            btnConfirmarPago.disabled = false;
            modalInfo.style.display = 'none';
            modalError.style.display = 'none';

            if (metodo === 'EFECTIVO') {
                grupoReferencia.style.display = 'none';
                grupoCliente.style.display = 'none';
                grupoRecibido && (grupoRecibido.style.display = 'block');
                grupoCambio && (grupoCambio.style.display = 'block');
                modalRecibido.type = 'number';
                modalRecibido.readOnly = false;
                modalRecibido.required = true;
                modalRecibido.placeholder = '0';
                btnConfirmarPago.disabled = true;
                modalInfo.style.display = 'block';
            } else if (metodo === 'TRANSFERENCIA' || metodo === 'TARJETA') {
                grupoReferencia.style.display = 'block';
                grupoCliente.style.display = 'none';
                grupoRecibido && (grupoRecibido.style.display = 'none');
                grupoElectronico && (grupoElectronico.style.display = 'none');
                grupoCambio && (grupoCambio.style.display = 'none');
                modalRecibido.type = 'number';
                modalRecibido.readOnly = true;
                modalRecibido.required = false;
                modalRecibido.placeholder = '0';
                modalRecibido.value = '';
                modalCambio.value = '$0';
                const totalElement = document.getElementById('modal-total');
                const total = parseFloat(totalElement.textContent.replace('$', '').replace(/\./g, '')) || 0;
                const hiddenRecibido = document.getElementById('input-monto-recibido');
                if (hiddenRecibido) hiddenRecibido.value = total.toFixed(2);
            } else if (metodo === 'MIXTO') {
                grupoReferencia.style.display = 'block';
                grupoCliente.style.display = 'none';
                grupoRecibido && (grupoRecibido.style.display = 'block');
                grupoElectronico && (grupoElectronico.style.display = 'block');
                grupoCambio && (grupoCambio.style.display = 'block');
                modalRecibido.type = 'number';
                modalRecibido.readOnly = false;
                modalRecibido.required = true;
                modalRecibido.placeholder = '0';
                if (modalElectronico) {
                    modalElectronico.readOnly = false;
                    modalElectronico.required = true;
                    modalElectronico.placeholder = '0';
                    modalElectronico.value = '';
                }
                btnConfirmarPago.disabled = true;
                modalInfo.style.display = 'block';
            } else if (metodo === 'CLIENTE_FRECUENTE') {
                grupoReferencia.style.display = 'none';
                grupoCliente.style.display = 'block';
                grupoRecibido && (grupoRecibido.style.display = 'block');
                grupoElectronico && (grupoElectronico.style.display = 'none');
                grupoCambio && (grupoCambio.style.display = 'none');
                modalRecibido.type = 'password';
                modalRecibido.readOnly = true;
                modalRecibido.required = false;
                modalRecibido.value = '******';
                modalRecibido.placeholder = '******';
                modalCambio.value = '$0';
                const hiddenRecibido = document.getElementById('input-monto-recibido');
                if (hiddenRecibido) hiddenRecibido.value = '0';
                if (inputMontoEfectivoHidden) inputMontoEfectivoHidden.value = '0';
                if (inputMontoElectronicoHidden) inputMontoElectronicoHidden.value = '0';
                btnConfirmarPago.disabled = true;
                modalInfo.style.display = 'none';
                modalError.style.display = 'none';
            }
        }

        if (metodoPagoSelect) {
            metodoPagoSelect.addEventListener('change', actualizarUIMetodoPago);
        }

        referenciaInput && referenciaInput.addEventListener('input', () => {
            inputReferenciaHidden.value = referenciaInput.value.trim();
        });
        clienteSelect && clienteSelect.addEventListener('change', () => {
            inputClienteHidden.value = clienteSelect.value;
            if (metodoPagoSelect && metodoPagoSelect.value === 'CLIENTE_FRECUENTE') {
                const opt = clienteSelect.options[clienteSelect.selectedIndex];
                const saldoAttr = opt ? opt.getAttribute('data-saldo') : null;
                const saldo = saldoAttr ? parseFloat(saldoAttr) : 0;
                modalRecibido.type = 'password';
                modalRecibido.value = '******';
                modalRecibido.placeholder = '******';
                const hiddenRecibido = document.getElementById('input-monto-recibido');
                if (hiddenRecibido) hiddenRecibido.value = Number.isFinite(saldo) ? saldo.toFixed(2) : '0';
                btnConfirmarPago.disabled = !clienteSelect.value;
            }
        });

        if (tablaPagosPendientes) {
            const cuerpo = tablaPagosPendientes.querySelector('tbody');
            const pagerTop = document.getElementById('pagosPagination');
            const pagerBottom = document.getElementById('pagosPaginationBottom');
            const info = document.getElementById('pagosInfo');
            const pageSize = 7;
            let rows = Array.from(cuerpo ? cuerpo.querySelectorAll('tr[data-row]') : []);
            if (rows.length === 0 && cuerpo) {
                rows = Array.from(cuerpo.querySelectorAll('tr')).filter(r => !r.querySelector('td[colspan]'));
            }
            let filtered = rows.slice();
            let page = 1;

            const renderPager = (pages) => {
                const build = (target) => {
                    if (!target) return;
                    target.innerHTML = '';
                    const prevBtn = document.createElement('button');
                    prevBtn.type = 'button'; prevBtn.className = 'btn btn-outline-secondary rounded-pill'; prevBtn.textContent = '«';
                    prevBtn.disabled = page <= 1; prevBtn.onclick = () => { page = Math.max(1, page-1); render(); };
                    target.appendChild(prevBtn);
                    for (let i=1;i<=pages;i++){
                        const b = document.createElement('button');
                        b.type = 'button'; b.className = 'btn ' + (i===page?'btn-primary':'btn-outline-secondary') + ' rounded-pill';
                        b.textContent = i; b.onclick = () => { page = i; render(); };
                        target.appendChild(b);
                    }
                    const nextBtn = document.createElement('button');
                    nextBtn.type = 'button'; nextBtn.className = 'btn btn-outline-secondary rounded-pill'; nextBtn.textContent = '»';
                    nextBtn.disabled = page >= pages; nextBtn.onclick = () => { page = Math.min(pages, page+1); render(); };
                    target.appendChild(nextBtn);
                };
                build(pagerTop);
                build(pagerBottom);
            };

            const render = () => {
                rows.forEach(r => { r.style.display = 'none'; });
                const total = filtered.length;
                const pages = Math.max(1, Math.ceil(total / pageSize));
                if (page > pages) page = pages;
                const start = (page - 1) * pageSize;
                const visible = filtered.slice(start, start + pageSize);
                visible.forEach(r => { r.style.display = ''; });
                if (info) info.textContent = `Resultados: ${total} • Página ${page} de ${pages}`;
                renderPager(pages);
            };

            render();

            if (inputBusquedaPagos) {
                inputBusquedaPagos.addEventListener('input', () => {
                    const termino = inputBusquedaPagos.value.trim().toLowerCase();
                    filtered = rows.filter(r => r.textContent.toLowerCase().includes(termino));
                    page = 1;
                    render();
                });
            }
        }
    }

    // Cards de pedidos (colapsables en inicio-caja)
    setupPedidosCards() {
        const toggles = document.querySelectorAll('.pedido-card .pedido-toggle, .pedido-card .pedido-toggle-btn');
        if (!toggles || toggles.length === 0) return;

        toggles.forEach(toggle => {
            toggle.addEventListener('click', (e) => {
                const card = e.currentTarget.closest('.pedido-card');
                if (!card) return;
                card.classList.toggle('collapsed');
            });
        });
    }

    setupGestionPedidos() {
        const setupList = (selector, inputId, pagerId, infoId) => {
            const cont = document.querySelector(selector);
            if (!cont) return;
            const items = Array.from(cont.querySelectorAll('.pedido-card[data-row]'));
            const input = document.getElementById(inputId);
            const pager = document.getElementById(pagerId);
            const info = document.getElementById(infoId);
            const pageSize = pager ? 7 : Infinity;
            let rows = items.slice();
            let filtered = rows.slice();
            let page = 1;

            const renderPager = (pages) => {
                if (!pager) return;
                pager.innerHTML = '';
                const prevBtn = document.createElement('button');
                prevBtn.type = 'button'; prevBtn.className = 'btn btn-outline-secondary rounded-pill'; prevBtn.textContent = '«';
                prevBtn.disabled = page <= 1; prevBtn.onclick = () => { page = Math.max(1, page-1); render(); };
                pager.appendChild(prevBtn);
                for (let i=1;i<=pages;i++){
                    const b = document.createElement('button');
                    b.type = 'button'; b.className = 'btn ' + (i===page?'btn-primary':'btn-outline-secondary') + ' rounded-pill';
                    b.textContent = i; b.onclick = () => { page = i; render(); };
                    pager.appendChild(b);
                }
                const nextBtn = document.createElement('button');
                nextBtn.type = 'button'; nextBtn.className = 'btn btn-outline-secondary rounded-pill'; nextBtn.textContent = '»';
                nextBtn.disabled = page >= pages; nextBtn.onclick = () => { page = Math.min(pages, page+1); render(); };
                pager.appendChild(nextBtn);
            };

            const render = () => {
                const total = filtered.length;
                const pages = Math.max(1, Math.ceil(total / pageSize));
                if (page > pages) page = pages;
                const start = (page - 1) * pageSize;
                const visible = filtered.slice(start, start + pageSize);

                if (pager) {
                    rows.forEach(r => { r.style.display = 'none'; });
                    visible.forEach(r => { r.style.display = ''; });
                } else {
                    rows.forEach(r => { r.style.display = filtered.includes(r) ? '' : 'none'; });
                }

                if (info) info.textContent = pager ? `Resultados: ${total} • Página ${page} de ${pages}` : `Resultados: ${total}`;
                renderPager(pages);
            };

            render();
            if (input) {
                input.addEventListener('input', () => {
                    const t = input.value.trim().toLowerCase();
                    filtered = rows.filter(r => r.textContent.toLowerCase().includes(t));
                    page = 1;
                    render();
                });
            }
        };

        setupList('#pedidosPendientesGrid', 'pedidosPendientesBusqueda', 'pedidosPendientesPagination', 'pedidosPendientesInfo');
        setupList('#pedidosCompletadosGrid', 'pedidosPagadosBusqueda', null, 'pedidosPagadosInfo');
        }

    // Inicio / Cierre de caja: cálculo de base y retiro estimado
    setupCorteCaja() {
        const corteContainer = document.querySelector('.form-container.mt-4[data-ingresos-dia]');
        if (!corteContainer) return;

        const ingresosAttr = corteContainer.getAttribute('data-ingresos-dia');
        const efectivoGeneradoDia = parseFloat(ingresosAttr) || 0; // ingresos EFECTIVO - gastos EFECTIVO
        let baseApertura = 0;

        const inputBase = document.getElementById('baseSiguienteDia');
        const inputRetiro = document.getElementById('retiroEstimado');
        const btnCalcular = document.getElementById('btnCalcularCorte');
        const btnAbrir = document.getElementById('btnAbrirCaja');
        const btnCerrar = document.getElementById('btnCerrarCaja');
        const estadoTexto = document.getElementById('estadoCajaTexto');

        if (!inputBase || !inputRetiro || !btnCalcular) return;

        const calcular = () => {
            const baseSiguiente = parseFloat(inputBase.value) || 0;
            const efectivoEnCaja = baseApertura + efectivoGeneradoDia;
            let retiro = efectivoEnCaja - baseSiguiente;
            if (retiro < 0) retiro = 0;
            inputRetiro.value = retiro.toFixed(2);
        };

        btnCalcular.addEventListener('click', calcular);
        inputBase.addEventListener('input', calcular);

        // ===== Estado de caja (front-end) =====
        const getEstadoCajaLocal = () => {
            try {
                const raw = localStorage.getItem('caja-estado');
                return raw ? JSON.parse(raw) : null;
            } catch (e) {
                return null;
            }
        };

        const fetchEstadoCaja = async () => {
            try {
                const res = await fetch('/caja/estado', { credentials: 'same-origin' });
                if (!res.ok) throw new Error('Estado no disponible');
                const data = await res.json();
                if (data) localStorage.setItem('caja-estado', JSON.stringify(data));
                if (data && typeof data.base === 'number') {
                    baseApertura = data.base;
                } else {
                    const local = getEstadoCajaLocal();
                    baseApertura = local && typeof local.base === 'number' ? local.base : 0;
                }
                return data;
            } catch (e) {
                const local = getEstadoCajaLocal();
                baseApertura = local && typeof local.base === 'number' ? local.base : 0;
                return local;
            }
        };

        const setEstadoCaja = (data) => {
            localStorage.setItem('caja-estado', JSON.stringify(data));
            actualizarBadgeEstado();
            actualizarUIEstado();
        };

        const actualizarBadgeEstado = () => {
            const badge = document.querySelector('.caja-status-badge');
            const icon = badge?.querySelector('.status-icon');
            const estado = getEstadoCajaLocal();
            const abierta = !!(estado && estado.abierta);
            if (badge) {
                badge.classList.toggle('open', abierta);
                badge.classList.toggle('closed', !abierta);
                const span = badge.querySelector('span');
                if (span) span.textContent = abierta ? 'Caja abierta' : 'Caja cerrada';
                if (icon) {
                    icon.classList.toggle('fa-check-circle', abierta);
                    icon.classList.toggle('fa-lock', !abierta);
                }
            }
        };

        const actualizarUIEstado = () => {
            const estado = getEstadoCajaLocal();
            const abierta = !!(estado && estado.abierta);
            if (estadoTexto) {
                estadoTexto.textContent = abierta
                    ? `Estado: abierta desde ${new Date(estado.fechaApertura).toLocaleString()} (base $${Number(estado.base || 0).toFixed(2)})`
                    : 'Estado: cerrada';
            }
            if (btnAbrir && btnCerrar) {
                btnAbrir.disabled = abierta;
                btnCerrar.disabled = !abierta;
            }
            // Sin botón en header: solo badge
            const diaLabel = document.getElementById('diaCorteLabel');
            if (diaLabel) {
                if (abierta && estado.fechaApertura) {
                    diaLabel.textContent = new Date(estado.fechaApertura).toLocaleDateString('es-ES');
                } else if (!abierta && estado && estado.fechaCierre) {
                    diaLabel.textContent = new Date(estado.fechaCierre).toLocaleDateString('es-ES');
                }
            }
        };

        const abrirCaja = async () => {
            const base = parseFloat(inputBase.value) || 0;
            try {
                const res = await fetch('/caja/abrir?base=' + encodeURIComponent(base), { method: 'POST', credentials: 'same-origin' });
                const data = await res.json();
                if (data && data.estado) {
                    setEstadoCaja(data.estado);
                    return;
                }
            } catch (e) {}
            // Fallback local
            const estado = { abierta: true, fechaApertura: new Date().toISOString(), base };
            setEstadoCaja(estado);
        };

        const cerrarCaja = async () => {
            const retiro = parseFloat(inputRetiro.value) || 0;
            const baseSig = parseFloat(inputBase.value) || 0;
            // Limpiar tabla de "Últimos gastos" en UI al cerrar
            const tableBody = document.querySelector('.gastos-table tbody');
            if (tableBody) tableBody.innerHTML = '';
            try {
                const res = await fetch('/caja/cerrar?retiro=' + encodeURIComponent(retiro) + '&baseSiguiente=' + encodeURIComponent(baseSig), { method: 'POST', credentials: 'same-origin' });
                const data = await res.json();
                if (data && data.estado) {
                    setEstadoCaja(data.estado);
                    resetTarjetasDia();
                    return;
                }
            } catch (e) {}
            // Fallback local
            const estado = { abierta: false, fechaCierre: new Date().toISOString(), retiro, baseSiguiente: baseSig };
            setEstadoCaja(estado);
            resetTarjetasDia();
        };

        btnAbrir && btnAbrir.addEventListener('click', abrirCaja);
        btnCerrar && btnCerrar.addEventListener('click', cerrarCaja);

        // Inicializar UI
        calcular();
        fetchEstadoCaja().then(() => {
            actualizarBadgeEstado();
            actualizarUIEstado();
            const estado = (function(){ try { const raw = localStorage.getItem('caja-estado'); return raw ? JSON.parse(raw) : null; } catch(e){ return null; } })();
            if (!estado || !estado.abierta) {
                resetTarjetasDia();
            }
        });

        const resetTarjetasDia = () => {
            const ingresosEl = document.getElementById('ingresosEfectivoLabel');
            const gastosEl = document.getElementById('gastosEfectivoLabel');
            const ventasEl = document.getElementById('ventasDiaLabel');
            ingresosEl && (ingresosEl.textContent = '$0.00');
            gastosEl && (gastosEl.textContent = '$0.00');
            ventasEl && (ventasEl.textContent = '0 ventas pagadas');
            const container = document.querySelector('.form-container.mt-4');
            if (container) container.setAttribute('data-ingresos-dia', '0');
            calcular();
        };

        // Historial modal
        const modal = document.getElementById('cajaHistorialModal');
        const applyBtn = document.getElementById('historialAplicar');
        const monthInput = document.getElementById('historialMes');
        const tbody = document.getElementById('historialTablaBody');
        const loadHistorial = async () => {
            const mes = monthInput && monthInput.value ? monthInput.value : '';
            const url = '/caja/historial' + (mes ? ('?mes=' + encodeURIComponent(mes)) : '');
            try {
                const res = await fetch(url, { credentials: 'same-origin' });
                const lista = await res.json();
                if (Array.isArray(lista) && tbody) {
                    tbody.innerHTML = lista.map(item => {
                        const fecha = item.fecha;
                        const ingresos = (item.ingresosEfectivo || 0).toFixed(2);
                        const gastos = (item.gastosEfectivo || 0).toFixed(2);
                        const ventas = item.ventasDia || 0;
                        const baseApertura = (item.baseApertura || 0).toFixed(2);
                        const baseSiguiente = (item.baseSiguiente || 0).toFixed(2);
                        const retiro = (item.retiro || 0).toFixed(2);
                        return `<tr>
                            <td>${fecha}</td>
                            <td>$${ingresos}</td>
                            <td>$${gastos}</td>
                            <td>${ventas}</td>
                            <td>$${baseApertura}</td>
                            <td>$${baseSiguiente}</td>
                            <td>$${retiro}</td>
                        </tr>`;
                    }).join('');

                    // Totales
                    const totals = lista.reduce((acc, it) => {
                        acc.ingresos += (it.ingresosEfectivo || 0);
                        acc.gastos += (it.gastosEfectivo || 0);
                        acc.ventas += (it.ventasDia || 0);
                        acc.baseApertura += (it.baseApertura || 0);
                        acc.baseSiguiente += (it.baseSiguiente || 0);
                        acc.retiro += (it.retiro || 0);
                        return acc;
                    }, { ingresos:0, gastos:0, ventas:0, baseApertura:0, baseSiguiente:0, retiro:0 });
                    const fmt = v => '$' + (v || 0).toFixed(2);
                    const setText = (id, text) => { const el = document.getElementById(id); if (el) el.textContent = text; };
                    setText('histTotalIngresos', fmt(totals.ingresos));
                    setText('histTotalGastos', fmt(totals.gastos));
                    setText('histTotalVentas', String(totals.ventas));
                    setText('histTotalBaseApertura', fmt(totals.baseApertura));
                    setText('histTotalBaseSiguiente', fmt(totals.baseSiguiente));
                    setText('histTotalRetiro', fmt(totals.retiro));
                    // Guardar última lista para exportación
                    window.__historialLista__ = lista;
                }
            } catch (e) {
                if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No se pudo cargar el historial</td></tr>';
            }
        };
        if (modal) {
            modal.addEventListener('shown.bs.modal', loadHistorial);
        }
        applyBtn && applyBtn.addEventListener('click', loadHistorial);

        // Exportar CSV
        const exportBtn = document.getElementById('historialExport');
        const exportCSV = () => {
            const lista = window.__historialLista__ || [];
            const header = ['Fecha','IngresosEF','GastosEF','Ventas','BaseApertura','BaseSiguiente','Retiro'];
            const rows = lista.map(it => [
                it.fecha,
                (it.ingresosEfectivo || 0).toFixed(2),
                (it.gastosEfectivo || 0).toFixed(2),
                String(it.ventasDia || 0),
                (it.baseApertura || 0).toFixed(2),
                (it.baseSiguiente || 0).toFixed(2),
                (it.retiro || 0).toFixed(2),
            ]);
            const csv = [header].concat(rows).map(r => r.join(',')).join('\n');
            const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            const mes = monthInput && monthInput.value ? monthInput.value : '';
            a.href = url;
            a.download = 'historial_cierres' + (mes ? ('_' + mes) : '') + '.csv';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        };
        exportBtn && exportBtn.addEventListener('click', exportCSV);
    }

    /* ===== FUNCIONALIDADES MODO OSCURO ===== */
    toggleDarkMode() {
        const root = document.documentElement;
        const currentTheme = root.getAttribute("data-theme");
        const icon = document.querySelector("#darkModeToggle i");
        
        if (currentTheme === "dark") {
            root.removeAttribute("data-theme");
            if (icon) {
                icon.className = "fas fa-moon";
                icon.title = "Activar modo oscuro";
            }
            localStorage.setItem("caja-theme", "light");
            this.actualizarSelectTema('claro');
        } else {
            root.setAttribute("data-theme", "dark");
            if (icon) {
                icon.className = "fas fa-sun";
                icon.title = "Activar modo claro";
            }
            localStorage.setItem("caja-theme", "dark");
            this.actualizarSelectTema('oscuro');
        }
        
        // Forzar actualización de estilos
        this.forceStyleUpdate();
    }

    cambiarTemaDesdeConfiguracion(tema) {
        const root = document.documentElement;
        const icon = document.querySelector("#darkModeToggle i");
        
        if (tema === "oscuro") {
            root.setAttribute("data-theme", "dark");
            if (icon) {
                icon.className = "fas fa-sun";
                icon.title = "Activar modo claro";
            }
            localStorage.setItem("caja-theme", "dark");
        } else {
            root.removeAttribute("data-theme");
            if (icon) {
                icon.className = "fas fa-moon";
                icon.title = "Activar modo oscuro";
            }
            localStorage.setItem("caja-theme", "light");
        }
        
        this.forceStyleUpdate();
    }

    actualizarSelectTema(tema) {
        const temaSelect = document.getElementById('tema');
        if (temaSelect) {
            temaSelect.value = tema;
        }
    }

    restoreTheme() {
        const savedTheme = localStorage.getItem("caja-theme");
        const icon = document.querySelector("#darkModeToggle i");
        
        if (savedTheme === "dark") {
            document.documentElement.setAttribute("data-theme", "dark");
            if (icon) {
                icon.className = "fas fa-sun";
                icon.title = "Activar modo claro";
            }
            this.actualizarSelectTema('oscuro');
        } else {
            document.documentElement.removeAttribute("data-theme");
            if (icon) {
                icon.className = "fas fa-moon";
                icon.title = "Activar modo oscuro";
            }
            this.actualizarSelectTema('claro');
        }
        
        // Asegurar que los estilos se apliquen correctamente
        setTimeout(() => this.forceStyleUpdate(), 100);
    }

    forceStyleUpdate() {
        // Forzar repaint para asegurar que los estilos se apliquen
        document.body.style.display = 'none';
        document.body.offsetHeight; // Trigger reflow
        document.body.style.display = '';
    }

    showNotification(message, type = "info") {
        try {
            const raw = localStorage.getItem("caja-config");
            if (raw) {
                const cfg = JSON.parse(raw);
                if (cfg && cfg.notificaciones === false) {
                    return;
                }
            }
        } catch (e) {
            console.error("No se pudo leer configuración de notificaciones de caja", e);
        }

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
                const targetSection = document.getElementById(sectionId);
                if (targetSection) {
                    targetSection.classList.add('visible');
                }
            });
        });

        // Activar la primera sección por defecto
        const firstLink = document.querySelector('.caja-nav-link');
        if (firstLink) {
            firstLink.click();
        }
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
        this.showNotification(`${productoNombre} agregado al carrito`, 'success');
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
            } else if (accion === 'decrement' && item.cantidad === 1) {
                // Si la cantidad es 1 y se presiona decrement, eliminar el producto
                this.carrito = this.carrito.filter(i => i.id !== itemId);
            }
            
            this.actualizarCarrito();
        }
    }

    eliminarDelCarrito(event) {
        const boton = event.currentTarget;
        const itemElement = boton.closest('.item-carrito');
        const itemId = itemElement.getAttribute('data-id');
        const itemNombre = this.carrito.find(item => item.id === itemId)?.nombre;
        
        this.carrito = this.carrito.filter(item => item.id !== itemId);
        this.actualizarCarrito();
        
        if (itemNombre) {
            this.showNotification(`${itemNombre} eliminado del carrito`, 'success');
        }
    }

    vaciarCarrito() {
        if (this.carrito.length === 0) {
            this.showNotification("El carrito ya está vacío", "info");
            return;
        }

        if (confirm('¿Estás seguro de que quieres vaciar el carrito?')) {
            this.carrito = [];
            this.actualizarCarrito();
            this.showNotification("Carrito vaciado", "success");
        }
    }

    finalizarVenta() {
        if (this.carrito.length === 0) {
            this.showNotification("El carrito está vacío", "error");
            return;
        }

        if (this.metodoPago === 'efectivo') {
            const montoRecibido = parseFloat(document.getElementById('monto-recibido')?.value);
            if (isNaN(montoRecibido) || montoRecibido < this.total) {
                this.showNotification("El monto recibido debe ser igual o mayor al total", "error");
                return;
            }
        }

        // Simular proceso de venta
        this.showNotification(`Venta realizada exitosamente! Total: $${this.total.toFixed(2)}`, "success");
        this.carrito = [];
        this.actualizarCarrito();
        
        const montoRecibidoInput = document.getElementById('monto-recibido');
        if (montoRecibidoInput) montoRecibidoInput.value = '';
        
        this.calcularCambio();
    }

    imprimirTicket() {
        if (this.carrito.length === 0) {
            this.showNotification("No hay productos en el carrito para imprimir", "error");
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
        
        this.showNotification("Ticket generado para impresión", "success");
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
        const termino = document.getElementById('buscar-productos')?.value.toLowerCase();
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
        
        const targetContent = document.getElementById(`pago-${metodo}`);
        if (targetContent) {
            targetContent.classList.add('visible');
        }
        
        this.metodoPago = metodo;
    }

    calcularCambio() {
        const montoRecibidoInput = document.getElementById('monto-recibido');
        const cambioElement = document.getElementById('cambio');
        
        if (!montoRecibidoInput || !cambioElement) return;
        
        const montoRecibido = parseFloat(montoRecibidoInput.value);
        
        if (!isNaN(montoRecibido) && montoRecibido >= this.total) {
            const cambio = montoRecibido - this.total;
            cambioElement.value = `$${cambio.toFixed(2)}`;
        } else {
            cambioElement.value = '$0.00';
        }
    }

    iniciarCaja(event) {
        event.preventDefault();
        const efectivoInicial = parseFloat(document.getElementById('efectivo-inicial')?.value);
        
        if (isNaN(efectivoInicial) || efectivoInicial < 0) {
            this.showNotification("Por favor ingresa un monto válido para el efectivo inicial", "error");
            return;
        }
        
        // Simular inicio de caja
        this.showNotification(`Caja iniciada exitosamente. Efectivo inicial: $${efectivoInicial.toFixed(2)}`, "success");
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
