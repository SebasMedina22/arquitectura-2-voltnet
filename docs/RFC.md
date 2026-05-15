# RFC: VoltNet — Plataforma de Gestión de Carga Eléctrica Urbana

**Autores:** Sebastián Medina · _(pendientes integrantes del equipo)_
**Materia:** Arquitectura de Software II — 2026-1
**Caso de estudio:** #2 — VoltNet: Gestión de Carga Eléctrica Urbana
**Fecha de diseño:** 2026-05-13
**Fecha de cierre de implementación:** 2026-05-15
**Versión:** 1.1
**Estado:** Implementado y verificado end-to-end (R1, R2 y R3 demostrables; observabilidad y bonus Nginx+UI incluidos)

---

## 1. Resumen Ejecutivo

### El problema

"VoltNet" gestiona una red de cargadores eléctricos urbanos. Si todos los vehículos cargan simultáneamente a máxima potencia, la red eléctrica del barrio colapsa. Adicionalmente, la empresa debe (1) impedir el uso del servicio a usuarios con deuda vencida y (2) garantizar que la facturación nunca bloquee al conductor en la calle, incluso si los sistemas de cobro están caídos.

### La solución propuesta

Un ecosistema de **tres microservicios** que separa responsabilidades por perfil de carga, latencia y disponibilidad:

1. **MS-ChargeOrchestrator** (núcleo hexagonal, MySQL) — orquesta sesiones de carga y aloja las 3 reglas de negocio.
2. **MS-GridLoad** (REST sync, Redis) — responde en sub-milisegundo cuánta carga consume cada estación.
3. **MS-Billing** (broker async, PostgreSQL) — genera facturas a partir de eventos de consumo; nunca está en el camino crítico del usuario.

La comunicación combina **REST síncrono** (Feign) para validaciones críticas y **mensajería asíncrona** (RabbitMQ) para flujos de facturación y sincronización de estado. Se aplica el patrón **Transactional Outbox** para garantizar entrega exactly-once-effective sin necesidad de transacciones distribuidas.

### Resultado

Un conductor puede iniciar y cerrar cargas con latencia p95 < 300 ms, el sistema valida automáticamente las 3 reglas del caso de estudio en la capa de dominio, y el cierre de sesión es resiliente a fallos de los sistemas de facturación.

---

## 2. Atributos de Calidad

Siguiendo la **clasificación estándar de atributos de calidad** del curso (Rendimiento, Seguridad, Usabilidad, Fiabilidad, Mantenibilidad, Escalabilidad, Compatibilidad, Portabilidad), se identifican **tres atributos críticos** para VoltNet según los requisitos del caso de estudio.

### 2.1 Rendimiento / Eficiencia

**Definición operacional:** `POST /sessions/start` con latencia p95 < 300 ms en condiciones normales (GridLoad respondiendo en < 50 ms).

**Mecanismos que lo soportan:**
- **Redis** en GridLoad para respuesta sub-milisegundo del lado del servidor.
- **Proyección local de solvencia** en Orchestrator (MySQL local) para validar R2 sin llamada remota.
- **Feign con timeout agresivo** (500 ms) y **Circuit Breaker** (Resilience4j) para fallar rápido si GridLoad degrada.

**Métrica objetivo:** p95 < 300 ms, p99 < 800 ms.

### 2.2 Fiabilidad (madurez, disponibilidad, tolerancia a fallos)

**Definición operacional:** `POST /sessions/{id}/stop` debe responder exitosamente al usuario aunque MS-Billing y/o RabbitMQ estén caídos. Adicionalmente, cada sesión completada genera exactamente una factura (exactly-once-effective).

**Mecanismos que la soportan:**
- **Patrón Transactional Outbox**: el evento de consumo se persiste en la misma transacción que cierra la sesión. Un worker lo publica al broker después; si el broker está caído, reintenta sin afectar la respuesta al usuario.
- **Aislamiento de fallos**: Orchestrator no depende de la disponibilidad de Billing para responder al usuario final.
- **Idempotencia en el consumer**: `UNIQUE(session_id)` en la tabla `invoices` rechaza duplicados a nivel de DB.
- **Dead Letter Queues**: aislamiento de mensajes problemáticos sin pérdida.

