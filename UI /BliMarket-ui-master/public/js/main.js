// Main JavaScript for BliMarket UI
// This file contains common functions and event handlers

document.addEventListener('DOMContentLoaded', () => {
    // Initialize mobile menu
    initMobileMenu();
    
    // Initialize dropdown
    initDropdown();
    
    // Initialize toast notifications
    initNotifications();
});

/**
 * Mobile Menu Toggle
 */
function initMobileMenu() {
    const toggle = document.getElementById('mobileMenuToggle');
    const menu = document.getElementById('mobileMenu');
    
    if (toggle && menu) {
        toggle.addEventListener('click', () => {
            menu.classList.toggle('active');
            toggle.querySelector('i').classList.toggle('fa-bars');
            toggle.querySelector('i').classList.toggle('fa-times');
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', (e) => {
            if (!toggle.contains(e.target) && !menu.contains(e.target)) {
                menu.classList.remove('active');
                toggle.querySelector('i').classList.add('fa-bars');
                toggle.querySelector('i').classList.remove('fa-times');
            }
        });
    }
}

/**
 * User Dropdown Toggle
 */
function initDropdown() {
    const dropdownToggle = document.getElementById('userDropdown');
    const dropdownMenu = document.getElementById('userDropdownMenu');
    
    if (dropdownToggle && dropdownMenu) {
        dropdownToggle.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdownMenu.classList.toggle('active');
        });
        
        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!dropdownToggle.contains(e.target) && !dropdownMenu.contains(e.target)) {
                dropdownMenu.classList.remove('active');
            }
        });
    }
}

/**
 * Initialize notification system
 */
function initNotifications() {
    // Create notification container if it doesn't exist
    if (!document.getElementById('notificationContainer')) {
        const container = document.createElement('div');
        container.id = 'notificationContainer';
        container.className = 'notification-container';
        document.body.appendChild(container);
    }
}

/**
 * Show a toast notification
 * @param {string} message - The message to display
 * @param {string} type - The type of notification (success, error, warning, info)
 * @param {number} duration - How long to show the notification in milliseconds
 */
window.showNotification = function(message, type = 'success', duration = 3000) {
    const container = document.getElementById('notificationContainer') || document.body;
    
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    
    const icons = {
        success: 'fa-check-circle',
        error: 'fa-exclamation-circle',
        warning: 'fa-exclamation-triangle',
        info: 'fa-info-circle'
    };
    
    notification.innerHTML = `
        <i class="fas ${icons[type] || icons.info}"></i>
        <span>${message}</span>
        <button class="notification-close">
            <i class="fas fa-times"></i>
        </button>
    `;
    
    container.appendChild(notification);
    
    // Close button handler
    const closeBtn = notification.querySelector('.notification-close');
    closeBtn.addEventListener('click', () => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    });
    
    // Show notification
    setTimeout(() => notification.classList.add('show'), 10);
    
    // Auto-remove after duration
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, duration);
};

/**
 * Show a loading spinner
 * @param {string} message - Optional loading message
 */
window.showLoading = function(message = 'Loading...') {
    const existing = document.getElementById('loadingOverlay');
    if (existing) return;
    
    const overlay = document.createElement('div');
    overlay.id = 'loadingOverlay';
    overlay.className = 'loading-overlay';
    overlay.innerHTML = `
        <div class="loading-spinner">
            <i class="fas fa-spinner fa-spin"></i>
            <p>${message}</p>
        </div>
    `;
    document.body.appendChild(overlay);
    setTimeout(() => overlay.classList.add('show'), 10);
};

/**
 * Hide the loading spinner
 */
window.hideLoading = function() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.classList.remove('show');
        setTimeout(() => overlay.remove(), 300);
    }
};

/**
 * Format currency
 * @param {number} amount - The amount to format
 * @param {string} currency - Currency code (default: USD)
 */
window.formatCurrency = function(amount, currency = 'USD') {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currency
    }).format(amount);
};

/**
 * Debounce function for search inputs
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in milliseconds
 */
window.debounce = function(func, wait = 300) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
};

/**
 * Smooth scroll to element
 * @param {string} selector - CSS selector of target element
 */
window.smoothScrollTo = function(selector) {
    const element = document.querySelector(selector);
    if (element) {
        element.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    }
};

