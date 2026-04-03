/**
 * k6 Stress Test — Find the breaking point
 *
 * Answers: "Where does it break?"
 * Scenario: Ramp from 0 to 500 users over 10 minutes
 *
 * Run: k6 run k6/stress-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-api-key-1';

export const options = {
    stages: [
        { duration: '2m', target: 100 },
        { duration: '3m', target: 200 },
        { duration: '3m', target: 500 },
        { duration: '2m', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(99)<500'],   // p99 < 500ms even under stress
    },
};

export default function () {
    const headers = { 'X-API-Key': API_KEY };
    const num = Math.floor(Math.random() * 3999) + 1;
    const res = http.get(`${BASE_URL}/romannumeral?query=${num}`, { headers });

    check(res, {
        'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    });

    sleep(0.05);
}
