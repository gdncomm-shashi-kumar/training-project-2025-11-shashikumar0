# BliMarket UI - E-Commerce Frontend

A modern, responsive e-commerce user interface built with Node.js, Express, Handlebars, and SCSS. This UI integrates seamlessly with microservices backend for Member, Product, and Cart management.

## 🌟 Features

### Authentication System
- User registration with password validation (10+ chars, uppercase, lowercase, digit, special character)
- JWT-based login with access and refresh tokens
- Secure session management with HTTP-only cookies
- Protected routes with authentication middleware
- Forgot password flow (2-step: email → token → reset)
- Logout with token invalidation

### Product Catalog
- Product listing with pagination (20 items per page)
- Advanced search with wildcard support
- Category filtering (All, Electronics, Fashion, Home & Living, Sports)
- Multiple sort options (name A-Z/Z-A, price low-high)
- Variant-level SKU support (colors, sizes)
- Product detail pages with image galleries
- Clickable product cards and images
- Empty state handling for no search results

### Shopping Cart
- Add/remove items with variant selection
- Quantity management with +/- buttons
- Real-time cart updates
- Cart summary with totals
- Guest cart functionality (non-logged-in users)
- Cart merging after login
- Persistent cart storage (MongoDB backend)
- Clickable product images in cart

### User Profile
- Profile management
- Account information update
- Security settings (placeholder)
- Order history (placeholder)
- Saved addresses (placeholder)

### Modern UI/UX
- Responsive design (mobile, tablet, desktop)
- Clean and intuitive interface
- Toast notifications for user feedback
- Loading states
- Error handling with user-friendly messages
- Gradient designs and animations
- Modal popups for login/register
- Empty state messages

## 🚀 Quick Start

### Prerequisites
- Node.js 12+ and npm/yarn
- Backend microservices running:
  - Member Service (default: http://localhost:8089)
  - Product Service (default: http://localhost:8083)
  - Cart Service (default: http://localhost:8089)

### Installation (3 Steps)

1. **Install dependencies:**
```bash
cd BliMarket-ui-master
npm install
```

2. **Start the server:**
```bash
npm start
# or use the startup script
./start.sh
```

3. **Open your browser:**
```
http://localhost:3000
```

### Environment Variables (Optional)

Create a `.env` file or set environment variables:
```bash
PORT=3000
NODE_ENV=development
MEMBER_SERVICE_URL=http://localhost:8089
PRODUCT_SERVICE_URL=http://localhost:8083
CART_SERVICE_URL=http://localhost:8089
```

Default values will be used if not specified.

## 📁 Project Structure

```
BliMarket-ui-master/
├── public/
│   ├── dist/              # Compiled assets
│   │   ├── scripts.min.js
│   │   └── styles.min.css
│   ├── img/               # Images
│   ├── js/
│   │   └── main.js        # Client-side JavaScript
│   └── scss/
│       ├── main.scss      # Main styles
│       └── _variables.scss
├── views/
│   ├── pages/             # Page templates
│   │   ├── home.hbs
│   │   ├── login.hbs
│   │   ├── register.hbs
│   │   ├── product-detail.hbs
│   │   ├── cart.hbs
│   │   ├── profile.hbs
│   │   └── error.hbs
│   └── partials/          # Reusable components
│       ├── header.hbs
│       ├── navbar.hbs
│       ├── footer.hbs
│       └── modals.hbs
├── routes.js              # Express routes & API proxy
├── server.js              # Express server
└── package.json
```

## 🎯 Routes Reference

### Public Routes
- `GET /` - Home (Product Catalog)
- `GET /product/:id` - Product Detail
- `GET /login` - Login Page (also available as modal)
- `GET /register` - Registration Page (also available as modal)

### Protected Routes (Require Auth)
- `GET /cart` - Shopping Cart
- `GET /profile` - User Profile
- `GET /logout` - Logout (clears cookies)

### API Proxy Routes
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Register
- `POST /api/auth/logout` - Logout (with token invalidation)
- `POST /api/auth/forgot-password` - Request password reset token
- `POST /api/auth/reset-password` - Reset password with token
- `GET /api/v1/cart` - Get cart
- `POST /api/v1/cart` - Add item to cart
- `PUT /api/v1/cart/item/:sku` - Update item quantity
- `DELETE /api/v1/cart/:sku` - Remove item from cart
- `DELETE /api/v1/cart` - Clear cart
- `POST /api/v1/cart/merge` - Merge guest cart with user cart
- `GET /api/members/:id` - Get member profile
- `PUT /api/members/:id` - Update member profile

## 🔧 Backend API Integration

### Member Service (Port 8089)
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/logout` - User logout (requires Authorization header + refreshToken in body)
- `POST /api/v1/auth/forgot-password` - Request password reset token
- `POST /api/v1/auth/reset-password` - Reset password with token
- `GET /api/v1/members/:id` - Get member details
- `PUT /api/v1/members/:id` - Update member (Content-Type: application/json)

### Product Service (Port 8083)
- `GET /api/v1/products` - List products with search, filter, sort, pagination
- `GET /api/v1/products/:id` - Get product details

### Cart Service (Port 8089)
- `GET /api/v1/cart` - Get user cart (supports guest cart via memberId query param)
- `POST /api/v1/cart` - Add item to cart (supports guest cart via memberId in body)
- `PUT /api/v1/cart/item/:sku` - Update item quantity
- `DELETE /api/v1/cart/:sku` - Remove item from cart
- `DELETE /api/v1/cart` - Clear cart
- `POST /api/v1/cart/merge` - Merge guest cart with authenticated user cart

## 🧪 API Testing Examples

### Test Registration
```bash
curl -X POST http://localhost:8089/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!@",
    "name": "Test User"
  }'
```

### Test Login
```bash
curl -X POST http://localhost:8089/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!@"
  }'
```

### Test Logout
```bash
curl -X POST http://localhost:8089/api/v1/auth/logout \
  -H "Authorization: Bearer ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "REFRESH_TOKEN"
  }'