/**
 * Validate email format
 * @param {string} email - Email to validate
 */
window.isValidEmail = function(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
};

/**
 * Copy text to clipboard
 * @param {string} text - Text to copy
 */
window.copyToClipboard = function(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
            showNotification('Copied to clipboard!', 'success');
        }).catch(err => {
            console.error('Failed to copy:', err);
            showNotification('Failed to copy', 'error');
        });
    } else {
        // Fallback for older browsers
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            showNotification('Copied to clipboard!', 'success');
        } catch (err) {
            console.error('Failed to copy:', err);
            showNotification('Failed to copy', 'error');
        }
        document.body.removeChild(textarea);
    }
};

/**
 * API Helper Functions
 */
window.api = {
    /**
     * Make a GET request
     */
    get: async (url) => {
        try {
            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            return await response.json();
        } catch (error) {
            console.error('API GET error:', error);
            throw error;
        }
    },
    
    /**
     * Make a POST request
     */
    post: async (url, data) => {
        try {
            const response = await fetch(url, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            return await response.json();
        } catch (error) {
            console.error('API POST error:', error);
            throw error;
        }
    },
    
    /**
     * Make a PUT request
     */
    put: async (url, data) => {
        try {
            const response = await fetch(url, {
                method: 'PUT',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            return await response.json();
        } catch (error) {
            console.error('API PUT error:', error);
            throw error;
        }
    },
    
    /**
     * Make a DELETE request
     */
    delete: async (url) => {
        try {
            const response = await fetch(url, {
                method: 'DELETE',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            return await response.json();
        } catch (error) {
            console.error('API DELETE error:', error);
            throw error;
        }
    }
};

/**
 * Local Storage Helper
 */
window.storage = {
    set: (key, value) => {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (e) {
            console.error('LocalStorage set error:', e);
        }
    },
    
    get: (key) => {
        try {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : null;
        } catch (e) {
            console.error('LocalStorage get error:', e);
            return null;
        }
    },
    
    remove: (key) => {
        try {
            localStorage.removeItem(key);
        } catch (e) {
            console.error('LocalStorage remove error:', e);
        }
    },
    
    clear: () => {
        try {
            localStorage.clear();
        } catch (e) {
            console.error('LocalStorage clear error:', e);
        }
    }
};

/**
 * Handle form validation
 */
window.validateForm = function(formId, rules) {
    const form = document.getElementById(formId);
    if (!form) return false;
    
    let isValid = true;
    
    // Clear previous errors
    form.querySelectorAll('.form-error').forEach(el => el.textContent = '');
    form.querySelectorAll('.form-input').forEach(el => el.classList.remove('error'));
    
    // Validate each field
    Object.keys(rules).forEach(fieldName => {
        const field = form.querySelector(`[name="${fieldName}"]`);
        const rule = rules[fieldName];
        const value = field?.value?.trim();
        
        if (rule.required && !value) {
            showFieldError(field, rule.message || 'This field is required');
            isValid = false;
        } else if (rule.minLength && value.length < rule.minLength) {
            showFieldError(field, `Minimum ${rule.minLength} characters required`);
            isValid = false;
        } else if (rule.maxLength && value.length > rule.maxLength) {
            showFieldError(field, `Maximum ${rule.maxLength} characters allowed`);
            isValid = false;
        } else if (rule.pattern && !rule.pattern.test(value)) {
            showFieldError(field, rule.message || 'Invalid format');
            isValid = false;
        } else if (rule.email && !isValidEmail(value)) {
            showFieldError(field, 'Invalid email address');
            isValid = false;
        } else if (rule.custom && !rule.custom(value)) {
            showFieldError(field, rule.message || 'Validation failed');
            isValid = false;
        }
    });
    
    return isValid;
};

function showFieldError(field, message) {
    if (!field) return;
    field.classList.add('error');
    const errorEl = field.parentElement.querySelector('.form-error');
    if (errorEl) {
        errorEl.textContent = message;
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        showNotification,
        showLoading,
        hideLoading,
        formatCurrency,
        debounce,
        smoothScrollTo,
        isValidEmail,
        copyToClipboard,
        api,
        storage,
        validateForm
    };
}
