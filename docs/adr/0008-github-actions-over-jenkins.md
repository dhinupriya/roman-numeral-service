# ADR-0008: GitHub Actions over Jenkins for CI/CD

## Status
Accepted

## Context
Need a CI/CD pipeline. The assessment requires a public GitHub repository. Two main options: GitHub Actions (native to GitHub) or Jenkins (enterprise standard, self-hosted).

## Options Considered
1. **GitHub Actions** — native to GitHub, reviewer sees pipeline runs in the Actions tab, free for public repos, YAML config, no server needed.
2. **Jenkins** — enterprise standard, self-hosted, 1800+ plugins, runs inside your network. Requires a Jenkins server that the reviewer can't access.

## Decision
GitHub Actions as primary CI/CD. Jenkinsfile included as a bonus artifact to demonstrate enterprise CI/CD familiarity.

## Consequences
- **Gain:** Reviewer sees green/red pipeline directly in the repo. Zero infrastructure to set up. Quality gate (JaCoCo ≥ 80%) enforced on every push/PR.
- **Trade-off:** Less control than Jenkins (no self-hosted runners by default). Acceptable for an assessment.
- **Bonus:** Jenkinsfile shows the reviewer you know enterprise tooling without requiring them to run a Jenkins server.
