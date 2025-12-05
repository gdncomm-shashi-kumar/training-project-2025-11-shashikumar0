import http from 'k6/http';
import { check } from 'k6';

// Base URLs
export const AUTH_URL = 'http://localhost:8089';
export const PRODUCT_URL = 'http://localhost:8089';
export const CART_URL = 'http://localhost:8089';
export const MEMBER_URL = 'http://localhost:8089';

// Helper to generate random string
export function randomString(length) {
  const charset = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let res = '';
  for (let i = 0; i < length; i++) {
    res += charset[Math.floor(Math.random() * charset.length)];
  }
  return res;
}

// Helper to authenticate a user and return token, memberId, etc.
export function authenticateUser() {
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
  });
  
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
    console.error('Login failed in authenticateUser:', loginRes.body);
    return null;
  }

  const token = loginRes.json('data.accessToken');

  return { token, email, password, memberId };
}
