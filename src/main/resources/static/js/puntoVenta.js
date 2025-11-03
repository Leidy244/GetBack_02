// 🛒 Carrito de compras
let carrito = [];

// 🎯 Esperar a que cargue TODO el documento
window.addEventListener('DOMContentLoaded', function() {
    console.log('✅ Punto de venta cargado');
    inicializarCarrito();
});

// 📌 Inicializar el sistema de carrito
function inicializarCarrito() {
    // Cargar carrito guardado
    cargarCarritoDesdeLocalStorage();
    
    // Configurar botones de agregar
    configurarBotonesAgregar();
    
    // Configurar otros botones
    configurarOtrosBotones();
    
    // Configurar búsqueda
    configurarBusqueda();
    
    console.log('✅ Sistema de carrito inicializado');
}

// 🛍️ Configurar botones de agregar al carrito
function configurarBotonesAgregar() {
    const botones = document.querySelectorAll('.btn-agregar');
    console.log(`📦 Encontrados ${botones.length} productos`);
    
    botones.forEach((btn, index) => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            console.log(`🔵 Click en producto ${index + 1}`);
            
            const card = this.closest('.producto-card');
            
            if (!card) {
                console.error('❌ No se encontró la tarjeta del producto');
                return;
            }
            
            const id = card.getAttribute('data-id');
            const nombre = card.getAttribute('data-nombre');
            const precio = parseFloat(card.getAttribute('data-precio'));
            
            console.log('Datos del producto:', { id, nombre, precio });
            
            if (!id || !nombre || isNaN(precio)) {
                console.error('❌ Datos del producto incompletos');
                alert('Error: Datos del producto incompletos');
                return;
            }
            
            agregarAlCarrito({ id, nombre, precio });
        });
    });
}

// ➕ Agregar producto al carrito
function agregarAlCarrito(producto) {
    console.log('➕ Agregando al carrito:', producto);
    
    // Buscar si ya existe
    const itemExistente = carrito.find(item => item.id === producto.id);
    
    if (itemExistente) {
        itemExistente.cantidad++;
        console.log('📈 Cantidad aumentada:', itemExistente);
    } else {
        carrito.push({
            id: producto.id,
            nombre: producto.nombre,
            precio: producto.precio,
            cantidad: 1
        });
        console.log('🆕 Producto nuevo agregado');
    }
    
    console.log('🛒 Carrito actual:', carrito);
    
    // Actualizar vista
    actualizarVistaCarrito();
    guardarCarritoEnLocalStorage();
    
    // Mostrar feedback
    mostrarNotificacion('✅ Producto agregado al carrito');
}

