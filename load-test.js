import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10, // 10 виртуальных пользователей
  duration: '30s', // Тест длится 30 секунд
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% запросов быстрее 500 мс
    http_req_failed: ['rate<0.01'], // Ошибок менее 1%
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
  check(registerRes, {
    'registration successful': (r) => r.status === 200,
  });

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
  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  const token = loginRes.json('token');
  if (!token) {
    console.error('Failed to retrieve JWT token');
    return null;
  }

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

  // GET /api/tasks
  let res = http.get('http://api-gateway:8080/api/tasks', params);
  check(res, {
    'GET /api/tasks status is 200': (r) => r.status === 200,
  });

  // POST /api/tasks
  const taskPayload = JSON.stringify({
    title: `Test Task ${Math.random()}`,
    description: 'Test Description',
    completed: false,
  });
  res = http.post('http://api-gateway:8080/api/tasks', taskPayload, params);
  check(res, {
    'POST /api/tasks status is 200': (r) => r.status === 200,
  });

  // Получение ID созданной задачи
  const taskId = res.json('id');
  if (taskId) {
    // DELETE /api/tasks/{id}
    res = http.del(`http://api-gateway:8080/api/tasks/${taskId}`, null, params);
    check(res, {
      'DELETE /api/tasks status is 204': (r) => r.status === 204,
    });
  }

  sleep(1); // Пауза между итерациями
}