const express = require('express');
const router = express.Router();
const axios = require('axios');

// Middleware to check authentication
const isAuthenticated = (req, res, next) => {
    const token = req.cookies.accessToken;
    if (!token) {
        return res.redirect('/login?redirect=' + req.originalUrl);
    }
    req.token = token;
    next();
};

// Middleware to pass user info to templates
const getUserInfo = (req, res, next) => {
    const token = req.cookies.accessToken;
    if (token) {
        try {
            // Decode JWT payload (basic decoding, no verification here)
            const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
            res.locals.user = {
                email: payload.email,
                role: payload.role,
                memberId: payload.sub
            };
            res.locals.isAuthenticated = true;
        } catch (e) {
            res.locals.isAuthenticated = false;
        }
    } else {
        res.locals.isAuthenticated = false;
    }
    next();
};

// Apply user info middleware to all routes
router.use(getUserInfo);

// ============================================
// Public Routes
// ============================================

// Home page - Product Catalog
router.get('/', async (req, res, next) => {
    try {
        const page = parseInt(req.query.page) || 0;
        const size = parseInt(req.query.size) || 20;
        let sort = req.query.sort || 'name,asc';
        const name = req.query.name || '';
        const category = req.query.category || '';

        // Fix price sorting - products have variants with prices
        // Backend might need variants.price or we handle client-side
        let sortParam = sort;
        let products = [];
        
        console.log('Fetching products from:', `${process.env.PRODUCT_SERVICE_URL}/api/v1/products`);
        console.log('Sort parameter:', sortParam);

        const response = await axios.get(`${process.env.PRODUCT_SERVICE_URL}/api/v1/products`, {
            params: { name, category, page, size, sort: sortParam }
        });
        
        // Ensure products is always an array (even if empty)
        products = Array.isArray(response.data.data?.content) ? response.data.data.content : [];
        
        console.log('Products fetched:', products.length, 'items');
        console.log('Is array?', Array.isArray(products));
        console.log('Products value:', JSON.stringify(products));
        
        // If price sorting and backend didn't sort correctly, sort client-side
        if (sort.startsWith('price') && products.length > 0) {
            products.sort((a, b) => {
                const priceA = a.variants && a.variants.length > 0 ? a.variants[0].price : 0;
                const priceB = b.variants && b.variants.length > 0 ? b.variants[0].price : 0;
                
                if (sort === 'price,asc') {
                    return priceA - priceB;
                } else if (sort === 'price,desc') {
                    return priceB - priceA;
                }
                return 0;
            });
        }

        // Always render with products array (even if empty) to show empty state instead of loading
        // Ensure products is explicitly an array to avoid Handlebars truthiness issues
        const productsArray = Array.isArray(products) ? products : [];
        
        console.log('Rendering with products array length:', productsArray.length);
        console.log('Products array type:', typeof productsArray, 'IsArray:', Array.isArray(productsArray));
        
        // Always set hasProducts explicitly - this is the key to fixing the loading issue
        const hasProducts = productsArray.length > 0;
        
        console.log('Final render - hasProducts:', hasProducts, 'productsArray.length:', productsArray.length);
        
        res.render('pages/home', {
            title: 'BliMarket - Shop Smart, Live Better',
            products: productsArray, // Always pass array (even if empty)
            hasProducts: hasProducts, // Explicit boolean flag - false when empty
            pagination: {
                currentPage: response.data.data?.number || 0,
                totalPages: response.data.data?.totalPages || 0,
                totalElements: response.data.data?.totalElements || 0,
                size: response.data.data?.size || size
            },
            search: { name, category, sort }
        });
    } catch (error) {
        console.error('Error fetching products:', error.message);
        res.render('pages/home', {
            title: 'BliMarket - Shop Smart, Live Better',
            products: [],
            error: 'Unable to load products. Please try again later.'
        });
    }
});

// Product detail page
router.get('/product/:id', async (req, res, next) => {
    try {
        // Try both productId and id formats
        let response;
        try {
            response = await axios.get(`${process.env.PRODUCT_SERVICE_URL}/api/v1/products/${req.params.id}`);
        } catch (err) {
            // If direct ID fails, try to find by productId
            const allProducts = await axios.get(`${process.env.PRODUCT_SERVICE_URL}/api/v1/products?size=1000`);
            const product = allProducts.data.data.content.find(p => 
                p.id === req.params.id || p.productId === req.params.id
            );
            if (product) {
                response = { data: { data: product } };
            } else {
                throw err;
            }
        }
        
        if (response && response.data && response.data.data) {
            res.render('pages/product-detail', {
                title: response.data.data.name || 'Product Details',
                product: response.data.data
            });
        } else {
            throw new Error('Product not found');
        }
    } catch (error) {
        console.error('Error fetching product:', error.message);
        res.status(404).render('pages/error', {
            title: 'Product Not Found',
            error: {
                status: 404,
                message: 'The product you are looking for does not exist. Please check the product ID and try again.'
            }
        });
    }
});

