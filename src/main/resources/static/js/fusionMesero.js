document.addEventListener('DOMContentLoaded', () => {
    
    function sendOrder() {
        if (orderItems.length === 0) {
            showNotification('❌ No hay productos en el pedido', 'error');
            return;
        }

        const comments = orderComments ? orderComments.value : '';
        const mesaId = confirmBtn ? confirmBtn.getAttribute('data-mesa') : '1';

        // Recalcular total por si hubo cambios recientes
        totalOrder = orderItems.reduce((sum, item) => sum + item.subtotal, 0);

        const itemsData = {
            items: orderItems.map(item => ({
                productoId: parseInt(item.id),
                productoNombre: item.name,
                cantidad: item.quantity,
                precio: item.price,
                subtotal: item.price * item.quantity,
                comentarios: ''
            })),
            total: totalOrder
        };

        const itemsJson = JSON.stringify(itemsData);

        const formData = new URLSearchParams();
        formData.append('mesaId', parseInt(mesaId));
        formData.append('itemsJson', itemsJson);
        formData.append('comentarios', comments);
        formData.append('total', totalOrder);

        fetch('/pedidos/preparar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('✅ Pedido preparado. Redirigiendo a vista de confirmación...');
                orderModal.style.display = 'none';
                // Reset order after successful preparation
                resetOrder();
                setTimeout(() => {
                    const redirectUrl = data.redirect || ('/verpedido?mesa=' + mesaId);
                    window.location.href = redirectUrl;
                }, 800);
            } else {
                showNotification('❌ Error al preparar pedido: ' + data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showNotification('❌ Error de conexión', 'error');
        });
    }
    
    // Botón flotante para volver a mesas
    const backToMesasBtn = document.getElementById('btn-back-to-mesas');
    if (backToMesasBtn) {
        backToMesasBtn.addEventListener('click', () => {
            // Redirigir a la vista de mesas
            window.location.href = '/mesero';
        });
    }

    // Mobile navigation toggle
    const menuToggle = document.getElementById('menu-toggle');
    const navLinks = document.getElementById('nav-links');
    const navOverlay = document.getElementById('nav-overlay');

    if (menuToggle && navLinks && navOverlay) {
        menuToggle.addEventListener('click', () => {
            navLinks.classList.toggle('active');
            navOverlay.classList.toggle('active');
            document.body.classList.toggle('no-scroll');
        });

        navOverlay.addEventListener('click', () => {
            navLinks.classList.remove('active');
            navOverlay.classList.remove('active');
            document.body.classList.remove('no-scroll');
        });
    }

    // Modal functionality
    const orderModal = document.getElementById('orderModal');
    const closeBtn = orderModal?.querySelector('.close-modal');
    const cancelBtn = orderModal?.querySelector('.btn-cancel');
    const confirmBtn = orderModal?.querySelector('.btn-confirm');
    const orderBtn = document.querySelector('.btn-order');

    // Quantity controls and order total (declarados antes de usar borrador)
    const quantityControls = document.querySelectorAll('.quantity-control');
    const pedidoTotalValue = document.getElementById('pedido-total-value');
    let totalOrder = 0;
    let orderItems = [];

    if (orderBtn && orderModal) {
        orderBtn.addEventListener('click', () => {
            orderModal.style.display = 'flex';
            updateOrderSummary();
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            orderModal.style.display = 'none';
        });
    }

    if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
            orderModal.style.display = 'none';
        });
    }

    if (confirmBtn) {
        confirmBtn.addEventListener('click', () => {
            sendOrder();
        });
    }

    // Close modal when clicking outside of it
    if (orderModal) {
        window.addEventListener('click', (event) => {
            if (event.target === orderModal) {
                orderModal.style.display = 'none';
            }
        });
    }

    const orderComments = document.getElementById('order-comments');

    // Inicializar desde borrador (flujo de "Editar") si existe
    try {
        if (typeof draftItemsJson !== 'undefined' && draftItemsJson) {
            const draftData = JSON.parse(draftItemsJson);
            if (draftData && Array.isArray(draftData.items)) {
                orderItems = draftData.items.map(it => {
                    const price = Number(it.precio) || 0;
                    const quantity = Number(it.cantidad) || 0;
                    return {
                        id: String(it.productoId),
                        name: it.productoNombre,
                        price: price,
                        quantity: quantity,
                        subtotal: price * quantity
                    };
                });

                // Reflejar cantidades en los controles de la UI
                orderItems.forEach(item => {
                    const product = document.querySelector(`.product-item[data-producto-id="${item.id}"]`);
                    if (product) {
                        const quantityInput = product.querySelector('.quantity-input');
                        if (quantityInput) {
                            quantityInput.value = item.quantity;
                        }
                    }
                });

                // Recalcular SIEMPRE el total a partir de los subtotales
                totalOrder = orderItems.reduce((sum, it) => sum + (it.subtotal || 0), 0);
                updateTotal();

                // Cargar comentarios del borrador si existen
                if (typeof draftComentarios !== 'undefined' && draftComentarios && orderComments) {
                    orderComments.value = draftComentarios;
                }
            }
        }
    } catch (e) {
        console.error('Error al inicializar borrador de pedido:', e);
    }

    quantityControls.forEach(control => {
        const minusBtn = control.querySelector('.minus');
        const plusBtn = control.querySelector('.plus');
        const quantityInput = control.querySelector('.quantity-input');
        const addToOrderBtn = control.querySelector('.btn-add-to-order');
        const productItem = control.closest('.product-item');
        const priceElement = productItem?.querySelector('.product-price');
        
        if (priceElement) {
            const price = parseFloat(priceElement.textContent.replace('$', '').replace('.', '').replace(',', '.'));
            const productId = productItem.getAttribute('data-producto-id');
            const productName = productItem.querySelector('.product-name')?.textContent || 'Producto';
            
            if (minusBtn) {
                minusBtn.addEventListener('click', (e) => {
                    e.stopPropagation(); // Prevent product click event
                    let currentValue = parseInt(quantityInput.value);
                    if (currentValue > 0) {
                        const newQuantity = currentValue - 1;
                        quantityInput.value = newQuantity;
                        updateOrderItem(productId, productName, price, newQuantity);
                        // Recalculate total from all items
                        totalOrder = orderItems.reduce((sum, item) => sum + item.subtotal, 0);
                        updateTotal();
                        
                        if (newQuantity === 0) {
                            showNotification(`❌ Eliminado: ${productName}`);
                        } else {
                            showNotification(`➖ ${productName}: ${newQuantity}`);
                        }
                    }
                });
            }

            if (plusBtn) {
                plusBtn.addEventListener('click', (e) => {
                    e.stopPropagation(); // Prevent product click event
                    let currentValue = parseInt(quantityInput.value);
                    const newQuantity = currentValue + 1;
                    quantityInput.value = newQuantity;
                    updateOrderItem(productId, productName, price, newQuantity);
                    // Recalculate total from all items
                    totalOrder = orderItems.reduce((sum, item) => sum + item.subtotal, 0);
                    updateTotal();
                    showNotification(`➕ ${productName}: ${newQuantity}`);
                });
            }

            if (addToOrderBtn) {
                addToOrderBtn.addEventListener('click', () => {
                    const quantity = parseInt(quantityInput.value);
                    if (quantity > 0) {
                        updateOrderItem(productId, productName, price, quantity);
                        showNotification(`✅ Agregado: ${quantity} x ${productName}`);
                    } else {
                        showNotification('⚠️ Por favor selecciona al menos 1 cantidad', 'warning');
                    }
                });
            }
        }
    });

    function updateOrderItem(productId, productName, price, quantity) {
        const existingItemIndex = orderItems.findIndex(item => item.id === productId);
        
        if (quantity === 0) {
            // Remove item if quantity is 0
            if (existingItemIndex !== -1) {
                orderItems.splice(existingItemIndex, 1);
            }
        } else {
            if (existingItemIndex !== -1) {
                // Update existing item
                orderItems[existingItemIndex].quantity = quantity;
                orderItems[existingItemIndex].subtotal = price * quantity;
            } else {
                // Add new item
                orderItems.push({
                    id: productId,
                    name: productName,
                    price: price,
                    quantity: quantity,
                    subtotal: price * quantity
                });
            }
        }
    }

    const updateTotal = () => {
        if (pedidoTotalValue) {
            pedidoTotalValue.textContent = `$${totalOrder.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, '.')}`;
        }
    };

    // Click on product to add functionality (only on product info, not buttons)
    const clickableProducts = document.querySelectorAll('.clickable-product');
    clickableProducts.forEach(product => {
        const productInfo = product.querySelector('.product-info');
        if (productInfo) {
            productInfo.addEventListener('click', (e) => {
                // Prevent event if clicking on quantity controls
                if (e.target.closest('.quantity-control')) {
                    return;
                }
                
                const productId = product.getAttribute('data-producto-id');
                const productName = product.getAttribute('data-producto-nombre');
                const productPrice = parseFloat(product.getAttribute('data-precio'));
                
                // Find existing item or get current quantity
                const existingItem = orderItems.find(item => item.id === productId);
                const newQuantity = existingItem ? existingItem.quantity + 1 : 1;
                
                // Update order item with new quantity
                updateOrderItem(productId, productName, productPrice, newQuantity);
                
                // Recalculate total from all items
                totalOrder = orderItems.reduce((sum, item) => sum + item.subtotal, 0);
                updateTotal();
                
                showNotification(`✅ Agregado: ${newQuantity} x ${productName}`);
                
                // Update the quantity input
                const quantityInput = product.querySelector('.quantity-input');
                if (quantityInput) {
                    quantityInput.value = newQuantity;
                }
            });
        }
    });

    // Filter functionality by category
    const filterButtons = document.querySelectorAll('.nav-links .filter-btn');
    const productItems = document.querySelectorAll('.product-item');
    
    filterButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            
            // Remove active class from all buttons
            filterButtons.forEach(b => b.classList.remove('active'));
            // Add active class to clicked button
            btn.classList.add('active');
            
            const categoriaId = btn.getAttribute('data-categoria');
            filterProductsByCategory(categoriaId);
        });
    });

    function filterProductsByCategory(categoriaId) {
        let hasVisibleProducts = false;
        
        productItems.forEach(item => {
            const itemCategoria = item.getAttribute('data-categoria');
            
            if (categoriaId === 'todas' || itemCategoria === categoriaId) {
                item.style.display = 'flex';
                hasVisibleProducts = true;
            } else {
                item.style.display = 'none';
            }
        });
        
        // Show/hide no products message
        const noProductsMessage = document.getElementById('no-products');
        if (noProductsMessage) {
            noProductsMessage.style.display = hasVisibleProducts ? 'none' : 'block';
        }
    }

    // Search functionality
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase().trim();
            
            if (searchTerm === '') {
                // If search is empty, restore category filter
                const activeFilter = document.querySelector('.nav-links .filter-btn.active');
                const activeCategory = activeFilter ? activeFilter.getAttribute('data-categoria') : 'todas';
                filterProductsByCategory(activeCategory);
                return;
            }
            
            let hasVisibleProducts = false;
            
            productItems.forEach(item => {
                const productName = item.querySelector('.product-name')?.textContent.toLowerCase() || '';
                const productDesc = item.querySelector('.product-description')?.textContent.toLowerCase() || '';
                
                if (productName.includes(searchTerm) || productDesc.includes(searchTerm)) {
                    item.style.display = 'flex';
                    hasVisibleProducts = true;
                } else {
                    item.style.display = 'none';
                }
            });
            
            // Show/hide no products message
            const noProductsMessage = document.getElementById('no-products');
            if (noProductsMessage) {
                noProductsMessage.style.display = hasVisibleProducts ? 'none' : 'block';
            }
        });
    }

    // Comments functionality
    const commentsBtn = document.getElementById('btn-comments');
    const commentsContainer = document.getElementById('comments-container');
    
    if (commentsBtn && commentsContainer) {
        commentsBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            commentsContainer.classList.toggle('active');
        });

        // Close comments when clicking outside
        document.addEventListener('click', (e) => {
            if (!commentsContainer.contains(e.target) && e.target !== commentsBtn) {
                commentsContainer.classList.remove('active');
            }
        });
    }

    // Update order summary for modal
    function updateOrderSummary() {
        const orderSummary = document.getElementById('order-summary');
        if (!orderSummary) return;

        if (orderItems.length === 0) {
            orderSummary.innerHTML = '<p class="no-items">No hay productos en el pedido</p>';
            return;
        }

        let summaryHTML = `
            <div class="order-items">
                <h4>Resumen del Pedido:</h4>
                <div class="order-items-list">
        `;

        orderItems.forEach(item => {
            summaryHTML += `
                <div class="order-item">
                    <span class="item-name">${item.quantity} x ${item.name}</span>
                    <span class="item-subtotal">$${(item.subtotal).toFixed(2)}</span>
                </div>
            `;
        });

        summaryHTML += `
                </div>
                <div class="order-total-modal">
                    <strong>Total: $${totalOrder.toFixed(2)}</strong>
                </div>
            </div>
        `;

        orderSummary.innerHTML = summaryHTML;
    }

    function resetOrder() {
        orderItems = [];
        totalOrder = 0;
        updateTotal();
        
        // Reset all quantity inputs
        quantityControls.forEach(control => {
            const quantityInput = control.querySelector('.quantity-input');
            if (quantityInput) quantityInput.value = 0;
        });
        
        // Reset comments
        if (orderComments) orderComments.value = '';
        
        // Close comments container
        if (commentsContainer) commentsContainer.classList.remove('active');
    }

    // Notification system
    function showNotification(message, type = 'success') {
        // Remove existing notification
        const existingNotification = document.querySelector('.notification');
        if (existingNotification) {
            existingNotification.remove();
        }

        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 5px;
            color: white;
            font-weight: bold;
            z-index: 10000;
            animation: slideIn 0.3s ease;
            max-width: 300px;
        `;

        if (type === 'success') {
            notification.style.background = 'linear-gradient(135deg, var(--success), #1a8c16)';
        } else if (type === 'warning') {
            notification.style.background = 'linear-gradient(135deg, var(--warning), #e68900)';
        } else if (type === 'error') {
            notification.style.background = 'linear-gradient(135deg, var(--error), #d32f2f)';
        }

        document.body.appendChild(notification);

        // Auto remove after 3 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOut 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, 3000);
    }

    // Add CSS animations for notifications
    const style = document.createElement('style');
    style.textContent = `
        @keyframes slideIn {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
        @keyframes slideOut {
            from { transform: translateX(0); opacity: 1; }
            to { transform: translateX(100%); opacity: 0; }
        }
        .no-items { text-align: center; color: var(--text-secondary); margin: 20px 0; }
        .order-items { margin: 15px 0; }
        .order-items h4 { margin-bottom: 10px; color: var(--accent); }
        .order-item { display: flex; justify-content: space-between; margin: 8px 0; padding: 5px 0; border-bottom: 1px solid rgba(255,255,255,0.1); }
        .item-name { flex: 2; }
        .item-subtotal { flex: 1; text-align: right; font-weight: bold; }
        .order-total-modal { margin-top: 15px; padding-top: 10px; border-top: 2px solid var(--primary); text-align: center; font-size: 1.2em; }
    `;
    document.head.appendChild(style);

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