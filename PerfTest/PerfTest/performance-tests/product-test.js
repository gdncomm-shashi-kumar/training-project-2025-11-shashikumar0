import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { PRODUCT_URL, authenticateUser } from './utils.js';

// Metrics
const errorRate = new Rate('errors');
const searchTrend = new Trend('product_search_duration');
const detailTrend = new Trend('product_detail_duration');

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

  // Search Products
  const searchRes = http.get(`${PRODUCT_URL}/api/v1/products?page=0&size=10`, params);
  check(searchRes, {
    'search products status 200': (r) => r.status === 200,
  }) || errorRate.add(1);
  searchTrend.add(searchRes.timings.duration);

  // Search Products by Name (Plastic)
  const searchByNameRes = http.get(`${PRODUCT_URL}/api/v1/products?page=0&size=20&sort=name%2Casc&name=Plastic`, params);
  check(searchByNameRes, {
    'search products by name status 200': (r) => r.status === 200,
  }) || errorRate.add(1);
  searchTrend.add(searchByNameRes.timings.duration);

  // Pick a random product if available
  let productId;
  const products = searchRes.json('data.content');
  if (products && products.length > 0) {
    const product = products[Math.floor(Math.random() * products.length)];
    productId = product.productId;
  }

  if (productId) {
    // Get Product Details
    const productDetailRes = http.get(`${PRODUCT_URL}/api/v1/products/${productId}`, params);
    check(productDetailRes, {
      'get product detail status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
    detailTrend.add(productDetailRes.timings.duration);
  }

  sleep(1);
}
