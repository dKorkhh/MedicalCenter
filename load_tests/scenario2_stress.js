import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 25 },
    { duration: '10s', target: 60 },
    { duration: '15s', target: 60 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<650', 'p(99)<800'],
    http_req_failed: ['rate<0.02'],
  },
};

export default function () {

  const res = http.get('http://localhost:8081/actuator/health/liveness');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.2);
}
