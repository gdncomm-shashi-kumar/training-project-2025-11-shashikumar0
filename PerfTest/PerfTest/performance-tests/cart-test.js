import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { CART_URL, PRODUCT_URL, authenticateUser } from './utils.js';

// Metrics
const errorRate = new Rate('errors');
const addToCartTrend = new Trend('add_to_cart_duration');

// Configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    errors: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export function setup() {
  return authenticateUser();
}

export default function (data) {
  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  };

  // We need a product SKU to add to cart. 
  // Ideally we search for one first.
  const searchRes = http.get(`${PRODUCT_URL}/api/v1/products?page=0&size=10`, params);
  let sku;
  const products = searchRes.json('data.content');
  if (products && products.length > 0) {
    const product = products[Math.floor(Math.random() * products.length)];
    if (product.variants && product.variants.length > 0) {
      sku = product.variants[0].sku;
    }
  }

  if (sku) {
    // Add to Cart
    const addToCartPayload = JSON.stringify({
      sku: sku,
      qty: 1,
    });
    const addToCartRes = http.post(`${CART_URL}/api/v1/cart`, addToCartPayload, params);
    check(addToCartRes, {
      'add to cart status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    addToCartTrend.add(addToCartRes.timings.duration);

    sleep(1);

    // Get Cart
    const getCartRes = http.get(`${CART_URL}/api/v1/cart`, params);
    check(getCartRes, {
      'get cart status 200': (r) => r.status === 200,
    });

    sleep(1);

    // Update Quantity
    const updateQtyPayload = JSON.stringify({
      qty: 2,
    });
    const updateQtyRes = http.put(`${CART_URL}/api/v1/cart/item/${sku}`, updateQtyPayload, params);
    check(updateQtyRes, {
      'update quantity status 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    sleep(1);

    // Remove Item
    const removeItemRes = http.del(`${CART_URL}/api/v1/cart/${sku}`, null, params);
    check(removeItemRes, {
      'remove item status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  }

  sleep(1);
}
