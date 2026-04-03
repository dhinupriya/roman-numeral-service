/**
 * k6 Soak Test — Detect memory leaks over time
 *
 * Answers: "Are there memory leaks after extended use?"
 * Scenario: 50 users for 30 minutes (reduce for quick verification)
 *
 * Run: k6 run k6/soak-test.js
 * Quick: k6 run --duration 5m k6/soak-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-api-key-1';

export const options = {
    stages: [
        { duration: '1m', target: 50 },     // ramp up
        { duration: '28m', target: 50 },     // sustained load
        { duration: '1m', target: 0 },       // ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<100'],    // p95 should stay consistent over time
        http_req_failed: ['rate<0.01'],       // <1% errors over the entire run
    },
};

export default function () {
    const headers = { 'X-API-Key': API_KEY };

    // Mix of operations
    if (Math.random() < 0.7) {
        const num = Math.floor(Math.random() * 3999) + 1;
        const res = http.get(`${BASE_URL}/romannumeral?query=${num}`, { headers });
        check(res, { 'single 200': (r) => r.status === 200 });
    } else {
        const min = Math.floor(Math.random() * 3900) + 1;
        const max = min + Math.floor(Math.random() * 50) + 1;
        const res = http.get(`${BASE_URL}/romannumeral?min=${min}&max=${Math.min(max, 3999)}`, { headers });
        check(res, { 'range 200': (r) => r.status === 200 });
    }

    sleep(0.2);
}