**Métrica objetivo:** 99.9% de cierres exitosos incluso bajo caída total de Billing; 0 facturas duplicadas.

### 2.3 Mantenibilidad

**Definición operacional:** facilidad para modificar, actualizar y corregir el sistema. Cambios de tecnología o reglas de negocio deben tocar partes localizadas del código.

**Mecanismos que la soportan:**
- **Arquitectura Hexagonal** con separación estricta dominio/aplicación/infraestructura — cambios técnicos no contaminan el dominio.
- **Principios SOLID** aplicados sistemáticamente.
- **Patrones GoF (Strategy, Adapter, Factory, Observer)** que hacen el código predecible.
- **Tests del dominio sin Spring ni DB** — el feedback loop al modificar reglas de negocio es de milisegundos. La suite total del proyecto corre en menos de 5 segundos (55 tests verdes entre los 3 MS).
- **Migraciones de schema versionadas con Flyway** en Orchestrator y Billing — el estado de la base es reproducible y auditable.

**Métrica objetivo:** un desarrollador nuevo entiende el flujo principal en menos de 2 horas; cambios típicos a una regla de negocio no requieren tocar más de 2 clases. Verificable abriendo `domain/policy/GridCapacityPolicy.java` y `domain/policy/UserSolvencyPolicy.java` — cada regla vive en un único archivo de ~30 líneas.

---

## 3. Decisiones Arquitectónicas

### 3.1 Lenguaje y framework

| Decisión | **Java 25 LTS + Spring Boot 3.5** |
|---|---|
| Alternativas evaluadas | Node.js/NestJS, Python/FastAPI, .NET 8 |
| Justificación | (1) El profesor lo recomienda explícitamente en la guía del proyecto. (2) Ecosistema maduro para los patrones requeridos: Feign declarativo, Spring AMQP, springdoc-openapi, Micrometer + OpenTelemetry. (3) Equipo con experiencia en JVM. |

### 3.2 Persistencia políglota

| MS | DB | Justificación |
|---|---|---|
| **Orchestrator** | **MySQL 8** | Datos transaccionales con ACID requerido para outbox. Modelo relacional simple. |
| **GridLoad** | **Redis 7** | Lectura key-value sub-ms en camino crítico. Sin necesidad de esquema relacional ni durabilidad estricta (los datos se regeneran). |
| **Billing** | **PostgreSQL 16** | Datos financieros con tipo NUMERIC preciso, JSONB para auditoría flexible, y soporte para queries analíticas (window functions). |

Esta combinación **SQL + NoSQL** cumple el requisito de "persistencia políglota" de la rúbrica y cada elección está técnicamente justificada por el perfil de carga del servicio.

### 3.3 Comunicación inter-servicios

| Tipo | Tecnología | Cuándo se usa |
|---|---|---|
| **Sync** | OpenFeign (declarativo, integración Spring nativa) | Orchestrator → GridLoad, **únicamente** durante validación R1 al iniciar carga |
| **Async** | RabbitMQ 3.13 + Spring AMQP | Orchestrator ↔ Billing en ambas direcciones (cierre de sesión y sincronización de deudas) |

**RabbitMQ sobre Kafka:** se eligió Rabbit por (1) menor complejidad operacional, (2) UI de management embebida, (3) throughput suficiente para el caso (no se esperan millones de eventos/seg). Kafka habría sido overkill.

### 3.4 Arquitectura interna de cada MS

**Arquitectura Hexagonal (Ports & Adapters)** con separación estricta en tres capas:

- `domain/` — entidades, value objects, eventos, puertos (interfaces). **Cero dependencias de frameworks.**
- `application/` — casos de uso que orquestan el dominio.
- `infrastructure/` — adaptadores REST, JPA, Feign, AMQP.

La regla de dependencias va de fuera hacia adentro. El dominio no conoce Spring, JPA ni HTTP.

