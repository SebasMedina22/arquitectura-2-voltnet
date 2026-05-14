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
docker compose up --build
```

URLs principales tras levantar:

| Servicio | URL |
|---|---|
| API Gateway (Nginx) | http://localhost |
| MS-ChargeOrchestrator Swagger | http://localhost:8081/swagger-ui.html |
| MS-GridLoad Swagger | http://localhost:8082/swagger-ui.html |
| MS-Billing Swagger | http://localhost:8083/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Jaeger UI | http://localhost:16686 |
| UI (bonus) | http://localhost:5173 |

---

## Stack tecnológico

| Capa | Tecnología | Por qué |
|---|---|---|
| Lenguaje | Java 25 LTS | Última LTS, soportada por Spring Boot 3.4+ |
| Framework | Spring Boot 3.4 | Estándar de facto para microservicios JVM |
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

🚧 **En construcción** — ver [`docs/RFC.md`](docs/RFC.md) para el diseño detallado.
