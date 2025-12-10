// Base de datos de mesas
const mesasData = {
    tarima: [
        { numero: 1, capacidad: 4, estado: 'disponible', icon: 'fas fa-utensils' },
        { numero: 2, capacidad: 4, estado: 'disponible', icon: 'fas fa-wine-glass' },
        { numero: 3, capacidad: 6, estado: 'ocupada', icon: 'fas fa-beer-mug-empty' },
        { numero: 4, capacidad: 2, estado: 'ocupada', icon: 'fas fa-burger' },
        { numero: 5, capacidad: 4, estado: 'disponible', icon: 'fas fa-cocktail' },
        { numero: 6, capacidad: 4, estado: 'ocupada', icon: 'fas fa-coffee' },
        { numero: 7, capacidad: 8, estado: 'disponible', icon: 'fas fa-pizza-slice' },
        { numero: 8, capacidad: 4, estado: 'ocupada', icon: 'fas fa-utensils' },
        { numero: 9, capacidad: 2, estado: 'ocupada', icon: 'fas fa-wine-glass' },
        { numero: 10, capacidad: 4, estado: 'disponible', icon: 'fas fa-beer-mug-empty' }
    ],
    barra: [
        { numero: 1, capacidad: 2, estado: 'disponible', icon: 'fas fa-cocktail' },
        { numero: 2, capacidad: 4, estado: 'ocupada', icon: 'fas fa-wine-glass' },
        { numero: 3, capacidad: 2, estado: 'disponible', icon: 'fas fa-beer-mug-empty' },
        { numero: 4, capacidad: 2, estado: 'disponible', icon: 'fas fa-burger' },
        { numero: 5, capacidad: 4, estado: 'ocupada', icon: 'fas fa-utensils' },
        { numero: 6, capacidad: 4, estado: 'disponible', icon: 'fas fa-coffee' },
        { numero: 7, capacidad: 8, estado: 'disponible', icon: 'fas fa-pizza-slice' },
        { numero: 8, capacidad: 4, estado: 'ocupada', icon: 'fas fa-utensils' },
        { numero: 9, capacidad: 2, estado: 'ocupada', icon: 'fas fa-wine-glass' },
        { numero: 10, capacidad: 4, estado: 'disponible', icon: 'fas fa-beer-mug-empty' }
    ],
    fumadores: [
        { numero: 1, capacidad: 4, estado: 'disponible', icon: 'fas fa-smoking' },
        { numero: 2, capacidad: 4, estado: 'ocupada', icon: 'fas fa-smoking' },
        { numero: 3, capacidad: 6, estado: 'disponible', icon: 'fas fa-smoking' },
        { numero: 4, capacidad: 2, estado: 'ocupada', icon: 'fas fa-smoking' },
        { numero: 5, capacidad: 4, estado: 'disponible', icon: 'fas fa-smoking' },
        { numero: 6, capacidad: 4, estado: 'ocupada', icon: 'fas fa-smoking' },
        { numero: 7, capacidad: 8, estado: 'disponible', icon: 'fas fa-smoking' },
        { numero: 8, capacidad: 4, estado: 'ocupada', icon: 'fas fa-smoking' },
        { numero: 9, capacidad: 2, estado: 'disponible', icon: 'fas fa-smoking' },
        { numero: 10, capacidad: 4, estado: 'ocupada', icon: 'fas fa-smoking' }
    ],
    mesanine: [
        { numero: 1, capacidad: 4, estado: 'disponible', icon: 'fas fa-chair' },
        { numero: 2, capacidad: 4, estado: 'ocupada', icon: 'fas fa-chair' },
        { numero: 3, capacidad: 6, estado: 'disponible', icon: 'fas fa-chair' },
        { numero: 4, capacidad: 2, estado: 'ocupada', icon: 'fas fa-chair' },
        { numero: 5, capacidad: 4, estado: 'disponible', icon: 'fas fa-chair' },
        { numero: 6, capacidad: 4, estado: 'ocupada', icon: 'fas fa-chair' },
        { numero: 7, capacidad: 8, estado: 'disponible', icon: 'fas fa-chair' },
        { numero: 8, capacidad: 4, estado: 'ocupada', icon: 'fas fa-chair' },
        { numero: 9, capacidad: 2, estado: 'disponible', icon: 'fas fa-chair' },
        { numero: 10, capacidad: 4, estado: 'ocupada', icon: 'fas fa-chair' }
    ]
};

// Elementos comunes
const menuToggle = document.getElementById('menu-toggle');
const navLinks = document.getElementById('nav-links');
const welcomeToast = document.getElementById('welcome-toast');
const closeToastBtn = document.querySelector('.close-toast');
const backToTopBtn = document.getElementById('back-to-top');
const mesasContainer = document.getElementById('mesas-container');
const zonaTitulo = document.getElementById('zona-titulo');
const navOverlay = document.getElementById('nav-overlay');

