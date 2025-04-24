import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  // Регистрация тестового пользователя
  const registerUrl = 'http://api-gateway:8080/api/auth/register';
  const username = `testuser_${Math.random().toString(36).substring(7)}`;
  const registerPayload = JSON.stringify({
    username: username,
    password: 'testpassword',
    email: `${username}@example.com`,
    roles: 'USER',
  });
  const registerParams = {
    headers: { 'Content-Type': 'application/json' },
  };

  const registerRes = http.post(registerUrl, registerPayload, registerParams);
  console.log(`Register response: status=${registerRes.status}, body=${registerRes.body}, error=${registerRes.error}`);
  check(registerRes, {
    'registration successful': (r) => r.status === 200,
  });

  if (registerRes.status !== 200 || registerRes.error) {
    console.error(`Registration failed: status=${registerRes.status}, body=${registerRes.body}, error=${registerRes.error}`);
    return null;
  }

  // Получение JWT-токена
  const loginUrl = 'http://api-gateway:8080/api/auth/login';
  const loginPayload = JSON.stringify({
    username: username,
    password: 'testpassword',
  });
  const loginParams = {
    headers: { 'Content-Type': 'application/json' },
  };

  const loginRes = http.post(loginUrl, loginPayload, loginParams);
  console.log(`Login response: status=${loginRes.status}, body=${loginRes.body}, error=${loginRes.error}`);
  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  if (loginRes.status !== 200 || loginRes.error) {
    console.error(`Login failed: status=${loginRes.status}, body=${loginRes.body}, error=${loginRes.error}`);
    return null;
  }

  if (!loginRes.body) {
    console.error('Login response body is null');
    return null;
  }

  let token;
  try {
    token = loginRes.json('token');
  } catch (e) {
    console.error(`Failed to parse JSON or extract token: ${e.message}, body=${loginRes.body}`);
    return null;
  }

  if (!token) {
    console.error('JWT token is missing in response');
    return null;
  }

  console.log(`JWT token retrieved: ${token}`);
  return { token, username };
}

export default function (data) {
  if (!data || !data.token) {
    console.error('No token available, skipping requests');
    return;
  }

  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  };

  let res = http.get('http://api-gateway:8080/api/tasks', params);
  console.log(`GET /api/tasks response: status=${res.status}, body=${res.body}, error=${res.error}`);
  check(res, {
    'GET /api/tasks status is 200': (r) => r.status === 200,
  });

  const taskPayload = JSON.stringify({
    title: `Test Task ${Math.random()}`,
    description: 'Test Description',
    completed: false,
  });
  res = http.post('http://api-gateway:8080/api/tasks', taskPayload, params);
  console.log(`POST /api/tasks response: status=${res.status}, body=${res.body}, error=${res.error}`);
  check(res, {
    'POST /api/tasks status is 200': (r) => r.status === 200,
  });

  const taskId = res.json('id');
  if (taskId) {
    res = http.del(`http://api-gateway:8080/api/tasks/${taskId}`, null, params);
    console.log(`DELETE /api/tasks/${taskId} response: status=${res.status}, body=${res.body}, error=${res.error}`);
    check(res, {
      'DELETE /api/tasks status is 204': (r) => r.status === 204,
    });
  }

  sleep(1);
}