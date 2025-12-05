import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Metrics
const errorRate = new Rate('errors');
const loginTrend = new Trend('login_duration');
const productSearchTrend = new Trend('product_search_duration');
const addToCartTrend = new Trend('add_to_cart_duration');

// Configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 }, // Ramp up to 10 users
    { duration: '1m', target: 10 },  // Stay at 10 users
    { duration: '30s', target: 0 },  // Ramp down
  ],
  thresholds: {
    errors: ['rate<0.01'], // Error rate should be less than 1%
    http_req_duration: ['p(95)<500'], // 95% of requests should be under 500ms
  },
};

// Base URLs
const AUTH_URL = 'http://localhost:8089';
const PRODUCT_URL = 'http://localhost:8089';
const CART_URL = 'http://localhost:8089';
const MEMBER_URL = 'http://localhost:8089';

// Helper to generate random string
function randomString(length) {
  const charset = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let res = '';
  for (let i = 0; i < length; i++) {
    res += charset[Math.floor(Math.random() * charset.length)];
  }
  return res;
}

// Setup: Register and Login a user to get a token
export function setup() {
  const email = `testuser_${randomString(8)}@example.com`;
  const password = 'Password123!';
  const name = 'Test User';

  // Register
  const registerPayload = JSON.stringify({
    email: email,
    password: password,
    name: name,
  });

  const registerRes = http.post(`${AUTH_URL}/api/v1/auth/register`, registerPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(registerRes, {
    'registered successfully': (r) => r.status === 201,
  }) || errorRate.add(1);
  if (registerRes.status !== 201) {
      console.error('Register Failed:', registerRes.body);
  } else {
      console.log('Register Success:', registerRes.body);
  }
  
  const memberId = registerRes.json('data.memberId');

  // Login
  const loginPayload = JSON.stringify({
    email: email,
    password: password,
  });

  const loginRes = http.post(`${AUTH_URL}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const loginSuccess = check(loginRes, {
    'logged in successfully': (r) => r.status === 200,
    'has access token': (r) => r.json('data.accessToken') !== undefined,
  });

  if (!loginSuccess) {
    errorRate.add(1);
    console.error('Login failed:', loginRes.body);
    return { token: null };
  }
  console.log('Login Success:', loginRes.body);

  loginTrend.add(loginRes.timings.duration);
  const token = loginRes.json('data.accessToken');

  return { token, email, password, memberId };
}

export default function (data) {
  if (!data.token) {
    console.log('Skipping iteration due to setup failure');
    sleep(1);
    return;
  }

  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  };

  group('Product Flow', function () {
    // Search Products
    const searchRes = http.get(`${PRODUCT_URL}/api/v1/products?page=0&size=10`, params);
    check(searchRes, {
      'search products status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    productSearchTrend.add(searchRes.timings.duration);

    // Pick a random product if available
    let productId, sku;
    const products = searchRes.json('data.content');
    if (products && products.length > 0) {
      const product = products[Math.floor(Math.random() * products.length)];
      productId = product.productId;
      if (product.variants && product.variants.length > 0) {
        sku = product.variants[0].sku;
      }
    }

    if (productId) {
      // Get Product Details
      const productDetailRes = http.get(`${PRODUCT_URL}/api/v1/products/${productId}`, params);
      check(productDetailRes, {
        'get product detail status 200': (r) => r.status === 200,
      });
    }

    // Cart Operations
    if (sku) {
      group('Cart Flow', function () {
        // Add to Cart
        const addToCartPayload = JSON.stringify({
          sku: sku,
          qty: 1,
        });
        const addToCartRes = http.post(`${CART_URL}/api/v1/cart`, addToCartPayload, params);
        if (!check(addToCartRes, {
          'add to cart status 200': (r) => r.status === 200,
        })) {
          errorRate.add(1);
          console.error(`Add to Cart Failed for SKU ${sku}: ${addToCartRes.status} ${addToCartRes.body}`);
        } else {
          addToCartTrend.add(addToCartRes.timings.duration);
          console.log(`Add to Cart Success for SKU ${sku}:`, addToCartRes.body);
        }

        sleep(1);

        // Get Cart
        const getCartRes = http.get(`${CART_URL}/api/v1/cart`, params);
        console.log('Get Cart Response:', getCartRes.body);
        check(getCartRes, {
          'get cart status 200': (r) => r.status === 200,
        });

        sleep(1);

        // Update Quantity
        const updateQtyPayload = JSON.stringify({
          qty: 2,
        });
        console.log(`Updating Quantity for SKU ${sku} with headers:`, JSON.stringify(params));
        const updateQtyRes = http.put(`${CART_URL}/api/v1/cart/item/${sku}`, updateQtyPayload, params);
        if (!check(updateQtyRes, {
          'update quantity status 200': (r) => r.status === 200,
        })) {
           console.error(`Update Quantity Failed for SKU ${sku}: ${updateQtyRes.status} ${updateQtyRes.body}`);
        }

        sleep(1);

        // Remove Item
        console.log(`Removing Item SKU ${sku} with headers:`, JSON.stringify(params));
        const removeItemRes = http.del(`${CART_URL}/api/v1/cart/${sku}`, null, params);
        if (!check(removeItemRes, {
          'remove item status 200': (r) => r.status === 200,
        })) {
           console.error(`Remove Item Failed for SKU ${sku}: ${removeItemRes.status} ${removeItemRes.body}`);
        }
      });
    }
  });

  group('Member Flow', function () {
    if (data.memberId) {
        const getMemberRes = http.get(`${MEMBER_URL}/api/v1/members/${data.memberId}`, params);
        check(getMemberRes, {
            'get member status 200': (r) => r.status === 200,
        });
    } else {
        console.warn('Skipping Member Flow: No memberId available');
    }
  });

  sleep(1);
}
