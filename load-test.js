import http from 'k6/http';
import { check, sleep } from 'k6';

// Тест 1: Низкая нагрузка (5 пользователей, 10 секунд)
export const options = {
  scenarios: {
    low_load: {
      executor: 'constant-vus',
      vus: 5,
      duration: '10s',
    },
    medium_load: {
      executor: 'constant-vus',
      vus: 20,
      duration: '20s',
      startTime: '12s', // Запуск после завершения low_load
    },
    high_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50 }, // Наращивание до 50 пользователей
        { duration: '20s', target: 50 }, // Удержание 50 пользователей
        { duration: '10s', target: 0 },  // Снижение до 0
      ],
      startTime: '35s', // Запуск после medium_load
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% запросов должны быть быстрее 500 мс
    http_req_failed: ['rate<0.01'],   // Ошибки должны быть менее 1%
  },
};

export default function () {
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
    timeout: '10s',
  };

  console.log(`Sending registration request: ${registerUrl}, payload=${registerPayload}`);
  const registerRes = http.post(registerUrl, registerPayload, registerParams);
  console.log(`Register response: status=${registerRes.status}, body=${registerRes.body}, error=${registerRes.error}, error_code=${registerRes.error_code}`);

  check(registerRes, {
    'registration successful': (r) => r.status === 200,
  });

  if (registerRes.error || registerRes.status !== 200) {
    console.error(`Registration failed: status=${registerRes.status}, body=${registerRes.body}, error=${registerRes.error}, error_code=${registerRes.error_code}`);
  }

  sleep(1); // Пауза 1 секунда между запросами
}