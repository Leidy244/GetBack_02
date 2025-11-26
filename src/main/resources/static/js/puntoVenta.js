document.addEventListener('DOMContentLoaded', () => {
	const carritoContainer = document.getElementById('items-carrito');
	const totalCarrito = document.getElementById('total-carrito');
	const btnVaciar = document.getElementById('btn-vaciar-carrito');

	let carrito = [];

	// Renderizar el carrito
	function renderCarrito() {
		carritoContainer.innerHTML = '';
		carrito.forEach((item, index) => {
			const div = document.createElement('div');
			div.classList.add('item-carrito');
			div.innerHTML = `
                <div class="info-item">
                    <strong>${item.nombre}</strong>
                    <div class="cantidad-controles">
                        <button class="btn btn-sm btn-light btn-restar" data-index="${index}">−</button>
                        <span class="cantidad">${item.cantidad}</span>
                        <button class="btn btn-sm btn-light btn-sumar" data-index="${index}">+</button>
                    </div>
                    <span>${(item.precio * item.cantidad).toLocaleString('es-CO', { style: 'currency', currency: 'COP' })}</span>
                </div>
                <button class="btn btn-sm btn-danger btn-eliminar" data-index="${index}">
                    <i class="fas fa-trash-alt"></i>
                </button>
            `;
			carritoContainer.appendChild(div);
		});

		actualizarTotal();
	}

	//  Total del carrito
	function actualizarTotal() {
		const total = carrito.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
		totalCarrito.textContent = total.toLocaleString('es-CO', { style: 'currency', currency: 'COP' });
	}

	// Agregar productos
	document.addEventListener('click', (e) => {
		if (e.target.closest('.btn-agregar')) {
			const card = e.target.closest('.producto-card');
			const id = card.dataset.id;
			const nombre = card.dataset.nombre;
			const precio = parseFloat(card.dataset.precio);

			const existente = carrito.find(item => item.id === id);
			if (existente) {
				existente.cantidad++;
			} else {
				carrito.push({ id, nombre, precio, cantidad: 1 });
			}
		}
		renderCarrito();
	});

	//dentro del carrito
	carritoContainer.addEventListener('click', (e) => {
		const index = e.target.dataset.index;
		if (e.target.classList.contains('btn-sumar')) {
			carrito[index].cantidad++;
		} else if (e.target.classList.contains('btn-restar')) {
			carrito[index].cantidad--;
			if (carrito[index].cantidad <= 0) carrito.splice(index, 1);
		} else if (e.target.classList.contains('btn-eliminar')) {
			carrito.splice(index, 1);
		}
		renderCarrito();
	});

	// Vaciar carrito
	btnVaciar.addEventListener('click', () => {
		carrito = [];
		renderCarrito();
	});
});
// --- FILTRO DE BÚSQUEDA ---
const inputBusqueda = document.getElementById('buscar-productos');
const btnBuscar = document.getElementById('btn-buscar');

function filtrarProductos() {
	const texto = inputBusqueda.value.toLowerCase().trim();
	const productos = document.querySelectorAll('.producto-card');

	productos.forEach(card => {
		const nombre = card.dataset.nombre.toLowerCase();
		const descripcion = card.querySelector('p').textContent.toLowerCase();

		if (nombre.includes(texto) || descripcion.includes(texto)) {
			card.style.display = 'block';
		} else {
			card.style.display = 'none';
		}
	});
}

// Filtra mientras escribe
inputBusqueda.addEventListener('input', filtrarProductos);

//  O al hacer clic en el botón de buscar
btnBuscar.addEventListener('click', filtrarProductos);


//Modal de metodos de pago
(function() {
	function run() {
		const btnFinalizar = document.getElementById('btn-finalizar-venta');
		const modalEl = document.getElementById('metodosPagoModal');

		if (!btnFinalizar || !modalEl || !window.bootstrap) return;

		btnFinalizar.addEventListener('click', function() {
			const modal = new bootstrap.Modal(modalEl);
			modal.show();
		});

		// Opcional: manejar selección de método y cliente
		modalEl.addEventListener('click', function(e) {
			const metodoBtn = e.target.closest('[data-metodo]');
			if (metodoBtn) {
				// aquí podrías guardar el método seleccionado en una variable global o input oculto
				console.log('Método seleccionado:', metodoBtn.getAttribute('data-metodo'));
			}

			const clienteBtn = e.target.closest('[data-cliente-id]');
			if (clienteBtn) {
				console.log('Cliente seleccionado:', clienteBtn.getAttribute('data-cliente-id'));
			}
		});
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', run);
	} else {
		run();
	}
})();

