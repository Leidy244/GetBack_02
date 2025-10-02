function togglePassword() {
    const passwordInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eye-icon');
    
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        eyeIcon.name = 'eye';
    } else {
        passwordInput.type = 'password';
        eyeIcon.name = 'eye-off';
    }
}
document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form');
    
    form.addEventListener('submit', function(e) {
        const email = form.querySelector('input[type="email"]').value;
        const password = form.querySelector('input[type="password"]').value;
        
        if (!email || !password) {
            e.preventDefault();
            alert('Por favor, complete todos los campos');
            return;
        }
     
    });
    
    const loginContainer = document.querySelector('.login-container');
    loginContainer.style.opacity = '0';
    loginContainer.style.transform = 'translateY(20px)';
    
    setTimeout(() => {
        loginContainer.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        loginContainer.style.opacity = '1';
        loginContainer.style.transform = 'translateY(0)';
    }, 100);
    
    const themeToggle = document.getElementById('input-check');
    themeToggle.addEventListener('change', function() {
        localStorage.setItem('theme', this.checked ? 'purple' : 'blue');
    });
    
  
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'purple') {
        themeToggle.checked = true;
    }
});