/**
 * 首页脚本 - 主题切换
 */

function initTheme() {
    const savedTheme = localStorage.getItem('algoviz-theme') || 'light';
    setTheme(savedTheme);
}

function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('algoviz-theme', theme);
    
    const btn = document.getElementById('themeToggle');
    if (btn) {
        // Updated to use new theme switch structure
        const icon = btn.querySelector('.theme-icon');
        if (icon) {
            icon.textContent = theme === 'dark' ? '🌙' : '☀️';
        }
    }
}

function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    const next = current === 'dark' ? 'light' : 'dark';
    setTheme(next);
}

document.addEventListener('DOMContentLoaded', function() {
    initTheme();
    initAnimations();
    
    // Initialize profile click handling globally
    const profileLink = document.getElementById('profileLink');
    if (profileLink) {
        profileLink.addEventListener('click', function(e) {
            e.preventDefault();
            const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
            if (isLoggedIn) {
                window.location.href = '/pages/profile.html';
            } else {
                window.location.href = '/pages/login.html';
            }
        });
    }
});

function initAnimations() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(function(entry) {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    document.querySelectorAll('.card, .algo-category, .ds-selector').forEach(function(el) {
        observer.observe(el);
    });
}
