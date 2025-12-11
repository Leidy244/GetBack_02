document.addEventListener('DOMContentLoaded', () => {
    // Botón flotante para volver a mesas
    const backToMesasBtn = document.getElementById('btn-back-to-mesas');
    if (backToMesasBtn) {
        backToMesasBtn.addEventListener('click', () => {
            window.location.href = '/mesero';
        });
    }

    // Mobile navigation toggle
    const menuToggle = document.getElementById('menu-toggle');
    const categoriesToggle = document.getElementById('categories-toggle');
    const navLinks = document.getElementById('nav-links');
    const navOverlay = document.getElementById('nav-overlay');
    const drawerClose = document.getElementById('drawer-close');

    function openCategories() {
        navLinks.classList.add('active');
        navOverlay.classList.add('active');
        document.body.classList.add('categories-open');
        if (menuToggle) menuToggle.classList.add('hidden');
        if (categoriesToggle) categoriesToggle.classList.add('hidden');
    }

    function closeCategories() {
        navLinks.classList.remove('active');
        navOverlay.classList.remove('active');
        document.body.classList.remove('categories-open');
        if (menuToggle) menuToggle.classList.remove('hidden');
        if (categoriesToggle) categoriesToggle.classList.remove('hidden');
    }

    if (navLinks && navOverlay) {
        if (menuToggle) menuToggle.addEventListener('click', openCategories);
        if (categoriesToggle) categoriesToggle.addEventListener('click', openCategories);
        if (drawerClose) drawerClose.addEventListener('click', closeCategories);
        navOverlay.addEventListener('click', closeCategories);
        document.addEventListener('click', (e) => {
            if (navLinks.classList.contains('active') && !navLinks.contains(e.target) && !menuToggle?.contains(e.target) && !categoriesToggle?.contains(e.target)) {
                closeCategories();
            }
        });
    }

    // Modal functionality
    const orderModal = document.getElementById('orderModal');
    const closeBtn = orderModal?.querySelector('.close-modal');
    const cancelBtn = orderModal?.querySelector('.btn-cancel');
    const confirmBtn = orderModal?.querySelector('.btn-confirm');
    const orderBtns = document.querySelectorAll('.btn-order');

    // Quantity controls and order total
    const quantityControls = document.querySelectorAll('.quantity-control');
    const pedidoTotalValue = document.getElementById('pedido-total-value');
    let totalOrder = 0;
    let orderItems = [];
    let isPreparing = false;
    let preparingOverlay = null;

    // Helper: formatear montos sin .00, con separador de miles usando punto
    const formatMonto = (valor) => {
        const numero = Number(valor) || 0;
        const entero = Math.round(numero);
        return entero.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    };

    const escapeHtml = (s) => String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');

    // ✅ DECLARAR updateTotal ANTES de cualquier uso
    const updateTotal = () => {
        if (pedidoTotalValue) {
            pedidoTotalValue.textContent = `$${formatMonto(totalOrder)}`;
        }
    };

    if (orderModal && orderBtns && orderBtns.length) {
        orderBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                orderModal.style.display = 'flex';
                updateOrderSummary();
            });
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
            if (isPreparing) return;
            confirmBtn.disabled = true;
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
    const commentsModal = document.getElementById('commentsModal');
    const commentsCloseBtn = commentsModal?.querySelector('.close-modal');
    const commentsCancelBtn = commentsModal?.querySelector('.btn-cancel');
    const commentsSaveBtn = commentsModal?.querySelector('.btn-save');

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
                updateTotal(); // ✅ Ahora funciona correctamente

                // Cargar comentarios del borrador si existen
                if (typeof draftComentarios !== 'undefined' && draftComentarios && orderComments) {
                    orderComments.value = draftComentarios;
                }
            }
        }
    } catch (e) {
        console.error('Error al inicializar borrador de pedido:', e);
    }

    // Setup quantity controls
    quantityControls.forEach(control => {
        const minusBtn = control.querySelector('.minus');
        const plusBtn = control.querySelector('.plus');
        const quantityInput = control.querySelector('.quantity-input');
        const addToOrderBtn = control.querySelector('.btn-add-to-order');
        const productItem = control.closest('.product-item');
        const priceElement = productItem?.querySelector('.product-price');
        const area = productItem?.getAttribute('data-area') || '';
        const stockAttr = productItem?.getAttribute('data-stock');
        const stock = stockAttr !== null ? parseInt(stockAttr) : -1;
        const isBarProduct = area.toLowerCase() === 'bar';

        if (isBarProduct && (isNaN(stock) || stock <= 0)) {
            if (productItem) {
                productItem.classList.add('agotado');
            }
            if (minusBtn) minusBtn.disabled = true;
            if (plusBtn) plusBtn.disabled = true;
            if (addToOrderBtn) addToOrderBtn.disabled = true;
            return;
        }

        if (priceElement) {
            const price = parseFloat(priceElement.textContent.replace('$', '').replace('.', '').replace(',', '.'));
            const productId = productItem.getAttribute('data-producto-id');
            const productName = productItem.querySelector('.product-name')?.textContent || 'Producto';

            if (minusBtn) {
                minusBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    let currentValue = parseInt(quantityInput.value);
                    if (currentValue > 0) {
                        const newQuantity = currentValue - 1;
                        quantityInput.value = newQuantity;
                        updateOrderItem(productId, productName, price, newQuantity);
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
                    e.stopPropagation();
                    let currentValue = parseInt(quantityInput.value);

                    if (isBarProduct && stock >= 0 && currentValue >= stock) {
                        showNotification(`Stock máximo alcanzado para ${productName} (${stock} uds)`, 'warning');
                        return;
                    }

                    const newQuantity = currentValue + 1;
                    quantityInput.value = newQuantity;
                    updateOrderItem(productId, productName, price, newQuantity);
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
                        showNotification(` Agregado: ${quantity} x ${productName}`);
                    } else {
                        showNotification('Por favor selecciona al menos 1 cantidad', 'warning');
                    }
                });
            }
        }
    });

    // Click on product to add functionality
    const clickableProducts = document.querySelectorAll('.clickable-product');
    clickableProducts.forEach(product => {
        const productInfo = product.querySelector('.product-info');
        if (productInfo) {
            productInfo.addEventListener('click', (e) => {
                if (e.target.closest('.quantity-control')) {
                    return;
                }

                const area = product.getAttribute('data-area') || '';
                const stockAttr = product.getAttribute('data-stock');
                const stock = stockAttr !== null ? parseInt(stockAttr) : -1;
                const isBarProduct = area.toLowerCase() === 'bar';

                const productId = product.getAttribute('data-producto-id');
                const productName = product.getAttribute('data-producto-nombre');
                const productPrice = parseFloat(product.getAttribute('data-precio'));

                const existingItem = orderItems.find(item => item.id === productId);
                let newQuantity = existingItem ? existingItem.quantity + 1 : 1;

                if (isBarProduct && stock >= 0 && newQuantity > stock) {
                    showNotification(`Stock máximo alcanzado para ${productName} (${stock} uds)`, 'warning');
                    return;
                }

                updateOrderItem(productId, productName, productPrice, newQuantity);
                totalOrder = orderItems.reduce((sum, item) => sum + item.subtotal, 0);
                updateTotal();
                showNotification(` Agregado: ${newQuantity} x ${productName}`);

                const quantityInput = product.querySelector('.quantity-input');
                if (quantityInput) {
                    quantityInput.value = newQuantity;
                }
            });
        }
    });

    function updateOrderItem(productId, productName, price, quantity) {
        const existingItemIndex = orderItems.findIndex(item => item.id === productId);

        if (quantity === 0) {
            if (existingItemIndex !== -1) {
                orderItems.splice(existingItemIndex, 1);
            }
        } else {
            if (existingItemIndex !== -1) {
                orderItems[existingItemIndex].quantity = quantity;
                orderItems[existingItemIndex].subtotal = price * quantity;
            } else {
                const el = document.querySelector(`.product-item[data-producto-id="${productId}"]`);
                const area = el ? (el.getAttribute('data-area') || '') : '';
                orderItems.push({
                    id: productId,
                    name: productName,
                    price: price,
                    quantity: quantity,
                    subtotal: price * quantity,
                    area: area
                });
            }
        }
    }

    // Reconstruye la lista de items directamente desde el DOM según las cantidades seleccionadas
    function rebuildOrderItemsFromDom() {
        const newItems = [];
        const products = document.querySelectorAll('.product-item');
        products.forEach(product => {
            const quantityInput = product.querySelector('.quantity-input');
            const quantity = quantityInput ? parseInt(quantityInput.value) || 0 : 0;
            if (quantity > 0) {
                const productId = product.getAttribute('data-producto-id');
                const productName = product.getAttribute('data-producto-nombre') ||
                    (product.querySelector('.product-name')?.textContent || 'Producto');
                const price = parseFloat(product.getAttribute('data-precio')) ||
                    parseFloat(product.querySelector('.product-price')?.textContent.replace('$', '').replace('.', '').replace(',', '.') || '0');
                const area = product.getAttribute('data-area') || '';
                newItems.push({
                    id: String(productId),
                    name: productName,
                    price: price,
                    quantity: quantity,
                    subtotal: price * quantity,
                    area: area
                });
            }
        });
        orderItems = newItems;
        totalOrder = orderItems.reduce((sum, item) => sum + item.subtotal, 0);
        updateTotal();
    }

    // ❌ ELIMINAR esta declaración duplicada de updateTotal (está en las líneas ~250-256)
    // Filter functionality by category
    const filterButtons = document.querySelectorAll('.nav-links .filter-btn');
    const productItems = document.querySelectorAll('.product-item');

    filterButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            filterButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const categoriaId = btn.getAttribute('data-categoria');
            filterProductsByCategory(categoriaId);

            // Cerrar el panel de categorías en modo responsivo al seleccionar
            if (document.body.classList.contains('categories-open') || navLinks.classList.contains('active')) {
                closeCategories();
            }
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

            const noProductsMessage = document.getElementById('no-products');
            if (noProductsMessage) {
                noProductsMessage.style.display = hasVisibleProducts ? 'none' : 'block';
            }
        });
    }

    // Comments modal functionality
    const commentsBtn = document.getElementById('btn-comments');
    if (commentsBtn && commentsModal) {
        commentsBtn.addEventListener('click', (e) => {
            e.preventDefault();
            commentsModal.style.display = 'flex';
        });

        if (commentsCloseBtn) {
            commentsCloseBtn.addEventListener('click', () => {
                commentsModal.style.display = 'none';
            });
        }

        if (commentsCancelBtn) {
            commentsCancelBtn.addEventListener('click', () => {
                commentsModal.style.display = 'none';
            });
        }

        if (commentsSaveBtn) {
            commentsSaveBtn.addEventListener('click', () => {
                commentsModal.style.display = 'none';
                showNotification('✅ Comentario guardado');
            });
        }

        window.addEventListener('click', (event) => {
            if (event.target === commentsModal) {
                commentsModal.style.display = 'none';
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

        const commentText = orderComments ? orderComments.value.trim() : '';

        let summaryHTML = `
            <div class="order-items">
                <h4>Resumen del Pedido:</h4>
                <div class="order-items-list">
        `;

        orderItems.forEach(item => {
            summaryHTML += `
                <div class="order-item">
                    <span class="item-name">${item.quantity} x ${item.name}</span>
                    <span class="item-subtotal">$${formatMonto(item.subtotal)}</span>
                </div>
            `;
        });

        const commentSection = commentText ? `
                </div>
                <div class="order-comment-modal">
                    <strong>Comentarios:</strong>
                    <div class="comment-text">${escapeHtml(commentText)}</div>
                </div>
        ` : `
                </div>
                <div class="order-total-modal">
                    <strong>Total: $${formatMonto(totalOrder)}</strong>
                </div>
            </div>
        `;

        summaryHTML += commentSection;

        if (commentText) {
            summaryHTML += `
                <div class="order-total-modal">
                    <strong>Total: $${formatMonto(totalOrder)}</strong>
                </div>
            `;
        }

        orderSummary.innerHTML = summaryHTML;
    }

    function sendOrder() {
        if (isPreparing) return;
        // Antes de enviar, reconstruir la lista de items desde el DOM para asegurar coherencia
        rebuildOrderItemsFromDom();

        if (orderItems.length === 0) {
            showNotification('❌ No hay productos en el pedido', 'error');
            return;
        }

        const comments = orderComments ? orderComments.value : '';
        const mesaId = confirmBtn ? confirmBtn.getAttribute('data-mesa') : '1';

        totalOrder = orderItems.reduce((sum, item) => sum + item.subtotal, 0);

        const itemsData = {
            items: orderItems.map(item => ({
                productoId: parseInt(item.id),
                productoNombre: item.name,
                cantidad: item.quantity,
                precio: item.price,
                subtotal: item.price * item.quantity,
                comentarios: '',
                tipo: (item.area || '').toUpperCase()
            })),
            total: totalOrder
        };

        const itemsJson = JSON.stringify(itemsData);

        const proceed = () => {
            const controller = new AbortController();
            const t = setTimeout(() => { try{ controller.abort(); }catch(e){} }, 15000);
            if (!preparingOverlay) {
                preparingOverlay = document.createElement('div');
                preparingOverlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.6);display:flex;align-items:center;justify-content:center;z-index:10000';
                const box = document.createElement('div');
                box.style.cssText = 'background:#111;border:1px solid rgba(255,255,255,.1);border-radius:12px;padding:16px 20px;color:#fff;text-align:center;min-width:280px';
                const txt = document.createElement('div');
                txt.textContent = 'Preparando pedido...';
                txt.style.cssText = 'margin-bottom:10px';
                const spinner = document.createElement('div');
                spinner.style.cssText = 'width:24px;height:24px;border:3px solid #fff;border-top-color:transparent;border-radius:50%;margin:0 auto;animation:spin .8s linear infinite';
                box.appendChild(txt); box.appendChild(spinner); preparingOverlay.appendChild(box);
                const style = document.createElement('style');
                style.textContent = '@keyframes spin{to{transform:rotate(360deg)}}';
                preparingOverlay.appendChild(style);
                document.body.appendChild(preparingOverlay);
            }
            const formData = new URLSearchParams();
            formData.append('mesaId', parseInt(mesaId));
            formData.append('itemsJson', itemsJson);
            formData.append('comentarios', comments);
            formData.append('total', totalOrder);
            fetch('/pedidos/preparar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData,
                signal: controller.signal
            })
                .then(response => response.json())
                .then(data => {
                    clearTimeout(t);
                    if (data.success) {
                        showNotification('✅ Pedido preparado. Redirigiendo a vista de confirmación...');
                        if (preparingOverlay) { try{ document.body.removeChild(preparingOverlay); }catch(e){} preparingOverlay = null; }
                        orderModal.style.display = 'none';
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
                    clearTimeout(t);
                    console.error('Error:', error);
                    showNotification('❌ Error de conexión', 'error');
                })
                .finally(() => {
                    isPreparing = false;
                    if (confirmBtn) confirmBtn.disabled = false;
                    if (preparingOverlay) { try{ document.body.removeChild(preparingOverlay); }catch(e){} preparingOverlay = null; }
                });
        };
        isPreparing = true;
        proceed();
    }

    function resetOrder() {
        orderItems = [];
        totalOrder = 0;
        updateTotal();

        quantityControls.forEach(control => {
            const quantityInput = control.querySelector('.quantity-input');
            if (quantityInput) quantityInput.value = 0;
        });

        if (orderComments) orderComments.value = '';
        if (commentsModal) commentsModal.style.display = 'none';
    }

    // Notification system
    function showNotification(message, type = 'success') {
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
        .order-comment-modal { margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.15); }
        .order-comment-modal strong { display: block; margin-bottom: 6px; color: var(--accent); }
        .comment-text { background: rgba(0,0,0,0.2); border: 1px solid var(--primary-light); border-radius: 8px; padding: 8px; color: var(--text-primary); white-space: pre-wrap; }
    `;
    document.head.appendChild(style);

    // User dropdown functionality
    const userDropdownBtn = document.getElementById("userDropdown");
    const dropdownMenu = document.querySelector(".user-dropdown .dropdown-menu");

    if (userDropdownBtn && dropdownMenu) {
        userDropdownBtn.addEventListener("click", function(e) {
            e.preventDefault();
            e.stopPropagation();
            dropdownMenu.classList.toggle("show");
        });

        document.addEventListener("click", function(e) {
            if (!dropdownMenu.contains(e.target) && !userDropdownBtn.contains(e.target)) {
                dropdownMenu.classList.remove("show");
            }
        });

        document.addEventListener("keydown", function(e) {
            if (e.key === "Escape") {
                dropdownMenu.classList.remove("show");
            }
        });
    }

    // Apply color theme
    try {
        const saved = JSON.parse(localStorage.getItem('meseroSettings')) || {};
        const theme = saved.theme || 'default';

        if (theme && theme !== 'default') {
            document.documentElement.setAttribute('data-color', theme);
        } else {
            document.documentElement.removeAttribute('data-color');
        }
    } catch (e) {
        console.warn('No se pudo aplicar el tema del mesero en Fusion Mesero', e);
    }
});