//descontar al saldo abonado al admin 
    (function () {
        function run() {
            const totalEl = document.getElementById('total-carrito');
            const modalEl = document.getElementById('metodosPagoModal');

            if (!totalEl || !modalEl) return;

            // Escuchar clicks en los botones "Pagar" dentro del modal
            modalEl.addEventListener('click', function (e) {
                const btn = e.target.closest('.btn-pagar-con-abono');
                if (!btn) return;

                const clienteId = btn.getAttribute('data-cliente-id');
                const clienteNombre = btn.getAttribute('data-cliente-nombre') || '';

                // Obtener total de la venta desde el texto "$123.45"
                let totalTexto = totalEl.textContent || totalEl.innerText || '0';
                totalTexto = totalTexto.replace('$', '').replace(/\s/g, '').replace(/\./g, '').replace(',', '.');
                const monto = parseFloat(totalTexto);

                if (isNaN(monto) || monto <= 0) {
                    alert('El total de la venta debe ser mayor que 0 para poder pagar con abono.');
                    return;
                }

                if (!clienteId) {
                    alert('No se encontró el cliente.');
                    return;
                }

                const ok = confirm(
                    '¿Deseas descontar $' + monto.toFixed(2) +
                    ' del saldo de "' + clienteNombre + '"?'
                );
                if (!ok) return;

                // Crear y enviar formulario POST a /admin/clientes/consumo
				const form = document.createElement('form');
				form.method = 'post';
				form.action = '/admin/clientes/consumo';

				const inputId = document.createElement('input');
				inputId.type = 'hidden';
				inputId.name = 'clienteId';
				inputId.value = clienteId;
				form.appendChild(inputId);

				const inputMonto = document.createElement('input');
				inputMonto.type = 'hidden';
				inputMonto.name = 'monto';
				inputMonto.value = monto.toFixed(2);
				form.appendChild(inputMonto);

				const inputDesc = document.createElement('input');
				inputDesc.type = 'hidden';
				inputDesc.name = 'descripcion';
				inputDesc.value = 'Consumo desde caja - Punto de venta';
				form.appendChild(inputDesc);

				// >>> NUEVO: indicar que viene desde caja
				const inputFromCaja = document.createElement('input');
				inputFromCaja.type = 'hidden';
				inputFromCaja.name = 'fromCaja';
				inputFromCaja.value = 'true';
				form.appendChild(inputFromCaja);

				document.body.appendChild(form);
				form.submit();
            });
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', run);
        } else {
            run();
        }
    })();
	
	//filtrar por nombre de cliente 
	(function () {
	        function run() {
	            const inputBuscar = document.getElementById('buscar-cliente-frecuente-pos');
	            const tbody = document.getElementById('clientesFrecuentesTableBody');
	            if (!inputBuscar || !tbody) return;

	            const filas = Array.from(tbody.querySelectorAll('tr[data-row]'));

	            function filtrar() {
	                const q = (inputBuscar.value || '').toLowerCase().trim();
	                filas.forEach(tr => {
	                    const nombreTd = tr.querySelector('td');
	                    const nombre = nombreTd ? (nombreTd.textContent || '').toLowerCase() : '';
	                    tr.style.display = (!q || nombre.includes(q)) ? '' : 'none';
	                });
	            }

	            inputBuscar.addEventListener('input', filtrar);
	        }

	        if (document.readyState === 'loading') {
	            document.addEventListener('DOMContentLoaded', run);
	        } else {
	            run();
	        }
	    })();
		document.addEventListener('DOMContentLoaded', function () {
		    const btnEfectivo = document.querySelector('#metodosPagoModal [data-metodo="EFECTIVO"]');
		    const metodosPagoModalEl = document.getElementById('metodosPagoModal');
		    const modalPagoEl = document.getElementById('modalPago');

		    if (!btnEfectivo || !metodosPagoModalEl || !modalPagoEl) return;

		    // Instancias Bootstrap 5
		    const metodosPagoModal = bootstrap.Modal.getOrCreateInstance(metodosPagoModalEl);
		    const modalPago = bootstrap.Modal.getOrCreateInstance(modalPagoEl);

		    const totalCarritoEl = document.getElementById('total-carrito');
		    const modalTotalEl = document.getElementById('modal-total');
		    const modalDetalleEl = document.getElementById('modal-detalle');

		    btnEfectivo.addEventListener('click', function () {
		        // 1) Cerrar modal de métodos de pago
		        metodosPagoModal.hide();

		        // 2) Rellenar datos básicos en el modal de pago
		        const totalTexto = totalCarritoEl ? totalCarritoEl.textContent.trim() : '$0.00';
		        if (modalTotalEl) modalTotalEl.textContent = totalTexto;

		        // Detalle del pedido (puedes ajustar esto a tu estructura real)
		        if (modalDetalleEl) {
		            modalDetalleEl.textContent = 'Consumo bar / restaurante';
		        }

		        // 3) Limpiar campos de efectivo/cambio
		        const recibidoInput = document.getElementById('modal-recibido');
		        const cambioInput = document.getElementById('modal-cambio');
		        const infoAlert = document.getElementById('modal-info');
		        const errorAlert = document.getElementById('modal-error');
		        const hiddenMontoRecibido = document.getElementById('modal-monto-recibido-hidden');
		        const btnConfirmar = document.getElementById('btn-confirmar-pago');

		        if (recibidoInput) recibidoInput.value = '';
		        if (cambioInput) cambioInput.value = '$0';
		        if (infoAlert) infoAlert.style.display = 'block';
		        if (errorAlert) errorAlert.style.display = 'none';
		        if (hiddenMontoRecibido) hiddenMontoRecibido.value = '';
		        if (btnConfirmar) btnConfirmar.disabled = true;

		        // 4) Mostrar modal de pago
		        modalPago.show();
		    });

		    // Cálculo de cambio en tiempo real
		    const recibidoInput = document.getElementById('modal-recibido');
		    const cambioInput = document.getElementById('modal-cambio');
		    const infoAlert = document.getElementById('modal-info');
		    const errorAlert = document.getElementById('modal-error');
		    const hiddenMontoRecibido = document.getElementById('modal-monto-recibido-hidden');
		    const btnConfirmar = document.getElementById('btn-confirmar-pago');

		    if (recibidoInput && cambioInput && modalTotalEl) {
		        recibidoInput.addEventListener('input', function () {
		            const totalStr = modalTotalEl.textContent.replace('$','').replace('.','').replace(',','.');
		            const total = parseFloat(totalStr) || 0;
		            const recibido = parseFloat(recibidoInput.value) || 0;

		            if (!recibidoInput.value) {
		                cambioInput.value = '$0';
		                if (infoAlert) infoAlert.style.display = 'block';
		                if (errorAlert) errorAlert.style.display = 'none';
		                if (btnConfirmar) btnConfirmar.disabled = true;
		                if (hiddenMontoRecibido) hiddenMontoRecibido.value = '';
		                return;
		            }

		            if (recibido < total) {
		                cambioInput.value = '$0';
		                if (infoAlert) infoAlert.style.display = 'none';
		                if (errorAlert) errorAlert.style.display = 'block';
		                if (btnConfirmar) btnConfirmar.disabled = true;
		                if (hiddenMontoRecibido) hiddenMontoRecibido.value = '';
		            } else {
		                const cambio = recibido - total;
		                cambioInput.value = '$' + cambio.toFixed(2);
		                if (infoAlert) infoAlert.style.display = 'none';
		                if (errorAlert) errorAlert.style.display = 'none';
		                if (btnConfirmar) btnConfirmar.disabled = false;
		                if (hiddenMontoRecibido) hiddenMontoRecibido.value = recibido.toFixed(2);
		            }
		        });
		    }
		});
		
		const hiddenTotal = document.getElementById('modal-total-hidden');

		// al abrir el modal (en btnEfectivo.click):
		const totalTexto = totalCarritoEl ? totalCarritoEl.textContent.trim() : '$0.00';
		if (modalTotalEl) modalTotalEl.textContent = totalTexto;
		if (hiddenTotal) {
		    const totalNum = parseFloat(
		        totalTexto.replace('$','').replace(/\./g,'').replace(',','.')
		    ) || 0;
		    hiddenTotal.value = totalNum.toFixed(2);
		}

		// en el listener de input de modal-recibido, donde ya habilitas el botón:
		if (hiddenMontoRecibido) hiddenMontoRecibido.value = recibido.toFixed(2);
