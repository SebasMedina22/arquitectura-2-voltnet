# MS-Billing

Microservicio **asíncrono** de facturación. Sustenta el lado consumidor de la regla **R3** (cierre resiliente): MS-ChargeOrchestrator publica el evento de cierre de sesión y Billing lo procesa de forma idempotente, fuera del camino crítico del conductor.

## Stack

- Java 25, Spring Boot 3.5
- Spring AMQP (RabbitMQ)
- Spring Data JPA + PostgreSQL 16
- Flyway (migraciones)
- springdoc-openapi (Swagger), Micrometer + Prometheus, OpenTelemetry agent

## Arquitectura

Hexagonal estricta:

- `domain/` — Java puro. `Invoice` (con máquina de estados), `Money`, `UserId`, `SessionId`, `UserDebt`, eventos, puertos.
- `application/` — casos de uso (`ProcessChargeSession`, `MarkOverdueInvoices`, `QueryInvoices`).
- `infrastructure/` — adaptadores AMQP (consumer + publisher), JPA (entities + Spring Data + adapters), scheduler, REST de consulta interna, Swagger.

## Eventos AMQP

| Dirección | Exchange | Routing key | Evento |
|---|---|---|---|
| Consume | `voltnet.charge.events` | `charge.session.completed` | `ChargeSessionCompleted` |
| Publica | `voltnet.billing.events` | `user.debt.updated`       | `UserDebtUpdated` |

DLQ asociada: `billing.charge-session-completed.dlq` vía `voltnet.billing.dlx`.

## Endpoints REST (consulta interna)

| Método | Path | Descripción |
|---|---|---|
| GET | `/invoices/{id}` | Detalle de factura |
| GET | `/invoices?userId=U-042` | Lista por usuario |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Métricas |
| GET | `/swagger-ui.html` | Swagger UI |

## Cómo correr local

```powershell
docker run -d --name pg-billing -p 5433:5432 `
  -e POSTGRES_USER=billing -e POSTGRES_PASSWORD=billing -e POSTGRES_DB=billing `
  postgres:16-alpine

docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 `
  -e RABBITMQ_DEFAULT_USER=voltnet -e RABBITMQ_DEFAULT_PASS=voltnet `
  rabbitmq:3.13-management

mvn clean package
java -jar target\ms-billing-0.1.0.jar
```

- Puerto HTTP: **8083**. Swagger: http://localhost:8083/swagger-ui.html
- RabbitMQ management: http://localhost:15672 (voltnet / voltnet)

## Sustento de R3 en código

- **Idempotencia:** `UNIQUE(session_id)` en `invoices` (migración V1) + `ProcessChargeSessionService.process(...)` devuelve la factura existente si el evento llega duplicado.
- **Aislamiento del camino crítico:** Billing no expone REST de negocio — Orchestrator nunca espera por Billing para responder al conductor.
- **Resiliencia ante caídas de Billing:** los eventos quedan en RabbitMQ; cuando Billing reaparece, drena la cola.
- **DLQ:** mensajes con payload corrupto se aíslan sin perderse.

## Tests

```powershell
mvn test
```

Tests del dominio (Money, UserId, Invoice state machine) y de aplicación (idempotencia, vencimientos) corren **sin Postgres ni RabbitMQ** — usan adaptadores en memoria.