### 3.5 Patrones de diseño aplicados (GoF + adicionales)

| Patrón | Categoría | Dónde se aplica |
|---|---|---|
| **Strategy** | Comportamiento | Políticas de validación al iniciar carga (`GridCapacityPolicy`, `UserSolvencyPolicy`) |
| **Adapter** | Estructural | Cliente Feign de GridLoad adapta al puerto `GridLoadPort` del dominio |
| **Factory** | Creacional | `ChargeSessionFactory` centraliza la construcción del agregado con invariantes |
| **Observer / Pub-Sub** | Comportamiento | Eventos de dominio publicados al broker; Billing reacciona como observador remoto |
| **Transactional Outbox** | Patrón de integración (no GoF) | Garantía de entrega de eventos sin transacciones distribuidas |
| **Circuit Breaker** (Resilience4j) | Resiliencia | Protección de Orchestrator contra fallos de GridLoad |

### 3.6 Observabilidad

Stack abierto y estándar: **Prometheus** (métricas), **Grafana** (dashboards), **Jaeger** (trazas distribuidas), instrumentado vía **Micrometer** y **OpenTelemetry Java Agent**.

### 3.7 Decisiones de bonificación

- **Nginx** como API Gateway: punto de entrada único, enrutamiento por prefijo de path.
- **React 18 + Vite** como UI: SPA mínima para demostrar consumo del MS Principal.

---

## 4. Trade-offs

| # | Decisión | Beneficio | Costo aceptado |
|---|---|---|---|
| 1 | **Microservicios sobre monolito** | Escalado y SLAs independientes por servicio | Mayor complejidad operacional (más DBs, broker, observabilidad cross-service) |
| 2 | **Comunicación asíncrona Orchestrator ↔ Billing** | Desacoplamiento temporal; Billing puede estar caído sin afectar al usuario | **Consistencia eventual**: la deuda del usuario en Orchestrator puede tener segundos de atraso respecto a Billing |
| 3 | **Transactional Outbox** | Entrega garantizada de eventos sin 2PC ni Sagas | Una tabla extra, un worker en background, complejidad operacional adicional |
| 4 | **Proyección local de solvencia** | Latencia sub-ms en R2; sin punto de fallo extra | Stale reads en ventana de pago muy reciente (mitigable: reintento del usuario) |
| 5 | **Redis para GridLoad** | Latencia sub-ms en el camino crítico de R1 | No hay historial persistente de carga (no es requerido por el caso) |
| 6 | **RabbitMQ sobre Kafka** | Simplicidad operacional, UI de admin, suficiente para el throughput esperado | No tenemos event sourcing ni replays masivos (no los necesitamos) |
| 7 | **Hexagonal estricto** | Testabilidad del dominio sin Spring/DB/broker; reemplazo de adaptadores trivial | Mayor cantidad de carpetas y clases que un CRUD plano; curva de aprendizaje |
| 8 | **Política "en duda, rechazar" si GridLoad cae** | Seguridad de la red eléctrica preservada | Falsos negativos en ventanas de caída de GridLoad (usuario debe reintentar) |

**Trade-off central que define la arquitectura:**

> Sacrificamos **consistencia inmediata** del estado de facturación a cambio de **disponibilidad** del flujo de cierre de carga. Esto está alineado directamente con la regla R3 del enunciado del profesor, que exige explícitamente que la sesión cierre aunque facturación esté caída.

---

## 5. Diagrama C4 Nivel 2 (Contenedores)

Fuente PlantUML: [`diagrams/c4/c4-l2-containers.puml`](diagrams/c4/c4-l2-containers.puml)
Diagrama de contexto (L1): [`diagrams/c4/c4-l1-context.puml`](diagrams/c4/c4-l1-context.puml)

### Resumen narrativo del diagrama L2