```

### Test Forgot Password
```bash
# Step 1: Request reset token
curl -X POST http://localhost:8089/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'

# Step 2: Reset password with token (from Step 1 response)
curl -X POST http://localhost:8089/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "RESET_TOKEN_FROM_STEP_1",
    "newPassword": "NewSecurePass123!@"
  }'
```

### Test Get Products
```bash
curl -X GET "http://localhost:8083/api/v1/products?page=0&size=20&sort=name,asc&name=iphone&category=Electronics"
```

### Test Get Cart
```bash
# Authenticated user
curl -X GET http://localhost:8089/api/v1/cart \
  -H "Authorization: Bearer TOKEN"

# Guest user
curl -X GET "http://localhost:8089/api/v1/cart?memberId=guest-cart-id"
```

### Test Add to Cart
```bash
# Authenticated user
curl -X POST http://localhost:8089/api/v1/cart \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKU-001",
    "qty": 1
  }'

# Guest user (first time)
curl -X POST http://localhost:8089/api/v1/cart \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKU-001",
    "qty": 1
  }'

# Guest user (subsequent times - use guestCartId from first response)
curl -X POST http://localhost:8089/api/v1/cart \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SKU-002",
    "qty": 1,
    "memberId": "guest-cart-id-from-first-response"
  }'
```

### Test Merge Cart
```bash
curl -X POST "http://localhost:8089/api/v1/cart/merge?guestCartId=guest-cart-id" \
  -H "Authorization: Bearer USER_TOKEN" \
  -H "Content-Type: application/json"
```

## 🎨 Features Breakdown

### Authentication Pages
- **Login** (`/login` or modal)
  - Email and password authentication
  - Remember me option
  - Password visibility toggle
  - Forgot password link
  - Redirect to previous page after login

- **Register** (`/register` or modal)
  - Form validation
  - Password strength requirements (10+ chars, upper, lower, digit, special)
  - Real-time password validation feedback
  - Terms and conditions checkbox

- **Forgot Password** (modal)
  - Step 1: Enter email → receive reset token
  - Step 2: Enter token and new password → reset password

### Product Pages
- **Home/Catalog** (`/`)
  - Responsive product grid (1-4 columns based on screen size)
  - Search functionality with wildcard support
  - Category filtering
  - Sorting options (name, price)
  - Pagination with "Previous | Page X of Y | Next" format
  - Product cards with images, pricing, variants
  - Clickable product cards (navigate to detail page)
  - Empty state for no search results
  - Loading states

- **Product Detail** (`/product/:id`)
  - Image gallery with thumbnails
  - Variant selection (color, size)
  - Real-time price updates per variant
  - Quantity selector
  - Add to cart functionality
  - Product tabs (description, specifications, reviews)
  - Breadcrumb navigation

### Shopping Cart
- **Cart Page** (`/cart`)
  - Cart items list with clickable images
  - Quantity adjustment with +/- buttons
  - Item removal
  - Clear cart option
  - Order summary with totals
  - Guest cart login prompt
  - Promo code input (placeholder)
  - Trust badges
  - Empty cart state

### User Profile
- **Profile Page** (`/profile`)
  - Account information management
  - Profile update form
  - Navigation tabs
  - Order history (placeholder)
  - Security settings (placeholder)

## 🔐 Security Features

- JWT token management (access + refresh)
- HTTP-only cookies for token storage
- CSRF protection ready
- Password complexity validation
- Protected routes with authentication middleware
- Secure API communication
- Guest cart ID management

## 🌈 Styling

- **CSS Framework**: Custom SCSS with modern design
- **Colors**: Purple gradient theme (#667eea to #764ba2)
- **Typography**: Inter font family
- **Icons**: Font Awesome 6
- **Responsive**: Mobile-first approach
- **Animations**: Smooth transitions and hover effects

## 📱 Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## 🐛 Error Handling

- Global error handler middleware
- 404 page for not found routes
- User-friendly error messages
- Toast notifications for API errors
- Form validation errors
- Network error handling
- Empty state handling

## 📝 Key Implementation Details

### Guest Cart Flow
1. Non-logged-in user adds product → API returns `guestCartId`
2. `guestCartId` stored in HTTP-only cookie
3. Subsequent cart operations pass `guestCartId` in `memberId` field
4. User logs in → cart merge API called with `guestCartId`
5. Guest cart items merged into user's cart
6. `guestCartId` cookie cleared after merge

### API Port Configuration
- All cart and member APIs use port **8089**
- Product APIs use port **8083**
- Ports are enforced in `routes.js` with fallback logic

### Image Handling
- Product images use `picsum.photos` with seed-based URLs
- Images are product-specific using `productId` as seed
- Fallback images on error
- Responsive image sizing

### Toast Notifications
- Success, error, info, and warning types
- Auto-dismiss after 3 seconds
- Prevents stacking (removes existing toasts)
- Smooth animations


## 📄 License

ISC License

## 👥 Authors

GDN Team - E-Commerce Platform

## 🤝 Contributing

This is a demonstration project for e-commerce microservices architecture.

## 📞 Support

For support, please contact: support@gdn.com

---

**Note**: Ensure all backend microservices are running before starting the UI application. The application will gracefully handle service unavailability with appropriate error messages.
