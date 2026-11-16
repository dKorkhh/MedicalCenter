import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 5 },
    { duration: '2s', target: 80 },
    { duration: '12s', target: 80 },
    { duration: '3s', target: 5 },
    { duration: '3s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500', 'p(99)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

export default function () {
  const res = http.get('http://localhost:8081/actuator/health/liveness');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.15);
}