// Login page
router.get('/login', (req, res) => {
    if (res.locals.isAuthenticated) {
        return res.redirect('/');
    }
    res.render('pages/login', {
        title: 'Login - BliMarket',
        redirect: req.query.redirect || '/'
    });
});

// Register page
router.get('/register', (req, res) => {
    if (res.locals.isAuthenticated) {
        return res.redirect('/');
    }
    res.render('pages/register', {
        title: 'Register - BliMarket'
    });
});

// ============================================
// Protected Routes (Require Authentication)
// ============================================

// Shopping Cart - Show login modal for guests, show cart for authenticated users
router.get('/cart', async (req, res, next) => {
    // ENSURE PORT 8089
    const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
    const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
    
    const token = req.cookies.accessToken;
    const guestCartId = req.cookies.guestCartId;
    
    // If not authenticated, show cart page with login prompt
    if (!token) {
        let guestCart = { items: [], totalItems: 0, totalValue: 0 };
        
        // Try to load guest cart if exists
        if (guestCartId) {
            try {
                const response = await axios.get(`${finalUrl}/api/v1/cart`, {
                    headers: { 'X-Guest-Cart-Id': guestCartId }
                });
                guestCart = response.data.data || response.data || guestCart;
            } catch (error) {
                console.log('Guest cart not found or error:', error.message);
            }
        }
        
        return res.render('pages/cart', {
            title: 'Shopping Cart - BliMarket',
            cart: guestCart,
            requiresLogin: true,
            guestCartId: guestCartId
        });
    }
    
    // Authenticated user - load their cart
    try {
        const response = await axios.get(`${finalUrl}/api/v1/cart`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        const cart = response.data.data || response.data;
        
        res.render('pages/cart', {
            title: 'Shopping Cart - BliMarket',
            cart: cart || { items: [], totalItems: 0, totalValue: 0 },
            requiresLogin: false
        });
    } catch (error) {
        console.error('Error fetching cart:', error.message);
        res.render('pages/cart', {
            title: 'Shopping Cart - BliMarket',
            cart: { items: [], totalItems: 0, totalValue: 0 },
            error: 'Unable to load cart. Please try again later.',
            requiresLogin: false
        });
    }
});

// User Profile
router.get('/profile', isAuthenticated, async (req, res, next) => {
    try {
        const memberId = res.locals.user.memberId;
        const response = await axios.get(`${process.env.MEMBER_SERVICE_URL}/api/v1/members/${memberId}`, {
            headers: { 'Authorization': `Bearer ${req.token}` }
        });

        res.render('pages/profile', {
            title: 'My Profile - BliMarket',
            member: response.data.data
        });
    } catch (error) {
        console.error('Error fetching profile:', error.message);
        res.render('pages/profile', {
            title: 'My Profile - BliMarket',
            error: 'Unable to load profile. Please try again later.'
        });
    }
});

// Logout API
router.post('/api/auth/logout', async (req, res) => {
    try {
        const token = req.cookies.accessToken;
        const refreshToken = req.cookies.refreshToken;
        
        if (!token || !refreshToken) {
            // Clear cookies even if tokens are missing
            res.clearCookie('accessToken');
            res.clearCookie('refreshToken');
            return res.json({ success: true, message: 'Logged out successfully' });
        }
        
        // ENSURE PORT 8089
        const memberServiceUrl = process.env.MEMBER_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = memberServiceUrl.includes(':8089') ? memberServiceUrl : 'http://localhost:8089';
        
        console.log('Logout API called - Using URL:', `${finalUrl}/api/v1/auth/logout`);
        
        // Call logout API with Authorization header and refreshToken in body
        await axios.post(
            `${finalUrl}/api/v1/auth/logout`,
            { refreshToken: refreshToken },
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }
        );
        
        // Clear cookies after successful API call
        res.clearCookie('accessToken');
        res.clearCookie('refreshToken');
        res.clearCookie('guestCartId');
        
        res.json({ success: true, message: 'Logged out successfully' });
    } catch (error) {
        console.error('Logout error:', error.response?.data || error.message);
        // Clear cookies even if API call fails
        res.clearCookie('accessToken');
        res.clearCookie('refreshToken');
        res.clearCookie('guestCartId');
        res.json({ success: true, message: 'Logged out successfully' });
    }
});

