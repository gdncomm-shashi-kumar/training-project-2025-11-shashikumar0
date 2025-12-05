import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { AUTH_URL, randomString } from './utils.js';

// Metrics
const errorRate = new Rate('errors');
const loginTrend = new Trend('login_duration');
const registerTrend = new Trend('register_duration');

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

export default function () {
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
  registerTrend.add(registerRes.timings.duration);

  sleep(1);

  // Login
  const loginPayload = JSON.stringify({
    email: email,
    password: password,
  });

  const loginRes = http.post(`${AUTH_URL}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(loginRes, {
    'logged in successfully': (r) => r.status === 200,
    'has access token': (r) => r.json('data.accessToken') !== undefined,
  }) || errorRate.add(1);
  loginTrend.add(loginRes.timings.duration);

  sleep(1);
}
