/**
 * k6 Load Test — Sustained traffic at expected levels
 *
 * Answers: "Can we handle expected traffic?"
 * Scenario: 100 virtual users for 5 minutes, mix of single + range queries
 *
 * Run: k6 run k6/load-test.js
 * Or via Docker: docker compose -f k6/docker-compose.k6.yml run k6 run /scripts/load-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-api-key-1';

export const options = {
    stages: [
        { duration: '30s', target: 50 },   // ramp up to 50 users
        { duration: '4m', target: 100 },    // hold at 100 users
        { duration: '30s', target: 0 },     // ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<50', 'p(99)<200'],  // p95 < 50ms, p99 < 200ms
        http_req_failed: ['rate<0.01'],                  // <1% error rate
    },
};

export default function () {
    const headers = { 'X-API-Key': API_KEY };

    // 80% single queries, 20% range queries (realistic traffic mix)
    if (Math.random() < 0.8) {
        const num = Math.floor(Math.random() * 3999) + 1;
        const res = http.get(`${BASE_URL}/romannumeral?query=${num}`, { headers });
        check(res, {
            'single: status 200': (r) => r.status === 200,
            'single: has output': (r) => JSON.parse(r.body).output !== undefined,
        });
    } else {
        const min = Math.floor(Math.random() * 3900) + 1;
        const max = min + Math.floor(Math.random() * 100) + 1;
        const res = http.get(`${BASE_URL}/romannumeral?min=${min}&max=${Math.min(max, 3999)}`, { headers });
        check(res, {
            'range: status 200': (r) => r.status === 200,
            'range: has conversions': (r) => JSON.parse(r.body).conversions !== undefined,
        });
    }

    sleep(0.1); // 100ms think time between requests
}
