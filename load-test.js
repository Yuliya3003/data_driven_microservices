import http from 'k6/http';
import { check, sleep } from 'k6';


export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    'http_req_duration{status:200}': ['p(95)<2000'],
  },
};

const API_GATEWAY_URL = 'http://api-gateway:8080';


export function testUserService() {
  const res = http.get(`${API_GATEWAY_URL}/users`);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time is acceptable': (r) => r.timings.duration < 2000,
  });
  sleep(1);
}


export function testTaskService() {
  const res = http.get(`${API_GATEWAY_URL}/tasks`);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time is acceptable': (r) => r.timings.duration < 2000,
  });
  sleep(1);
}


export function testApiGateway() {
  const res = http.get(`${API_GATEWAY_URL}/actuator/health`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(1);
}

export default function () {

  const random = Math.random();
  if (random < 0.33) {
    testUserService();
  } else if (random < 0.66) {
    testTaskService();
  } else {
    testApiGateway();
  }
}