# CI Pipeline

## Overview

The Continuous Integration (CI) pipeline runs automatically on every push to `main` or `prod` branches when source code, build configuration, or Docker files change. The CI pipeline handles verification only — it answers the question: "Is this code safe to merge and free of regressions?"

## Pipeline Stages

```
[ Test ]
    │
    └──> [ Build Docker Image ] ──> [ Deploy ]
              (prod only)            (prod only)
```

**Note:** The Build Docker Image and Deploy stages are part of the [CD Pipeline](cd-pipeline.md) and only run on the `prod` branch.

## Stage Details

### 1. Test

**Purpose:** Compile Java source code and execute unit tests to verify code correctness.

**Runner:** `ubuntu-22.04`

**Steps:**
1. Checkout code with full git history (`fetch-depth: 0`)
2. Setup JDK 25 (Amazon Corretto)
3. Setup Gradle with caching (`gradle/actions/setup-gradle@v4`)
4. Run `./gradlew test`

**Test Configuration:**
- Unit tests: All tests except `*IT`, `*ITCase`, `*FlowIT` patterns
- Integration tests: Separate task (`integrationTest`) for `*IT`, `*FlowIT`, `*ITCase`
- Test database: H2 in-memory (MySQL compatibility mode)
- Testcontainers: Enabled for MySQL integration tests

**Trigger:** Always runs on pipeline trigger.

## Trigger Configuration

The CI pipeline triggers on pushes to `main` or `prod` branches when any of these files change:

```yaml
paths:
  - 'src/**'                         # Source code changes
  - 'build.gradle.kts'               # Build configuration changes
  - 'infrastructure/docker/**'       # Docker files changes
  - '.github/workflows/*-monolith.yml' # Pipeline changes
```

**Branch behavior:**
| Branch | CI Stages | CD Stages |
|---|---|---|
| `main` | test | — |
| `prod` | test | build, deploy |

## Caching Strategy

| Cache | Mechanism | Benefit |
|---|---|---|
| Gradle dependencies | `gradle/actions/setup-gradle@v4` | Faster subsequent builds |
| GitHub Actions | Built-in action cache | Faster step execution |

## Required GitHub Secrets

| Secret | Purpose | Used In |
|---|---|---|
| `GITHUB_TOKEN` | GHCR authentication | Build stage (auto-provided) |

## Running CI Locally

To run the CI pipeline locally:

```bash
# Test (compiles and runs tests)
./gradlew test
```