// Logout route (redirects to home after logout)
router.get('/logout', async (req, res) => {
    const token = req.cookies.accessToken;
    const refreshToken = req.cookies.refreshToken;
    
    if (token && refreshToken) {
        try {
            // ENSURE PORT 8089
            const memberServiceUrl = process.env.MEMBER_SERVICE_URL || 'http://localhost:8089';
            const finalUrl = memberServiceUrl.includes(':8089') ? memberServiceUrl : 'http://localhost:8089';
            
            // Call logout API
            await axios.post(
                `${finalUrl}/api/v1/auth/logout`,
                { refreshToken: refreshToken },
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                }
            );
        } catch (error) {
            console.error('Logout API error (non-critical):', error.message);
        }
    }
    
    // Always clear cookies and redirect
    res.clearCookie('accessToken');
    res.clearCookie('refreshToken');
    res.clearCookie('guestCartId');
    res.redirect('/');
});

// ============================================
// API Proxy Routes
// ============================================

// Login API
router.post('/api/auth/login', async (req, res) => {
    try {
        const response = await axios.post(`${process.env.MEMBER_SERVICE_URL}/api/v1/auth/login`, req.body);
        
        // Set cookies
        res.cookie('accessToken', response.data.data.accessToken, {
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            maxAge: 15 * 60 * 1000 // 15 minutes
        });
        
        res.cookie('refreshToken', response.data.data.refreshToken, {
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            maxAge: 30 * 24 * 60 * 60 * 1000 // 30 days
        });

        // Merge guest cart if exists
        const guestCartId = req.cookies.guestCartId;
        if (guestCartId) {
            try {
                // ENSURE PORT 8089
                const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
                const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
                
                console.log('Merging guest cart after login:', guestCartId);
                
                const mergeResponse = await axios.post(
                    `${finalUrl}/api/v1/cart/merge?guestCartId=${guestCartId}`,
                    {},
                    { headers: { 'Authorization': `Bearer ${response.data.data.accessToken}` } }
                );
                
                console.log('Cart merge successful:', mergeResponse.data);
                
                // Clear guest cart ID after successful merge
                res.clearCookie('guestCartId');
            } catch (mergeError) {
                console.error('Cart merge failed:', mergeError.response?.data || mergeError.message);
                // Don't fail login if merge fails, but log it
            }
        }

        res.json(response.data);
    } catch (error) {
        console.error('Login error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Login failed. Please try again.' 
            }
        );
    }
});

// Register API
router.post('/api/auth/register', async (req, res) => {
    try {
        const response = await axios.post(`${process.env.MEMBER_SERVICE_URL}/api/v1/auth/register`, req.body);
        res.status(201).json(response.data);
    } catch (error) {
        console.error('Register error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Registration failed. Please try again.' 
            }
        );
    }
});

// Forgot Password API (Step 1: Request reset token)
router.post('/api/auth/forgot-password', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const memberServiceUrl = process.env.MEMBER_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = memberServiceUrl.includes(':8089') ? memberServiceUrl : 'http://localhost:8089';
        
        console.log('Forgot Password API called - Using URL:', `${finalUrl}/api/v1/auth/forgot-password`);
        
        const response = await axios.post(
            `${finalUrl}/api/v1/auth/forgot-password`,
            req.body,
            { headers: { 'Content-Type': 'application/json' } }
        );
        
        res.json(response.data);
    } catch (error) {
        console.error('Forgot password error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to send reset token. Please try again.' 
            }
        );
    }
});

// Reset Password API (Step 2: Reset password with token)
router.post('/api/auth/reset-password', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const memberServiceUrl = process.env.MEMBER_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = memberServiceUrl.includes(':8089') ? memberServiceUrl : 'http://localhost:8089';
        
        console.log('Reset Password API called - Using URL:', `${finalUrl}/api/v1/auth/reset-password`);
        
        const response = await axios.post(
            `${finalUrl}/api/v1/auth/reset-password`,
            req.body,
            { headers: { 'Content-Type': 'application/json' } }
        );
        
        res.json(response.data);
    } catch (error) {
        console.error('Reset password error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to reset password. Please try again.' 
            }
        );
    }
});

