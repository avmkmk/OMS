# Order Management System (OMS)

A multi-service Order Management System built with Spring Boot, Kafka, and PostgreSQL. It includes:
- IAM (auth/JWT)
- Order service
- Inventory service
- Payment service
- Notification service
- Observability via Prometheus and Grafana

## Architecture
- **IAM** issues JWTs and hosts JWKS for resource servers.
- **Order** orchestrates order creation and publishes events.
- **Inventory** manages stock and reservation.
- **Payment** processes payments and emits events.
- **Notification** consumes events and logs notifications.

Services communicate over HTTP and Kafka. All services expose Actuator metrics for Prometheus.

## Prerequisites
Install these before running locally:
- Java 21
- Maven 3.9+
- Docker + Docker Compose
- Git

Optional (for local testing outside Docker):
- PostgreSQL 15+

## Repo Layout
- `iam/`, `order/`, `inventory/`, `payment/`, `notification/` ? services
- `common/` ? shared configuration (security, metrics, OpenAPI, Kafka publisher)
- `api/` ? shared event DTOs
- `monitoring/` ? Prometheus config
- `docker-compose.yaml` ? local stack

## Quick Start (Docker)
From the `services/` repo root:

```powershell
mvn -DskipTests clean package

docker compose -f docker-compose.yaml up -d --build
```

Verify:
- IAM: `http://localhost:8080`
- Order: `http://localhost:8081`
- Inventory: `http://localhost:8082`
- Payment: `http://localhost:8083`
- Notification: `http://localhost:8084`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

Grafana default login is typically `admin/admin` on first run (if unchanged), and it will prompt you to reset.

## Build Only
```powershell
mvn -DskipTests clean package
```

## Run Tests (Local Scripts)
These scripts expect services running:

```powershell
# Happy flow
powershell -ExecutionPolicy Bypass -File .\test_happy_path.ps1

# Failure flow
powershell -ExecutionPolicy Bypass -File .\test_failure_scenarios.ps1
```

## API Docs (Swagger UI)
- Order: `http://localhost:8081/swagger-ui/index.html`
- Inventory: `http://localhost:8082/swagger-ui/index.html`
- Payment: `http://localhost:8083/swagger-ui/index.html`
- IAM: `http://localhost:8080/swagger-ui/index.html`

## Metrics
Each service exposes Prometheus metrics at:
```
/actuator/prometheus
```

Prometheus scrapes all services using `monitoring/prometheus.yml`.

## Common Config
Shared defaults live in `common/src/main/resources/application-common.properties`.
Each service imports it using:
```
spring.config.import=optional:classpath:application-common.properties
```

## Forking and Contributing
1. Fork the repo in GitHub.
2. Clone your fork:

```bash
git clone <your-fork-url>
cd services
```

3. Add upstream (optional):
```bash
git remote add upstream <original-repo-url>
```

4. Create a branch:
```bash
git checkout -b feature/your-change
```

5. Commit and push:
```bash
git add -A
git commit -m "Your message"
git push -u origin feature/your-change
```

6. Open a PR from your fork.

## Notes
- If services fail right after `docker compose up`, wait ~30 seconds for IAM and DB migrations to finish.
- For local JVM runs (without Docker), ensure Postgres is running and properties point to `localhost`.
