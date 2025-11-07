document.addEventListener('DOMContentLoaded', () => {
    const carritoContainer = document.getElementById('items-carrito');
    const totalCarrito = document.getElementById('total-carrito');
    const btnVaciar = document.getElementById('btn-vaciar-carrito');

    let carrito = [];

    // 🧱 Renderizar el carrito
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

    // 💰 Total del carrito
    function actualizarTotal() {
        const total = carrito.reduce((sum, item) => sum + (item.precio * item.cantidad), 0);
        totalCarrito.textContent = total.toLocaleString('es-CO', { style: 'currency', currency: 'COP' });
    }

    // 🟢 Agregar productos
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

            renderCarrito();
        }
    });

    // ➕➖ y 🗑️ dentro del carrito
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

    // 🚮 Vaciar carrito
    btnVaciar.addEventListener('click', () => {
        carrito = [];
        renderCarrito();
    });
});
// 🔍 --- FILTRO DE BÚSQUEDA ---
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

// 🔹 Filtra mientras escribe
inputBusqueda.addEventListener('input', filtrarProductos);

// 🔹 O al hacer clic en el botón de buscar
btnBuscar.addEventListener('click', filtrarProductos);
