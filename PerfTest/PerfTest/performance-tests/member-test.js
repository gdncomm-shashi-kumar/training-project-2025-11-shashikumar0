import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { MEMBER_URL, authenticateUser } from './utils.js';

// Metrics
const errorRate = new Rate('errors');

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

  if (data.memberId) {
    const getMemberRes = http.get(`${MEMBER_URL}/api/v1/members/${data.memberId}`, params);
    check(getMemberRes, {
      'get member status 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  } else {
    console.warn('Skipping Member Flow: No memberId available');
  }

  sleep(1);
}
