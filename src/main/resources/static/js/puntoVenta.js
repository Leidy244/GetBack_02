document.addEventListener('DOMContentLoaded', function() {
    // ========== VARIABLES GLOBALES ==========
    let carrito = JSON.parse(localStorage.getItem('carritoPOS') || '[]');
    let totalGlobal = 0;

    // ========== FUNCIONES PRINCIPALES ==========
    function renderCarrito() {
        const contenedor = document.getElementById('items-carrito');
        const totalEl = document.getElementById('total-carrito');
        const modalTotalEl = document.getElementById('modal-total');
        
        if (!contenedor) {
            console.error("❌ NO SE ENCUENTRA items-carrito");
            return;
        }
        
        contenedor.innerHTML = '';
        totalGlobal = 0;
        
        console.log("📦 Renderizando carrito con", carrito.length, "productos");
        
        if (carrito && carrito.length > 0) {
            carrito.forEach((item, index) => {
                const precio = parseFloat(item.precio) || 0;
                const cantidad = parseInt(item.cantidad) || 1;
                const subtotal = precio * cantidad;
                totalGlobal += subtotal;
                
                console.log(`   Producto ${index + 1}: ${cantidad} x ${item.nombre} = $${subtotal}`);

                const div = document.createElement('div');
                div.className = 'item-carrito d-flex justify-content-between align-items-center p-2 border-bottom';
                div.innerHTML = `
                    <div>
                        <strong>${item.nombre}</strong>
                    </div>

                    <div class="d-flex align-items-center">
                        <button class="btn btn-sm btn-outline-secondary btn-restar" data-index="${index}">−</button>
                        <span class="mx-2">${cantidad}</span>
                        <button class="btn btn-sm btn-outline-secondary btn-sumar" data-index="${index}">+</button>

                        <strong class="ms-3">$ ${subtotal.toLocaleString('es-CO')}</strong>

                        <button class="btn btn-danger btn-sm ms-2 btn-eliminar" data-index="${index}">×</button>
                    </div>
                `;
                contenedor.appendChild(div);
            });
        } else {
            console.log("🛒 Carrito vacío");
            contenedor.innerHTML = '<div class="text-center text-muted p-3">Carrito vacío</div>';
        }

        const totalTexto = '$' + totalGlobal.toLocaleString('es-CO');
        console.log("💰 TOTAL CALCULADO:", totalGlobal);
        
        if (totalEl) totalEl.textContent = totalTexto;
        if (modalTotalEl) modalTotalEl.textContent = totalTexto;

        localStorage.setItem('carritoPOS', JSON.stringify(carrito));
    }

    function eliminarItem(index) {
        console.log("🗑️ Eliminando item:", index);
        if (index >= 0 && index < carrito.length) {
            carrito.splice(index, 1);
            renderCarrito();
        }
    }

    function actualizarCantidad(index, cambio) {
        if (index >= 0 && index < carrito.length) {
            carrito[index].cantidad += cambio;
            if (carrito[index].cantidad <= 0) {
                carrito.splice(index, 1);
            }
            renderCarrito();
        }
    }

    // ========== EVENT LISTENERS PARA CARRITO ==========
    document.getElementById('btn-vaciar-carrito')?.addEventListener('click', () => {
        console.log("🔄 Vaciar carrito clickeado");
        if (confirm('¿Está seguro de que desea vaciar el carrito?')) {
            carrito = [];
            totalGlobal = 0;
            localStorage.removeItem('carritoPOS');
            renderCarrito();
            console.log("✅ Carrito vaciado");
        }
    });

    // Delegación de eventos para sumar, restar y eliminar
    document.getElementById('items-carrito')?.addEventListener('click', (e) => {
        const btn = e.target;

        // ELIMINAR
        if (btn.classList.contains('btn-eliminar')) {
            const index = parseInt(btn.dataset.index);
            eliminarItem(index);
            return;
        }

        // SUMAR
        if (btn.classList.contains('btn-sumar')) {
            const index = parseInt(btn.dataset.index);
            actualizarCantidad(index, +1);
            return;
        }

        // RESTAR
        if (btn.classList.contains('btn-restar')) {
            const index = parseInt(btn.dataset.index);
            actualizarCantidad(index, -1);
            return;
        }
    });

    // Agregar productos al carrito
    document.querySelectorAll('.btn-agregar').forEach(btn => {
        btn.addEventListener('click', function() {
            const productoCard = this.closest('.producto-card');
            const id = productoCard.getAttribute('data-id');
            const nombre = productoCard.getAttribute('data-nombre');
            const precio = parseFloat(productoCard.getAttribute('data-precio'));

            console.log("🛍️ AGREGANDO PRODUCTO:", { id, nombre, precio });

            if (isNaN(precio) || precio <= 0) {
                console.error("💥 PRECIO INVÁLIDO:", precio);
                alert('Error: Precio inválido');
                return;
            }

            const itemExistente = carrito.find(item => item.id === id);
            if (itemExistente) {
                itemExistente.cantidad++;
            } else {
                carrito.push({ id, nombre, precio, cantidad: 1 });
            }

            renderCarrito();
        });
    });

    // ========== FILTRO DE BÚSQUEDA ==========
    const inputBusqueda = document.getElementById('buscar-productos');
    const btnBuscar = document.getElementById('btn-buscar');

    function filtrarProductos() {
        const texto = inputBusqueda.value.toLowerCase().trim();
        const productos = document.querySelectorAll('.producto-card');

        productos.forEach(card => {
            const nombre = card.dataset.nombre.toLowerCase();
            const descripcion = card.querySelector('p')?.textContent.toLowerCase() || '';

            card.style.display =
                nombre.includes(texto) || descripcion.includes(texto)
                ? 'block'
                : 'none';
        });
    }

    inputBusqueda?.addEventListener('input', filtrarProductos);
    btnBuscar?.addEventListener('click', filtrarProductos);

    // ========== MODAL MÉTODOS DE PAGO ==========
    const btnFinalizar = document.getElementById('btn-finalizar-venta');
    const modalMetodosEl = document.getElementById('metodosPagoModal');
    const modalPagoEl = document.getElementById('modalPago');

    btnFinalizar?.addEventListener('click', function() {
        console.log("🔄 Finalizar venta clickeado");
        
        renderCarrito();
        
        if (carrito.length === 0) {
            alert('❌ Carrito vacío. Agregue productos antes de finalizar la venta.');
            return;
        }

        if (totalGlobal <= 0) {
            alert('❌ Error: El total debe ser mayor a 0');
            return;
        }

        if (modalMetodosEl) {
            const modal = new bootstrap.Modal(modalMetodosEl);
            modal.show();
        }
    });

    // ========== BOTÓN EFECTIVO ==========
    const btnEfectivo = document.querySelector('#metodosPagoModal [data-metodo="EFECTIVO"]');
    
    btnEfectivo?.addEventListener('click', function() {
        console.log("💰 Método efectivo seleccionado");
        
        const metodosModal = bootstrap.Modal.getInstance(modalMetodosEl);
        if (metodosModal) metodosModal.hide();

        const modalTotalEl = document.getElementById('modal-total');
        if (modalTotalEl) modalTotalEl.textContent = '$' + totalGlobal.toLocaleString('es-CO');

        const recibidoInput = document.getElementById('modal-recibido');
        const cambioInput = document.getElementById('modal-cambio');
        
        if (recibidoInput) recibidoInput.value = '';
        if (cambioInput) cambioInput.value = '$0';

        if (modalPagoEl) {
            const modalPago = new bootstrap.Modal(modalPagoEl);
            modalPago.show();
        }
    });

    // ========== CÁLCULO DE CAMBIO ==========
    const recibidoInput = document.getElementById('modal-recibido');
    const cambioInput = document.getElementById('modal-cambio');
    
    recibidoInput?.addEventListener('input', function() {
        const recibido = parseFloat(this.value) || 0;

        if (recibido >= totalGlobal) {
            const cambio = recibido - totalGlobal;
            if (cambioInput) cambioInput.value = '$' + cambio.toLocaleString('es-CO');
        } else {
            if (cambioInput) cambioInput.value = '$0';
        }
    });

    // ========== FORMULARIO DE CONFIRMACIÓN ==========
    const formConfirmar = document.getElementById('form-confirmar-pago');
    
    formConfirmar?.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const montoRecibido = parseFloat(document.getElementById('modal-recibido').value) || 0;

        if (carrito.length === 0) {
            alert('❌ Error: Carrito vacío');
            return;
        }

        if (totalGlobal <= 0) {
            alert('❌ Error: Total inválido');
            return;
        }

        if (montoRecibido <= 0) {
            alert('❌ Error: Ingrese monto recibido');
            return;
        }

        if (montoRecibido < totalGlobal) {
            alert('❌ Error: Monto insuficiente');
            return;
        }

        const inputTotal = document.getElementById('input-total');
        const inputMontoRecibido = document.getElementById('input-monto-recibido');
        const inputCarrito = document.getElementById('input-carrito');
        const inputMesaId = document.getElementById('hidden-mesa-id');
        
        if (inputTotal) inputTotal.value = totalGlobal.toFixed(2);
        if (inputMontoRecibido) inputMontoRecibido.value = montoRecibido.toFixed(2);
        if (inputCarrito) inputCarrito.value = JSON.stringify(carrito);
        if (inputMesaId) inputMesaId.value = document.getElementById('modal-mesa')?.value || '';

        const confirmMessage = `¿Confirmar venta por $${totalGlobal.toLocaleString('es-CO')}?\nMonto recibido: $${montoRecibido.toLocaleString('es-CO')}\nCambio: $${(montoRecibido - totalGlobal).toLocaleString('es-CO')}`;
        
        if (confirm(confirmMessage)) {
            carrito = [];
            totalGlobal = 0;
            localStorage.removeItem('carritoPOS');
            renderCarrito();
            this.submit();
        }
    });

    // ========== CLIENTES FRECUENTES ==========
    const inputBuscarCliente = document.getElementById('buscar-cliente-frecuente-pos');
    const tbodyClientes = document.getElementById('clientesFrecuentesTableBody');

    if (inputBuscarCliente && tbodyClientes) {
        const filas = Array.from(tbodyClientes.querySelectorAll('tr[data-row]'));

        function filtrarClientes() {
            const q = (inputBuscarCliente.value || '').toLowerCase().trim();
            filas.forEach(tr => {
                const nombreTd = tr.querySelector('td');
                const nombre = nombreTd ? (nombreTd.textContent || '').toLowerCase() : '';
                tr.style.display = (!q || nombre.includes(q)) ? '' : 'none';
            });
        }

        inputBuscarCliente.addEventListener('input', filtrarClientes);
    }

    // ========== PAGO CON ABONO ==========
    modalMetodosEl?.addEventListener('click', function(e) {
        const btn = e.target.closest('.btn-pagar-con-abono');
        if (!btn) return;

        const clienteId = btn.getAttribute('data-cliente-id');
        const clienteNombre = btn.getAttribute('data-cliente-nombre') || '';

        let totalTexto = document.getElementById('total-carrito')?.textContent || '0';
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

        const inputFromCaja = document.createElement('input');
        inputFromCaja.type = 'hidden';
        inputFromCaja.name = 'fromCaja';
        inputFromCaja.value = 'true';
        form.appendChild(inputFromCaja);

        document.body.appendChild(form);
        form.submit();
    });

    // ========== INICIALIZACIÓN ==========
    console.log("🎬 INICIALIZANDO PUNTO DE VENTA...");
    renderCarrito();
    console.log("✅ PUNTO DE VENTA INICIALIZADO");
});
