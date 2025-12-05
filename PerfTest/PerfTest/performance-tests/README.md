# Performance Tests

This directory contains performance tests for the E-commerce APIs using [k6](https://k6.io/).

## Prerequisites

- [k6](https://k6.io/docs/get-started/installation/) must be installed on your machine.

### Installation

**macOS (using Homebrew):**
```bash
brew install k6
```

**Windows (using Chocolatey):**
```bash
choco install k6
```

## Running the Tests

You can run tests for individual services or the complete flow.

### Individual Service Tests

**Authentication Service:**
```bash
k6 run performance-tests/auth-test.js
```

**Product Service:**
```bash
k6 run performance-tests/product-test.js
```

**Cart Service:**
```bash
k6 run performance-tests/cart-test.js
```

**Member Service:**
```bash
k6 run performance-tests/member-test.js
```

### Complete Flow Test

To run the monolithic load test covering all flows:
```bash
k6 run performance-tests/load-test.js
```

### Configuration

The test configuration (stages, thresholds) is defined in each script under the `options` object. You can modify:
- **Stages**: Ramp-up, steady state, and ramp-down durations.
- **Thresholds**: Success criteria (e.g., error rate < 1%, p95 response time < 500ms).