- El **conductor EV** y el **operador VoltNet** acceden a la plataforma a través de la **UI Web** (React).
- La UI llama al **API Gateway (Nginx)** que enruta las peticiones a los microservicios correspondientes.
- **MS-ChargeOrchestrator** es el núcleo: recibe las peticiones de inicio/fin de sesión y orquesta las validaciones.
  - Llama síncronamente a **MS-GridLoad** vía REST (Feign) para validar R1.
  - Lee de su propia DB MySQL (proyección de solvencia) para validar R2.
  - Publica el evento `ChargeSessionCompleted` al broker RabbitMQ al cerrar (R3).
  - Consume `UserDebtUpdated` para mantener su proyección local actualizada.
- **MS-GridLoad** lee carga actual de Redis y responde.
- **MS-Billing** consume `ChargeSessionCompleted`, genera facturas en PostgreSQL, y publica `UserDebtUpdated` cuando una factura vence o se paga.
- Los tres MS exponen métricas a **Prometheus** vía `/actuator/prometheus` y trazas a **Jaeger** vía OpenTelemetry. **Grafana** visualiza ambas fuentes en dashboards unificados.

Ver imagen renderizada en el README del repositorio para la entrega final.

---

## 6. Verificación end-to-end del sistema implementado

Esta sección documenta lo que efectivamente se construyó y se verificó funcionando, no solo lo que se planeó. Sirve para que cualquier lector externo (profesor, evaluador, futuro compañero del equipo) pueda contrastar la propuesta de este RFC contra lo que está corriendo en el repositorio.

### 6.1 Cobertura de mínimos de rúbrica

| Mínimo de rúbrica | Cumplimiento |
|---|---|
| Arquitectura hexagonal estricta en los 3 MS | ✅ Carpetas `domain/`, `application/`, `infrastructure/` en cada servicio. El dominio no importa Spring ni JPA. |
| Base de datos propia por servicio | ✅ MySQL (Orchestrator), Redis (GridLoad), PostgreSQL (Billing). Ninguna se comparte. |
| Principios SOLID demostrables | ✅ Aplicados explícitamente; ejemplos puntuales en cada capa hexagonal documentados en `docs/ARCHITECTURE.md` §9. |
| Mínimo 3 patrones GoF | ✅ Implementados los 4: Strategy (políticas), Adapter (Feign/JPA/AMQP), Factory (`ChargeSessionFactory`), Observer (`DomainEventPublisher` + Outbox). |
| Validación programática de las 3 reglas del caso en la capa de dominio | ✅ R1 en `domain/policy/GridCapacityPolicy`, R2 en `domain/policy/UserSolvencyPolicy`, R3 mediante cierre de sesión transaccional + outbox en `application/usecase/StopChargeSessionUseCase`. |
| Swagger/OpenAPI funcional por MS | ✅ springdoc-openapi 2.8 en los 3 MS, expuesto en `/swagger-ui.html` y `/v3/api-docs`. |
| 1 MS Principal + 1 MS Síncrono REST + 1 MS Asíncrono Broker | ✅ Orchestrator (core hexagonal) + GridLoad (REST sync) + Billing (broker async, sin REST público). |
| Comunicación síncrona vía cliente REST declarativo | ✅ OpenFeign + Spring Cloud, instrumentado con Resilience4j Circuit Breaker. |
| Comunicación asíncrona vía broker | ✅ RabbitMQ 3.13 con dos exchanges topic (`voltnet.charge.events`, `voltnet.billing.events`), dos DLX, dos DLQ. |
| Docker Compose orquesta todo con un solo comando | ✅ `docker compose up -d --build` levanta 11 contenedores. |
| Stack de observabilidad (Prometheus + Grafana + Jaeger) | ✅ Auto-provisionado: datasources de Prometheus y Jaeger en Grafana, dashboard "VoltNet Health" con 6 paneles, OTel Agent en los 3 MS exportando trazas vía OTLP. |
| Documento RFC y Modelo C4 Nivel 2 | ✅ Este documento + diagramas PlantUML renderizados en `docs/diagrams/c4/`. |
| Repositorio Git público con tabla de entregables en el README | ✅ Repositorio publicado; el `README.md` lista todas las URLs de acceso y los artefactos. |

