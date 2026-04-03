/**
 * k6 Spike Test — Handle sudden burst traffic
 *
 * Answers: "Can we handle sudden bursts?"
 * Scenario: 10 users → spike to 500 → back to 10
 *
 * Run: k6 run k6/spike-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-api-key-1';

export const options = {
    stages: [
        { duration: '30s', target: 10 },    // warm up
        { duration: '10s', target: 500 },    // spike!
        { duration: '1m', target: 500 },     // hold spike
        { duration: '10s', target: 10 },     // recover
        { duration: '1m', target: 10 },      // verify recovery
        { duration: '30s', target: 0 },      // ramp down
    ],
};

export default function () {
    const headers = { 'X-API-Key': API_KEY };
    const num = Math.floor(Math.random() * 3999) + 1;
    const res = http.get(`${BASE_URL}/romannumeral?query=${num}`, { headers });

    check(res, {
        'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
        'response time < 1s': (r) => r.timings.duration < 1000,
    });

    sleep(0.05);
}