// Helper to generate guest cart ID
const generateGuestCartId = () => {
    return 'guest-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
};

// Get or create guest cart ID
const getGuestCartId = (req, res) => {
    let guestCartId = req.cookies.guestCartId;
    if (!guestCartId) {
        guestCartId = generateGuestCartId();
        res.cookie('guestCartId', guestCartId, {
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            maxAge: 30 * 24 * 60 * 60 * 1000 // 30 days
        });
    }
    return guestCartId;
};

// Add to cart API - supports both authenticated and guest users
router.post('/api/v1/cart', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
        
        console.log('Cart API called - Using URL:', `${finalUrl}/api/v1/cart`);
        
        const token = req.cookies.accessToken;
        const headers = {};
        let requestBody = { ...req.body };
        
        if (token) {
            // Authenticated user
            headers['Authorization'] = `Bearer ${token}`;
        } else {
            // Guest user - check if we have existing guest cart ID
            let guestCartId = req.cookies.guestCartId;
            
            if (guestCartId) {
                // Second time onwards - pass guest cart ID in memberId field
                requestBody.memberId = guestCartId;
                console.log('Using existing guest cart ID in memberId:', guestCartId);
            } else {
                // First time - let API generate guest cart ID
                console.log('No guest cart ID found, API will generate one');
            }
        }
        
        const response = await axios.post(
            `${finalUrl}/api/v1/cart`,
            requestBody,
            { headers }
        );
        
        // If guest cart, save the cart ID from response (API returns it)
        if (!token && response.data.data) {
            const guestCartId = response.data.data.id || response.data.data.memberId;
            if (guestCartId && guestCartId.startsWith('guest-')) {
                res.cookie('guestCartId', guestCartId, {
                    httpOnly: true,
                    secure: process.env.NODE_ENV === 'production',
                    maxAge: 30 * 24 * 60 * 60 * 1000 // 30 days
                });
                console.log('Stored guest cart ID from API response:', guestCartId);
            }
        }
        
        res.json(response.data);
    } catch (error) {
        console.error('Add to cart error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to add item to cart.' 
            }
        );
    }
});

// Legacy route for backward compatibility
router.post('/api/cart', async (req, res) => {
    // Redirect to v1 endpoint
    req.url = '/api/v1/cart';
    router.handle(req, res);
});

// Get cart API - supports both authenticated and guest
router.get('/api/v1/cart', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
        
        console.log('Get Cart API called - Using URL:', `${finalUrl}/api/v1/cart`);
        
        const token = req.cookies.accessToken;
        const headers = {};
        let params = {};
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        } else {
            // Guest user - pass guest cart ID as memberId query param
            const guestCartId = req.cookies.guestCartId;
            if (guestCartId) {
                params.memberId = guestCartId;
            } else {
                return res.json({ 
                    success: true, 
                    data: { items: [], totalItems: 0, totalValue: 0 } 
                });
            }
        }
        
        const response = await axios.get(
            `${finalUrl}/api/v1/cart`,
            { headers, params }
        );
        
        res.json(response.data);
    } catch (error) {
        console.error('Get cart error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to get cart.' 
            }
        );
    }
});

// Merge cart API - called after login
router.post('/api/v1/cart/merge', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
        
        console.log('Merge Cart API called - Using URL:', `${finalUrl}/api/v1/cart/merge`);
        
        const token = req.cookies.accessToken;
        const guestCartId = req.cookies.guestCartId;
        
        if (!token) {
            return res.status(401).json({ 
                success: false, 
                message: 'Authentication required' 
            });
        }
        
        if (!guestCartId) {
            return res.json({ 
                success: true, 
                message: 'No guest cart to merge' 
            });
        }
        
        const response = await axios.post(
            `${finalUrl}/api/v1/cart/merge?guestCartId=${guestCartId}`,
            {},
            { headers: { 'Authorization': `Bearer ${token}` } }
        );
        
        // Clear guest cart ID after merge
        res.clearCookie('guestCartId');
        
        res.json(response.data);
    } catch (error) {
        console.error('Merge cart error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to merge cart.' 
            }
        );
    }
});