// 🔄 Actualizar la vista del carrito
function actualizarVistaCarrito() {
    const contenedor = document.getElementById('items-carrito');
    const totalElement = document.getElementById('total-carrito');
    
    if (!contenedor || !totalElement) {
        console.error('❌ No se encontraron elementos del carrito en el DOM');
        return;
    }
    
    // Limpiar
    contenedor.innerHTML = '';
    
    if (carrito.length === 0) {
        contenedor.innerHTML = '<p class="carrito-vacio">🛒 El carrito está vacío</p>';
        totalElement.textContent = '$0.00';
        return;
    }
    
    let total = 0;
    
    // Renderizar items
    carrito.forEach((item, index) => {
        const subtotal = item.precio * item.cantidad;
        total += subtotal;
        
        const itemDiv = document.createElement('div');
        itemDiv.className = 'item-carrito';
        itemDiv.innerHTML = `
            <div class="info-item">
                <h5>${item.nombre}</h5>
                <p class="precio-unitario">$${item.precio.toFixed(2)} c/u</p>
            </div>
            <div class="controles-cantidad">
                <button class="btn btn-sm btn-outline-secondary btn-menos" data-index="${index}">
                    <i class="fas fa-minus"></i>
                </button>
                <span class="cantidad">${item.cantidad}</span>
                <button class="btn btn-sm btn-outline-secondary btn-mas" data-index="${index}">
                    <i class="fas fa-plus"></i>
                </button>
            </div>
            <div class="subtotal-item">
                <strong>$${subtotal.toFixed(2)}</strong>
                <button class="btn btn-sm btn-outline-danger btn-eliminar" data-index="${index}">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;
        
        contenedor.appendChild(itemDiv);
    });
    
    // Actualizar total
    totalElement.textContent = `$${total.toFixed(2)}`;
    
    // Configurar botones de cantidad y eliminar
    configurarBotonesCarrito();
    
    console.log('✅ Vista del carrito actualizada');
}

// 🎛️ Configurar botones dentro del carrito
function configurarBotonesCarrito() {
    // Botones de aumentar cantidad
    document.querySelectorAll('.btn-mas').forEach(btn => {
        btn.addEventListener('click', function() {
            const index = parseInt(this.getAttribute('data-index'));
            carrito[index].cantidad++;
            actualizarVistaCarrito();
            guardarCarritoEnLocalStorage();
        });
    });
    
    // Botones de disminuir cantidad
    document.querySelectorAll('.btn-menos').forEach(btn => {
        btn.addEventListener('click', function() {
            const index = parseInt(this.getAttribute('data-index'));
            if (carrito[index].cantidad > 1) {
                carrito[index].cantidad--;
            } else {
                carrito.splice(index, 1);
            }
            actualizarVistaCarrito();
            guardarCarritoEnLocalStorage();
        });
    });
    
    // Botones de eliminar
    document.querySelectorAll('.btn-eliminar').forEach(btn => {
        btn.addEventListener('click', function() {
            const index = parseInt(this.getAttribute('data-index'));
            if (confirm('¿Eliminar este producto del carrito?')) {
                carrito.splice(index, 1);
                actualizarVistaCarrito();
                guardarCarritoEnLocalStorage();
            }
        });
    });
}

// 🎛️ Configurar otros botones
function configurarOtrosBotones() {
    // Vaciar carrito
    const btnVaciar = document.getElementById('btn-vaciar-carrito');
    if (btnVaciar) {
        btnVaciar.addEventListener('click', function() {
            if (carrito.length === 0) {
                alert('El carrito ya está vacío');
                return;
            }
            if (confirm('¿Vaciar todo el carrito?')) {
                carrito = [];
                actualizarVistaCarrito();
                guardarCarritoEnLocalStorage();
                mostrarNotificacion('🗑️ Carrito vaciado');
            }
        });
    }
    
    // Finalizar venta
    const btnFinalizar = document.getElementById('btn-finalizar-venta');
    if (btnFinalizar) {
        btnFinalizar.addEventListener('click', finalizarVenta);
    }
    
    // Imprimir
    const btnImprimir = document.getElementById('btn-imprimir');
    if (btnImprimir) {
        btnImprimir.addEventListener('click', imprimirTicket);
    }
    
    // Actualizar
    const btnActualizar = document.getElementById('btn-actualizar');
    if (btnActualizar) {
        btnActualizar.addEventListener('click', () => location.reload());
    }
}

// 🔍 Configurar búsqueda
function configurarBusqueda() {
    const inputBuscar = document.getElementById('buscar-productos');
    if (inputBuscar) {
        inputBuscar.addEventListener('input', function() {
            const termino = this.value.toLowerCase();
            const productos = document.querySelectorAll('.producto-card');
            
            productos.forEach(producto => {
                const nombre = producto.getAttribute('data-nombre').toLowerCase();
                producto.style.display = nombre.includes(termino) ? 'block' : 'none';
            });
        });
    }
}

// ✅ Finalizar venta
function finalizarVenta() {
    if (carrito.length === 0) {
        alert('El carrito está vacío');
        return;
    }
    
    const total = carrito.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
    
    if (confirm(`¿Confirmar venta por $${total.toFixed(2)}?`)) {
        console.log('💰 Venta finalizada:', { carrito, total, fecha: new Date() });
        alert('✅ ¡Venta realizada con éxito!');
        carrito = [];
        actualizarVistaCarrito();
        guardarCarritoEnLocalStorage();
    }
}

// 🖨️ Imprimir ticket
function imprimirTicket() {
    if (carrito.length === 0) {
        alert('El carrito está vacío');
        return;
    }
    
    const total = carrito.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
    const fecha = new Date().toLocaleString('es-CO');
    
    let contenido = `
        <html>
        <head>
            <title>Ticket de Venta</title>
            <style>
                body { font-family: 'Courier New', monospace; padding: 20px; width: 300px; }
                h2 { text-align: center; }
                hr { border: 1px dashed #000; }
                .item { margin: 10px 0; }
                .total { font-size: 18px; font-weight: bold; margin-top: 20px; }
            </style>
        </head>
        <body>
            <h2>TICKET DE VENTA</h2>
            <hr>
            <p>Fecha: ${fecha}</p>
            <hr>
    `;
    
    carrito.forEach(item => {
        const subtotal = item.precio * item.cantidad;
        contenido += `
            <div class="item">
                <strong>${item.nombre}</strong><br>
                ${item.cantidad} x $${item.precio.toFixed(2)} = $${subtotal.toFixed(2)}
            </div>
        `;
    });
    
    contenido += `
            <hr>
            <div class="total">TOTAL: $${total.toFixed(2)}</div>
            <hr>
            <p style="text-align: center;">¡Gracias por su compra!</p>
            <script>
                window.print();
                window.onafterprint = () => window.close();
            </script>
        </body>
        </html>
    `;
    
    const ventana = window.open('', '', 'width=350,height=600');
    ventana.document.write(contenido);
}

// 💾 Guardar en localStorage
function guardarCarritoEnLocalStorage() {
    try {
        localStorage.setItem('carritoPOS', JSON.stringify(carrito));
        console.log('💾 Carrito guardado');
    } catch (e) {
        console.error('❌ Error guardando carrito:', e);
    }
}

// 📥 Cargar desde localStorage
function cargarCarritoDesdeLocalStorage() {
    try {
        const guardado = localStorage.getItem('carritoPOS');
        if (guardado) {
            carrito = JSON.parse(guardado);
            console.log('📥 Carrito cargado:', carrito);
            actualizarVistaCarrito();
        }
    } catch (e) {
        console.error('❌ Error cargando carrito:', e);
        carrito = [];
    }
}

// 🔔 Mostrar notificación
function mostrarNotificacion(mensaje) {
    // Crear notificación temporal
    const notif = document.createElement('div');
    notif.textContent = mensaje;
    notif.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #28a745;
        color: white;
        padding: 15px 25px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        z-index: 9999;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(notif);
    
    setTimeout(() => {
        notif.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notif.remove(), 300);
    }, 2000);
}