// Toggle de menú hamburguesa para mostrar Áreas, disponible siempre
if (menuToggle && navLinks) {
  menuToggle.addEventListener('click', () => {
    navLinks.classList.toggle('active');
    if (navOverlay) navOverlay.classList.toggle('active');
    document.body.classList.toggle('no-scroll');
    menuToggle.classList.toggle('hidden');
  });
  // Cerrar menú al seleccionar un área
  document.querySelectorAll('.nav-links a').forEach(item => {
    item.addEventListener('click', () => {
      navLinks.classList.remove('active');
      if (navOverlay) navOverlay.classList.remove('active');
      document.body.classList.remove('no-scroll');
      menuToggle.classList.remove('hidden');
    });
  });
  // Cerrar al tocar fuera
  if (navOverlay) {
    navOverlay.addEventListener('click', () => {
      navLinks.classList.remove('active');
      navOverlay.classList.remove('active');
      document.body.classList.remove('no-scroll');
      menuToggle.classList.remove('hidden');
    });
  }
  document.addEventListener('click', (e) => {
    if (navLinks.classList.contains('active') && !navLinks.contains(e.target) && !menuToggle.contains(e.target)) {
      navLinks.classList.remove('active');
      if (navOverlay) navOverlay.classList.remove('active');
      document.body.classList.remove('no-scroll');
      menuToggle.classList.remove('hidden');
    }
  });
}

// Si no existe el contenedor legacy de mesas, no ejecutamos el resto de este script
if (!mesasContainer || !zonaTitulo) {
    console.debug('fusionMesas.js: sin contenedor legacy, se omiten funciones de renderización.');
} else {

let zonaActual = 'tarima'; // Establecer la zona inicial

// (El toggle de navegación se maneja arriba y no depende de contenedores legacy)

// Funcionalidad del Toast de bienvenida
function closeToast() {
    welcomeToast.style.animation = 'fadeOut 0.5s forwards';
    setTimeout(() => {
        welcomeToast.style.display = 'none';
    }, 500);
}

closeToastBtn.addEventListener('click', closeToast);
setTimeout(closeToast, 5000);

// Funcionalidad del botón de volver arriba
window.addEventListener('scroll', () => {
    backToTopBtn.style.display = window.pageYOffset > 300 ? 'flex' : 'none';
});

backToTopBtn.addEventListener('click', () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
});

// Función para renderizar mesas
function renderMesas(mesas) {
    mesasContainer.innerHTML = '';
    mesas.forEach(mesa => {

        const mesaDiv = document.createElement('div');
        mesaDiv.classList.add('mesa', mesa.estado);
        mesaDiv.dataset.mesa = mesa.numero;
        
        // Hacer la mesa clickeable
        mesaDiv.style.cursor = 'pointer';
        mesaDiv.addEventListener('click', () => {
            redirectMesaLegacy(mesa.numero);
        });
        
        mesaDiv.innerHTML = `
            <i class="${mesa.icon} mesa-icon"></i>
            <div class="numero">${mesa.numero}</div>
            <div class="capacidad">${mesa.capacidad} personas</div>
        `;
        mesasContainer.appendChild(mesaDiv);
    });
}

// Lógica de filtrado
function handleFilter() {
    const estadoActivo = document.querySelector('.filter-btn-status.active').dataset.filter;
    const mesasDeZona = mesasData[zonaActual] || [];

    const mesasFiltradas = mesasDeZona.filter(mesa => {
        return estadoActivo === 'todas' || mesa.estado === estadoActivo;
    });

    renderMesas(mesasFiltradas);
}

// Event Listeners para los botones de zona y estado
document.querySelectorAll('.filter-btn-zone').forEach(btn => {
    btn.addEventListener('click', function(e) {
        e.preventDefault();
        document.querySelectorAll('.filter-btn-zone').forEach(el => el.classList.remove('active'));
        this.classList.add('active');
        zonaActual = this.dataset.zone;
        zonaTitulo.textContent = this.textContent.toUpperCase();
        handleFilter();
    });
});

document.querySelectorAll('.filter-btn-status').forEach(btn => {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.filter-btn-status').forEach(el => el.classList.remove('active'));
        this.classList.add('active');
        handleFilter();
    });
});

// Cargar las mesas de la zona inicial al cargar la página
document.addEventListener('DOMContentLoaded', () => {
    handleFilter();
});

document.addEventListener("DOMContentLoaded", function () {
    const userDropdownBtn = document.getElementById("userDropdown");

    const dropdownMenu = document.querySelector(".user-dropdown .dropdown-menu");

    // Toggle al hacer click en "Administrador"
    userDropdownBtn.addEventListener("click", function (e) {
        e.preventDefault();   // evita que afecte la barra
        e.stopPropagation();  // evita cierre inmediato
        dropdownMenu.classList.toggle("show");
    });

    // Cerrar si se hace click fuera
    document.addEventListener("click", function (e) {
        if (!dropdownMenu.contains(e.target) && !userDropdownBtn.contains(e.target)) {
            dropdownMenu.classList.remove("show");
        }
    });

    // Cerrar con tecla ESC
    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape") {
            dropdownMenu.classList.remove("show");
        }
    });
});

} // fin if mesasContainer