// Update cart item quantity API
router.put('/api/v1/cart/item/:sku', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
        
        console.log('Update Cart Item API called - Using URL:', `${finalUrl}/api/v1/cart/item/${req.params.sku}`);
        
        const token = req.cookies.accessToken;
        const headers = {};
        let requestBody = { ...req.body };
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        } else {
            // Guest user - pass guest cart ID in memberId field
            const guestCartId = req.cookies.guestCartId;
            if (guestCartId) {
                requestBody.memberId = guestCartId;
            }
        }
        
        const response = await axios.put(
            `${finalUrl}/api/v1/cart/item/${req.params.sku}`,
            requestBody,
            { headers }
        );
        res.json(response.data);
    } catch (error) {
        console.error('Update cart error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to update cart item.' 
            }
        );
    }
});

// Legacy route
router.put('/api/cart/item/:sku', async (req, res) => {
    req.url = `/api/v1/cart/item/${req.params.sku}`;
    router.handle(req, res);
});

// Remove from cart API
router.delete('/api/v1/cart/:sku', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
        
        console.log('Remove Cart Item API called - Using URL:', `${finalUrl}/api/v1/cart/${req.params.sku}`);
        
        const token = req.cookies.accessToken;
        const headers = {};
        let params = {};
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        } else {
            // Guest user - pass guest cart ID as memberId query param
            const guestCartId = req.cookies.guestCartId;
            if (guestCartId) {
                params.memberId = guestCartId;
            }
        }
        
        const response = await axios.delete(
            `${finalUrl}/api/v1/cart/${req.params.sku}`,
            { headers, params }
        );
        res.json(response.data);
    } catch (error) {
        console.error('Remove from cart error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to remove item from cart.' 
            }
        );
    }
});

// Legacy route
router.delete('/api/cart/:sku', async (req, res) => {
    req.url = `/api/v1/cart/${req.params.sku}`;
    router.handle(req, res);
});

// Clear cart API
router.delete('/api/v1/cart', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
        
        console.log('Clear Cart API called - Using URL:', `${finalUrl}/api/v1/cart`);
        
        const token = req.cookies.accessToken;
        const headers = {};
        let params = {};
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        } else {
            // Guest user - pass guest cart ID as memberId query param
            const guestCartId = req.cookies.guestCartId;
            if (guestCartId) {
                params.memberId = guestCartId;
            }
        }
        
        const response = await axios.delete(
            `${finalUrl}/api/v1/cart`,
            { headers, params }
        );
        res.json(response.data);
    } catch (error) {
        console.error('Clear cart error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to clear cart.' 
            }
        );
    }
});

// Legacy route
router.delete('/api/cart', async (req, res) => {
    req.url = '/api/v1/cart';
    router.handle(req, res);
});

// Cart count API (for badge) - supports both authenticated and guest
router.get('/cart-count', async (req, res) => {
    try {
        // ENSURE PORT 8089
        const cartServiceUrl = process.env.CART_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = cartServiceUrl.includes(':8089') ? cartServiceUrl : 'http://localhost:8089';
        
        const token = req.cookies.accessToken;
        const headers = {};
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        } else {
            const guestCartId = req.cookies.guestCartId;
            if (guestCartId) {
                headers['X-Guest-Cart-Id'] = guestCartId;
            } else {
                return res.json({ count: 0 });
            }
        }
        
        const response = await axios.get(`${finalUrl}/api/v1/cart`, {
            headers
        });
        const cart = response.data.data || response.data;
        res.json({ count: cart.totalItems || 0 });
    } catch (error) {
        res.json({ count: 0 });
    }
});

// Update profile API
router.put('/api/members/:id', isAuthenticated, async (req, res) => {
    try {
        // ENSURE PORT 8089
        const memberServiceUrl = process.env.MEMBER_SERVICE_URL || 'http://localhost:8089';
        const finalUrl = memberServiceUrl.includes(':8089') ? memberServiceUrl : 'http://localhost:8089';
        
        console.log('Update Profile API called - Using URL:', `${finalUrl}/api/v1/members/${req.params.id}`);
        
        const response = await axios.put(
            `${finalUrl}/api/v1/members/${req.params.id}`,
            req.body, // Send body data (e.g., { name: "..." })
            {
                headers: { 
                    'Authorization': `Bearer ${req.token}`,
                    'Content-Type': 'application/json'
                }
            }
        );
        res.json(response.data);
    } catch (error) {
        console.error('Update profile error:', error.response?.data || error.message);
        res.status(error.response?.status || 500).json(
            error.response?.data || { 
                success: false, 
                message: 'Failed to update profile.' 
            }
        );
    }
});

module.exports = router;
