document.addEventListener('DOMContentLoaded', () => {
	
	function sendOrder() {
	      if (orderItems.length === 0) {
	          showNotification('❌ No hay productos en el pedido', 'error');
	          return;
	      }

	      const comments = orderComments ? orderComments.value : '';
	      const mesaId = confirmBtn ? confirmBtn.getAttribute('data-mesa') : '1';

	      // Preparar datos para enviar (formato simple)
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

	      // Convertir a JSON string
	      const itemsJson = JSON.stringify(itemsData);

	      // Crear form data para enviar
	      const formData = new URLSearchParams();
	      formData.append('mesaId', parseInt(mesaId));
	      formData.append('itemsJson', itemsJson);
	      formData.append('comentarios', comments);
	      formData.append('total', totalOrder);

	      // Enviar pedido al servidor
	      fetch('/pedidos/crear', {
	          method: 'POST',
	          headers: {
	              'Content-Type': 'application/x-www-form-urlencoded',
	          },
	          body: formData
	      })
	      .then(response => response.json())
	      .then(data => {
	          if (data.success) {
	              showNotification('✅ Pedido enviado correctamente a la cocina!');
	              orderModal.style.display = 'none';
	              
	              // Redirigir a la página de ver pedido
	              setTimeout(() => {
	                  window.location.href = '/pedidos/ver?mesa=' + mesaId;
	              }, 1500);
	              
	              resetOrder();
	          } else {
	              showNotification('❌ Error al enviar pedido: ' + data.message, 'error');
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
            window.location.href = '/mesero/mesas';
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

    // Quantity controls and order total
    const quantityControls = document.querySelectorAll('.quantity-control');
    const pedidoTotalValue = document.getElementById('pedido-total-value');
    let totalOrder = 0;
    let orderItems = [];

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
                minusBtn.addEventListener('click', () => {
                    let currentValue = parseInt(quantityInput.value);
                    if (currentValue > 0) {
                        quantityInput.value = currentValue - 1;
                        totalOrder -= price;
                        updateOrderItem(productId, productName, price, currentValue - 1);
                        updateTotal();
                    }
                });
            }

            if (plusBtn) {
                plusBtn.addEventListener('click', () => {
                    let currentValue = parseInt(quantityInput.value);
                    quantityInput.value = currentValue + 1;
                    totalOrder += price;
                    updateOrderItem(productId, productName, price, currentValue + 1);
                    updateTotal();
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
    const orderComments = document.getElementById('order-comments');
    
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

    // Send order function
    function sendOrder() {
        if (orderItems.length === 0) {
            showNotification('❌ No hay productos en el pedido', 'error');
            return;
        }

        const comments = orderComments ? orderComments.value : '';
        const mesaId = confirmBtn ? confirmBtn.getAttribute('data-mesa') : '1';

        // Simular envío del pedido
        const orderData = {
            mesaId: mesaId,
            items: orderItems,
            total: totalOrder,
            comments: comments,
            timestamp: new Date().toISOString()
        };

        console.log('Pedido enviado:', orderData);
        
        showNotification('✅ Pedido enviado correctamente a la cocina!');
        orderModal.style.display = 'none';
        
        // Reset order
        resetOrder();
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
	
	// fusionMesero.js - FUNCIÓN sendOrder ACTUALIZADA
	function sendOrder() {
	    if (orderItems.length === 0) {
	        showNotification('❌ No hay productos en el pedido', 'error');
	        return;
	    }

	    const comments = orderComments ? orderComments.value : '';
	    const mesaId = confirmBtn ? confirmBtn.getAttribute('data-mesa') : '1';

	    // Preparar datos para enviar (formato simple)
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

	    // Convertir a JSON string
	    const itemsJson = JSON.stringify(itemsData);

	    // Crear form data para enviar
	    const formData = new URLSearchParams();
	    formData.append('mesaId', parseInt(mesaId));
	    formData.append('itemsJson', itemsJson);
	    formData.append('comentarios', comments);
	    formData.append('total', totalOrder);

	    // Enviar pedido al servidor
	    fetch('/pedidos/crear', {
	        method: 'POST',
	        headers: {
	            'Content-Type': 'application/x-www-form-urlencoded',
	        },
	        body: formData
	    })
	    .then(response => response.json())
	    .then(data => {
	        if (data.success) {
	            showNotification('✅ Pedido enviado correctamente a la cocina!');
	            orderModal.style.display = 'none';
	            
	            // Redirigir a la página de ver pedido
	            setTimeout(() => {
	                window.location.href = '/pedidos/ver?mesa=' + mesaId;
	            }, 1500);
	            
	            resetOrder();
	        } else {
	            showNotification('❌ Error al enviar pedido: ' + data.message, 'error');
	        }
	    })
	    .catch(error => {
	        console.error('Error:', error);
	        showNotification('❌ Error de conexión', 'error');
	    });
	}
	
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