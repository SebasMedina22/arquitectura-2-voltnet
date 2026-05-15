# VoltNet — Plataforma de Gestión de Carga Eléctrica Urbana

> Proyecto integrador — **Arquitectura de Software II** (2026-1)
> Caso de estudio 2: **VoltNet — Gestión de Carga Eléctrica Urbana**

VoltNet es un ecosistema de microservicios que orquesta la carga de vehículos eléctricos en estaciones urbanas, garantizando que la red eléctrica no colapse, que los usuarios morosos no inicien carga, y que la facturación se procese de forma resiliente aunque sistemas externos estén caídos.

---

## Arquitectura en una imagen

```
                  ┌──────────────┐
                  │   Nginx GW   │  ← punto de entrada único
                  └──────┬───────┘
                         │
        ┌────────────────▼────────────────┐
        │    MS-ChargeOrchestrator        │  Core hexagonal (Spring Boot)
        │    MySQL · puerto 8081          │
        └──┬──────────────────┬───────────┘
           │ REST sync        │ async (RabbitMQ)
           ▼                  ▼
   ┌───────────────┐   ┌─────────────────┐
   │ MS-GridLoad   │   │  MS-Billing     │
   │ Redis · 8082  │   │  PostgreSQL·8083│
   └───────────────┘   └─────────────────┘
```

---

## Tabla de entregables

| Entregable | Ubicación |
|---|---|
| Documento RFC (PDF) | [`docs/RFC.md`](docs/RFC.md) → exportar a PDF |
| Diagrama C4 Nivel 1 (Contexto) | [`docs/diagrams/c4/c4-l1-context.puml`](docs/diagrams/c4/c4-l1-context.puml) |
| Diagrama C4 Nivel 2 (Contenedores) | [`docs/diagrams/c4/c4-l2-containers.puml`](docs/diagrams/c4/c4-l2-containers.puml) |
| MS-ChargeOrchestrator (código) | [`services/ms-charge-orchestrator/`](services/ms-charge-orchestrator/) |
| MS-GridLoad (código) | [`services/ms-grid-load/`](services/ms-grid-load/) |
| MS-Billing (código) | [`services/ms-billing/`](services/ms-billing/) |
| Docker Compose | [`docker-compose.yml`](docker-compose.yml) |
| Nginx (Gateway) | [`infra/nginx/`](infra/nginx/) |
| Prometheus + Grafana + Jaeger | [`infra/`](infra/) |
| UI (bonus) | [`ui/`](ui/) |

---

## Cómo levantar todo

> Requisitos: Docker Desktop corriendo. Nada más.

```bash
docker compose up -d --build
```

El primer build tarda ~3-5 min (descarga imágenes + compila los 3 servicios). Después arranca en segundos. Verifica con `docker compose ps` que los 10 contenedores estén `Up` y `(healthy)`.

URLs principales tras levantar:

| Servicio | URL | Credenciales |
|---|---|---|
| **UI (frontend) + API Gateway Nginx** | **http://localhost:8080** | — |
| MS-ChargeOrchestrator Swagger | http://localhost:8081/swagger-ui.html | — |
| MS-GridLoad Swagger | http://localhost:8082/swagger-ui.html | — |
| MS-Billing Swagger | http://localhost:8083/swagger-ui.html | — |
| RabbitMQ Management | http://localhost:15672 | voltnet / voltnet |
| Prometheus | http://localhost:9090 | — |
| Grafana (dashboard "VoltNet Health") | http://localhost:3000 | admin / admin (anonymous viewer permitido) |
| Jaeger UI | http://localhost:16686 | — |

### Demo rápida de las 3 reglas de negocio

```bash
# R1 OK: estación con carga baja
curl -X POST http://localhost:8081/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"userId":"USR-001","stationId":"STN-001"}'
# -> 201 Created

# R1 bloquea: estación sobrecargada (105 kW > 100 kW)
curl -X POST http://localhost:8081/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"userId":"USR-001","stationId":"STN-003"}'
# -> 422 R1_GRID_OVERLOADED

# R2 bloquea: usuario con deuda > 30 días (seed: USR-003 con 45 d)
curl -X POST http://localhost:8081/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"userId":"USR-003","stationId":"STN-001"}'
# -> 422 R2_USER_NOT_SOLVENT

# R3: detener Billing, iniciar+cerrar sesión, levantar Billing, ver la factura creada
docker compose stop ms-billing
SID=$(curl -s -X POST http://localhost:8081/sessions/start \
  -H "Content-Type: application/json" \
  -d '{"userId":"USR-001","stationId":"STN-001"}' | jq -r .sessionId)
curl -X POST "http://localhost:8081/sessions/$SID/stop" \
  -H "Content-Type: application/json" -d '{"kwhConsumed":18.7}'
# -> 200 OK aunque Billing esté abajo (evento queda en outbox)
docker compose start ms-billing
sleep 8 && curl "http://localhost:8083/invoices?userId=USR-001"
# -> factura creada por el consumer cuando Billing volvió
```

---

## Stack tecnológico

| Capa | Tecnología | Por qué |
|---|---|---|
| Lenguaje | Java 25 LTS | Última LTS, soportada por Spring Boot 3.5+ |
| Framework | Spring Boot 3.5 | Estándar de facto para microservicios JVM |
| Comunicación síncrona | OpenFeign | Cliente REST declarativo, integración nativa con Spring |
| Comunicación asíncrona | RabbitMQ 3.13 + Spring AMQP | Más simple que Kafka para este escenario |
| Persistencia SQL | MySQL 8 (Orchestrator), PostgreSQL 16 (Billing) | Persistencia políglota como exige la rúbrica |
| Persistencia in-memory | Redis 7 (GridLoad) | Lectura sub-ms para el path crítico de validación |
| Observabilidad | Prometheus + Grafana + Jaeger + OpenTelemetry | Stack abierto estándar |
| Gateway | Nginx | Bonus — punto de entrada único |
| UI | React 18 + Vite | Bonus — SPA mínima |
| Documentación API | springdoc-openapi | Swagger UI autogenerado |

Para la justificación de cada decisión y el análisis de trade-offs, ver [`docs/RFC.md`](docs/RFC.md).

---

## Equipo

| Integrante | Rol |
|---|---|
| Sebastián Medina | _por definir_ |
| _Pendiente_ | _por definir_ |

---

## Estado del proyecto

| Fase | Estado |
|---|---|
| Documentación de diseño (C4 L1/L2, RFC, ARCHITECTURE) | ✅ |
| MS-GridLoad (REST + Redis) | ✅ |
| MS-Billing (RabbitMQ + PostgreSQL + Outbox idempotente) | ✅ |
| MS-ChargeOrchestrator (hexagonal, los 4 GoF, R1/R2/R3) | ✅ |
| Docker Compose + Observabilidad (Prometheus/Grafana/Jaeger) | ✅ |
| **API Gateway Nginx + UI React (bonus)** | ✅ |
| RFC PDF final | 🚧 |

Para el diseño detallado y el análisis de trade-offs, ver [`docs/RFC.md`](docs/RFC.md).