### 6.2 Bonificaciones implementadas

| Bonificación | Cumplimiento |
|---|---|
| API Gateway | ✅ Nginx 1.27 en contenedor `ui-gateway` haciendo proxy reverso de los tres prefijos `/api/charge`, `/api/grid`, `/api/billing`. |
| Interfaz de Usuario | ✅ SPA React 18 + Vite servida por el mismo Nginx. Cuatro pestañas: iniciar/cerrar carga, sesiones del usuario, facturas, panel de simulación de carga de estación. |

### 6.3 Demostraciones funcionales verificadas

Los siguientes escenarios se probaron contra el sistema corriendo en `docker compose` y respondieron como se esperaba.

| Escenario | Solicitud | Resultado |
|---|---|---|
| R1 — camino feliz | `POST /sessions/start` con estación a 45 kW | HTTP 201, sesión creada |
| R1 — bloqueo por sobrecarga | `POST /sessions/start` con estación a 105 kW | HTTP 422, código `R1_GRID_OVERLOADED` |
| R1 — fallback "en duda rechazar" | GridLoad caído durante 10 s | Circuit Breaker abre; HTTP 503 sostenido sin reintentos al servicio caído |
| R2 — camino feliz | Usuario con deuda 15 días | HTTP 201, sesión creada |
| R2 — bloqueo por solvencia | Usuario con deuda 45 días | HTTP 422, código `R2_USER_NOT_SOLVENT` |
| R3 — cierre con Billing caído | Billing apagado, `POST /sessions/start` + `/stop` | HTTP 200; evento queda en tabla `outbox_events` con `published_at = NULL`; al reanudar Billing el worker scheduleado lo publica en <1 s y Billing crea la factura sin duplicar. |
| Idempotencia del consumer | Reintento del mismo evento `ChargeSessionCompleted` | Restricción `UNIQUE(session_id)` en `invoices` rechaza el duplicado; sigue siendo una sola factura por sesión. |
| Trazabilidad distribuida | `POST /sessions/start` desde la UI | Jaeger muestra una traza con ~12 spans cubriendo `ui-gateway → ms-charge-orchestrator → Feign → ms-grid-load → Redis` bajo un único `trace_id`. |

### 6.4 Decisiones revisadas durante implementación

Cuatro decisiones se ajustaron en la integración respecto a este RFC original. Se documentan acá por transparencia:

1. **Puerto de MS-GridLoad: 8081 → 8082.** Originalmente el RFC asumía 8081 para todos los MS detrás del gateway; al levantar `docker compose` aparecieron dos servicios pidiendo el mismo puerto host. Se reasignó GridLoad a 8082 y MS-Billing a 8083, dejando Orchestrator (el principal) en 8081.
2. **Contrato `UserId`.** Orchestrator emite identificadores en formato `USR-XXX`; el código inicial de Billing aceptaba solo `U-XXX`. Se alineó Billing al formato del caso (`USR-XXX`) y se actualizaron sus tests.
3. **Topología AMQP precargada vs creada en runtime.** El RFC contemplaba precargar exchanges y colas con `load_definitions.json`. Se descartó porque RabbitMQ ignora `RABBITMQ_DEFAULT_USER/PASS` cuando se monta un definitions, lo que rompía la autenticación de Spring AMQP. Se dejó que `RabbitMqConfig` (Spring) declare la topología al primer arranque de cada MS.
4. **API Gateway y UI: dos contenedores → uno solo.** El RFC planteaba un Nginx Gateway y un Nginx separado para servir la UI estática. La implementación los fusionó en un único contenedor `ui-gateway` que cumple ambos roles: menos imágenes, menos superficie de mantenimiento, idéntico comportamiento externo.

Ninguno de estos ajustes invalida los atributos de calidad ni las decisiones arquitectónicas centrales (§3); son detalles de empaquetado y compatibilidad.

---

## Anexos

- [`README.md`](../README.md) — Tabla de entregables, stack tecnológico, instrucciones de despliegue.
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — Documento maestro explicativo para el equipo y para la sustentación oral.
