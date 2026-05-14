# VoltNet — Roadmap del Proyecto

> **Para qué sirve este archivo:** es la **fuente de verdad** del estado del proyecto. Sobrevive a cualquier sesión de trabajo. Si alguien (Claude o humano) entra al proyecto sin contexto, debe leer este archivo y `CLAUDE.md` para arrancar con todo claro.
>
> **Actualizar este archivo cada vez que:** se complete una fase, se tome una decisión arquitectónica nueva, se cambie el alcance, o se resuelva una pregunta pendiente con el profesor.

---

## 0. Contexto del proyecto

- **Materia:** Arquitectura de Software II — 2026-1
- **Caso de estudio:** #2 — VoltNet: Gestión de Carga Eléctrica Urbana
- **Repositorio:** https://github.com/SebasMedina22/arquitectura-2-voltnet
- **Carpeta local:** `C:\dev\voltnet-ev-platform\`
- **Modalidad de sustentación:** **individual** (cada miembro debe poder defender todo)
- **Peso de la sustentación oral:** **40%** de la nota final

### Documentos clave del proyecto
| Archivo | Para qué sirve |
|---|---|
| [`README.md`](README.md) | Visión general + tabla de entregables + URLs |
| [`ROADMAP.md`](ROADMAP.md) (este archivo) | Estado del proyecto + plan de fases |
| [`CLAUDE.md`](CLAUDE.md) | Instrucciones permanentes para Claude en este repo |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | **Documento maestro** — el que se lee para sustentar |
| [`docs/RFC.md`](docs/RFC.md) | RFC formato académico (entregable PDF) |
| [`docs/diagrams/c4/`](docs/diagrams/c4/) | Diagramas C4 L1 y L2 en PlantUML |

---

## 1. Stack tecnológico definitivo

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 25 LTS (Eclipse Temurin) |
| Framework | Spring Boot | 3.4 |
| Build tool | Maven | 3.9.15 |
| Comunicación síncrona | OpenFeign | (incluido en Spring Cloud) |
| Comunicación asíncrona | RabbitMQ + Spring AMQP | RabbitMQ 3.13 |
| DB Orchestrator | MySQL | 8 |
| DB GridLoad | Redis | 7 |
| DB Billing | PostgreSQL | 16 |
| Resiliencia | Resilience4j | (Circuit Breaker) |
| Observabilidad — métricas | Prometheus + Micrometer | latest |
| Observabilidad — trazas | Jaeger + OpenTelemetry Java Agent | latest |
| Observabilidad — dashboards | Grafana | latest |
| API Gateway (bonus) | Nginx | 1.27 |
| UI (bonus) | React + Vite | React 18 |
| Documentación API | springdoc-openapi | latest |
| Containerización | Docker + Docker Compose | latest |

---

## 2. Decisiones arquitectónicas cerradas

1. **Hexagonal estricto** en los 3 microservicios (`domain/`, `application/`, `infrastructure/`).
2. **MS-Billing es 100% asíncrono** — no expone REST. Se sincroniza con Orchestrator vía eventos (`UserDebtUpdated` para la regla R2, `ChargeSessionCompleted` para R3).
3. **Patrón Transactional Outbox** en Orchestrator para garantizar entrega de eventos al broker incluso si está caído.
4. **Proyección local de solvencia** en Orchestrator (tabla `users` en MySQL) — alimentada por eventos asíncronos de Billing.
5. **Patrones GoF implementados:** Strategy, Adapter, Factory, Observer (los 4, código en `ARCHITECTURE.md` §8).
6. **Atributos de calidad críticos (ISO 25010):** Rendimiento, Fiabilidad, Mantenibilidad.
7. **RabbitMQ sobre Kafka** — simplicidad operacional, suficiente throughput.
8. **Política "en duda, rechazar"** para R1 cuando GridLoad cae (seguridad sobre disponibilidad).
9. **Bonus activados:** Nginx (API Gateway) + UI (React + Vite).

---

## 3. Preguntas pendientes para el profesor

| # | Pregunta | Argumentación | Estado |
|---|---|---|---|
| 1 | ¿MS-Billing puede exponer un endpoint REST adicional para R2, o debe ser 100% async vía broker? | La arquitectura sugerida lo etiqueta como "Broker / PostgreSQL". Hicimos opción async pura (más estricta) — pero implica más código (proyección + consumer). Si el profesor acepta REST en Billing, podemos simplificar significativamente. | ⏳ Pendiente — clase del viernes |

> Si la respuesta es "puede tener REST también", el cambio en el código es relativamente pequeño y se documenta en `ARCHITECTURE.md`. Si dice "solo async", seguimos con el plan actual.

---

## 4. Plan de fases

### Fase 0 — Setup ✅ **COMPLETADA**
- [x] Crear `C:\dev\voltnet-ev-platform\`
- [x] Clonar repo desde GitHub
- [x] Estructura de carpetas (services, infra, docs, ui)
- [x] `.gitignore` para Java/Maven/Node/IDE/Docker
- [x] `README.md` con tabla de entregables
- [x] Skeletons de `docs/ARCHITECTURE.md` y `docs/RFC.md`
- [x] Configuración de variables de entorno: `JAVA_HOME`, `MAVEN_HOME`
- [x] Verificación: Java 25 + Maven 3.9.15 funcionando

### Fase 1 — Documentación de diseño ✅ **COMPLETADA**
- [x] Diagrama C4 L1 (Contexto) — `c4-l1-context.puml` + PNG
- [x] Diagrama C4 L2 (Contenedores) — `c4-l2-containers.puml` + PNG
- [x] `ARCHITECTURE.md` completo (~750 líneas) — documento maestro
  - 13 secciones: problema, 3 reglas, MS detalle, comunicación, persistencia, hexagonal, GoF, SOLID, observabilidad, atributos ISO 25010, trade-offs, guion oral, glosario
- [x] `RFC.md` formato académico (5 secciones según plantilla del profe)
- [x] `ROADMAP.md` (este archivo)
- [x] `CLAUDE.md` con instrucciones permanentes

### Fase 2 — MS-GridLoad ⏳ **SIGUIENTE**
- [ ] `pom.xml` con Spring Boot, springdoc-openapi, Redis client, Micrometer, OpenTelemetry agent
- [ ] Estructura hexagonal: `domain/`, `application/`, `infrastructure/`
- [ ] Endpoints REST:
  - [ ] `GET /grid/load?stationId={id}` — consulta carga actual
  - [ ] `POST /grid/load` — admin: setear/simular carga
- [ ] Adaptador Redis (Spring Data Redis)
- [ ] Configuración Swagger UI
- [ ] Health endpoint para Docker
- [ ] `Dockerfile` multi-stage
- [ ] Tests unitarios del dominio
- [ ] Datos de prueba (seed) — 3-5 estaciones con cargas iniciales variadas

### Fase 3 — MS-Billing
- [ ] `pom.xml` con Spring Boot, Spring AMQP, JPA + PostgreSQL driver, Flyway, observabilidad
- [ ] Estructura hexagonal
- [ ] Entidades de dominio: `Invoice`, `UserDebt`, `BillingEvent`
- [ ] Consumer AMQP de `ChargeSessionCompleted`
  - [ ] Idempotencia con `UNIQUE(session_id)` en `invoices`
  - [ ] DLQ configurada
- [ ] Producer AMQP de `UserDebtUpdated`
- [ ] Scheduled job para marcar facturas vencidas (cron diario simulado a frecuencia rápida en demo)
- [ ] Migraciones Flyway: `invoices`, `user_debts`, `billing_events`
- [ ] `Dockerfile`
- [ ] Tests del dominio

### Fase 4 — MS-ChargeOrchestrator (el corazón)
- [ ] `pom.xml` con Spring Boot, OpenFeign, Resilience4j, Spring AMQP, JPA + MySQL, Flyway, observabilidad
- [ ] Estructura hexagonal estricta
- [ ] Dominio:
  - [ ] Entidades: `ChargeSession`, `User`
  - [ ] Value Objects: `SessionId`, `UserId`, `StationId`, `Kwh`
  - [ ] Eventos de dominio: `ChargeSessionCompleted`
  - [ ] Puertos: `GridLoadPort`, `ChargeSessionRepository`, `UserRepository`, `DomainEventPublisher`
- [ ] Patrones GoF implementados:
  - [ ] Strategy — `ChargeStartPolicy` con `GridCapacityPolicy` + `UserSolvencyPolicy`
  - [ ] Adapter — `GridLoadFeignAdapter`
  - [ ] Factory — `ChargeSessionFactory`
  - [ ] Observer — `DomainEventPublisher` + outbox
- [ ] Casos de uso:
  - [ ] `StartChargeSessionUseCase` (valida R1 + R2)
  - [ ] `StopChargeSessionUseCase` (cierra sesión + outbox event para R3)
  - [ ] `GetChargeSessionUseCase` / `ListUserSessionsUseCase`
- [ ] Endpoints REST:
  - [ ] `POST /sessions/start`
  - [ ] `POST /sessions/{id}/stop`
  - [ ] `GET /sessions/{id}`
  - [ ] `GET /sessions?userId=...`
- [ ] Outbox + worker (`@Scheduled` cada 1s)
- [ ] Consumer AMQP de `UserDebtUpdated` → actualiza tabla local `users`
- [ ] Feign client para GridLoad + Circuit Breaker (Resilience4j)
- [ ] Migraciones Flyway: `charge_sessions`, `users`, `outbox_events`
- [ ] Tests del dominio (sin Spring)
- [ ] Tests de integración con Testcontainers (opcional)
- [ ] `Dockerfile`

### Fase 5 — Docker Compose + Observabilidad
- [ ] `docker-compose.yml` con TODOS los servicios:
  - 3 microservicios
  - MySQL 8, Redis 7, PostgreSQL 16
  - RabbitMQ 3.13 (con management plugin)
  - Prometheus
  - Grafana (con datasources y dashboards provisionados)
  - Jaeger (all-in-one)
- [ ] `infra/prometheus/prometheus.yml` con scrape configs de los 3 MS
- [ ] `infra/grafana/provisioning/datasources/` con Prometheus y Jaeger pre-configurados
- [ ] `infra/grafana/dashboards/` con dashboard "VoltNet Health"
- [ ] `infra/rabbitmq/definitions.json` con exchange y colas pre-configurados
- [ ] Health checks en todos los servicios
- [ ] Red Docker para que se vean entre sí
- [ ] Volúmenes para persistencia de DBs
- [ ] Verificar trace end-to-end en Jaeger

### Fase 6 — Bonus: Nginx + UI
- [ ] `infra/nginx/nginx.conf` con routing:
  - `/charge/*` → Orchestrator
  - `/grid/*` → GridLoad
  - `/billing/*` → Billing (si aplica)
  - `/` → UI estática
- [ ] UI React + Vite con:
  - [ ] Pantalla "Iniciar carga" (selección de estación + usuario)
  - [ ] Pantalla "Sesiones activas" con botón "Terminar"
  - [ ] Pantalla "Historial" del usuario
  - [ ] Panel de admin para simular cargas de estación
- [ ] `Dockerfile` de la UI (multi-stage: Vite build → Nginx serve)

### Fase 7 — Pulido
- [ ] Swagger UI funcional y bonito en los 3 MS
- [ ] Dashboard de Grafana revisado y con datos reales corriendo
- [ ] Traces end-to-end verificados en Jaeger (POST /sessions/start → GridLoad y vuelta)
- [ ] README final con screenshots
- [ ] RFC exportado a PDF (Pandoc o similar)
- [ ] Diagramas C4 exportados en alta resolución
- [ ] Tabla de entregables completa con todos los links

---

## 5. Convenciones del proyecto

### Lenguaje
- **Idioma:** español en documentación e identificadores funcionales; inglés en código (clases, métodos, paquetes).
- **Estilo de explicaciones técnicas:** sencillo pero no infantil. Si introduces un término técnico, explícalo o referencia el glosario.

### Git
- Trabajo directo en `main` (proyecto académico, un solo desarrollador principal).
- Commits con mensaje claro: `tipo: descripción corta` + cuerpo si es necesario.
- **No incluir** trailer `Co-Authored-By: Claude` en los mensajes.

### Código
- Java 25, target language level 21+ (registros, sealed classes, pattern matching donde aplique).
- Spring Boot 3.4 idioms (constructor injection, no `@Autowired` en campos).
- Naming: `UseCase`, `Port`, `Adapter`, `Repository`, `Service` reflejando el rol hexagonal.
- Tests del dominio sin Spring. Tests de integración con `@SpringBootTest` solo cuando sea estrictamente necesario.

---

## 6. Archivo de cambios

| Fecha | Cambio |
|---|---|
| 2026-05-13 | Inicio del proyecto. Fase 0 (setup) y Fase 1 (documentación) completadas en una sola sesión. |
