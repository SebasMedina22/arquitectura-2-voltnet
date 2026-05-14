# Arquitectura de VoltNet — Documento Maestro

> **Audiencia:** miembros del equipo que necesitan entender el proyecto **en su totalidad** para defenderlo en la sustentación oral individual (40% de la nota final).
>
> **Cómo usar este documento:** léelo de corrido una vez para tener la foto completa. Después vuelve a las secciones específicas cuando prepares tu defensa oral. La **sección 12** (Guion de Sustentación) resume los argumentos que debes saberte casi de memoria. La **sección 13** (Glosario) explica cada término técnico que aparece.

---

## Índice

1. [El problema de negocio](#1-el-problema-de-negocio)
2. [Las 3 reglas de negocio críticas](#2-las-3-reglas-de-negocio-críticas)
3. [Visión general de la arquitectura](#3-visión-general-de-la-arquitectura)
4. [Los 3 microservicios en detalle](#4-los-3-microservicios-en-detalle)
5. [Comunicación entre servicios (sync y async)](#5-comunicación-entre-servicios-sync-y-async)
6. [Persistencia políglota](#6-persistencia-políglota--por-qué-cada-ms-usa-la-db-que-usa)
7. [Arquitectura hexagonal](#7-arquitectura-hexagonal--cómo-está-organizado-el-código)
8. [Patrones GoF aplicados](#8-patrones-gof-aplicados)
9. [Principios SOLID](#9-principios-solid--dónde-y-cómo)
10. [Observabilidad](#10-observabilidad)
11. [Atributos de calidad y trade-offs](#11-atributos-de-calidad-y-trade-offs)
12. [Guion de sustentación oral](#12-guion-de-sustentación-oral)
13. [Glosario](#13-glosario)

---

## 1. El problema de negocio

**VoltNet** es una empresa que administra cargadores para vehículos eléctricos (EVs) en una ciudad. Su problema principal:

> *"Si todos los autos cargan al mismo tiempo a máxima potencia, la red eléctrica del barrio colapsa."*

Hay dos problemas asociados:

- **Solvencia del usuario:** no quieren dejar cargar a alguien que ya tiene deuda vencida (le aumenta el riesgo de impago).
- **Resiliencia del cobro:** el sistema de facturación es lento y puede caerse, pero un conductor parado en la calle no debería quedarse bloqueado esperando a que "facturación responda".

**Lo que se necesita técnicamente:** un **orquestador** que (1) valide el estado de la estación en tiempo real, (2) autorice al usuario, y (3) registre el consumo para cobrar después, **sin bloquear al usuario en la estación**.

---

## 2. Las 3 reglas de negocio críticas

Estas tres reglas vienen del enunciado del profesor y son **el corazón del dominio**. Están implementadas en la capa de **dominio** del MS-ChargeOrchestrator.

### R1 — Capacidad de red (validación síncrona y remota)

> *"No se puede iniciar una carga si el estado actual de la estación (verificado síncronamente) indica una carga total de red > 100 kW."*

**Aclaración importante:** los 100 kW se refieren a la **carga total de la estación**, no a un auto individual. Una estación tiene varios cargadores físicos; si entre todos los autos conectados están consumiendo más de 100 kW, no se admiten cargas nuevas.

**Cómo se implementa:** cuando llega `POST /sessions/start`, el Orquestador llama síncronamente a **MS-GridLoad** vía REST (con Feign). Si la respuesta es `> 100 kW`, rechaza con `422 GridOverloaded`. Es la **única** validación que requiere salir a otro servicio durante el inicio.

### R2 — Solvencia del usuario (validación síncrona pero local)

> *"El sistema debe verificar que el usuario tenga un método de pago activo; si tiene deudas vencidas de más de 30 días, el inicio de carga se bloquea automáticamente."*

**Cómo se implementa:** el Orquestador **NO** llama a Billing vía REST. En su lugar, consulta su **tabla local `users`** en MySQL (respuesta sub-1ms). Esa tabla se mantiene actualizada por eventos: cada vez que en Billing una factura vence o se paga, Billing publica un evento `UserDebtUpdated` al broker; el Orquestador lo consume y actualiza su tabla local.

**¿Por qué no llamar a Billing directamente?** Para no acoplarnos a su disponibilidad. Si Billing está caído, igual podemos validar solvencia porque tenemos la información sincronizada localmente.

### R3 — Cierre resiliente con facturación asíncrona

> *"Al finalizar, el cálculo de kW consumidos se envía a un sistema de facturación asíncrono. La sesión se cierra exitosamente aunque el sistema de facturación esté fuera de línea."*

**Cómo se implementa:** cuando llega `POST /sessions/{id}/stop`, el Orquestador hace **dos cosas en la misma transacción de MySQL**:
1. Marca la sesión como `COMPLETED` con los kWh consumidos.
2. Escribe un registro en la tabla **`outbox_events`**.

Un worker independiente lee la outbox y publica el evento a RabbitMQ. Si el broker o Billing están caídos, el evento se queda en outbox y se reintenta más tarde — **la respuesta al usuario es siempre 200 OK**.

> **¿Y si Billing está caído cómo se llega a facturar?** El evento queda esperando en la cola de RabbitMQ (los mensajes son durables y sobreviven reinicios). Cuando Billing vuelve a estar disponible, lee los mensajes pendientes y genera las facturas. El usuario nunca se entera del retraso.

---

## 3. Visión general de la arquitectura

VoltNet es un ecosistema de **3 microservicios** + **3 bases de datos** + **1 broker** + **stack de observabilidad** + **gateway** + **UI** (bonus).

### Diagrama C4 Nivel 2 — Contenedores

![Diagrama C4 L2 — Contenedores de VoltNet](./diagrams/c4/c4-l2-containers.png)

> Fuente PlantUML: [`diagrams/c4/c4-l2-containers.puml`](./diagrams/c4/c4-l2-containers.puml) · Diagrama de contexto (L1): [`diagrams/c4/c4-l1-context.png`](./diagrams/c4/c4-l1-context.png)

| Microservicio | Rol | Tecnología | DB |
|---|---|---|---|
| **MS-ChargeOrchestrator** | Núcleo (Hexagonal) | Spring Boot 3.4 / Java 25 | MySQL 8 |
| **MS-GridLoad** | Síncrono (REST) | Spring Boot 3.4 / Java 25 | Redis 7 |
| **MS-Billing** | Asíncrono (Broker) | Spring Boot 3.4 / Java 25 | PostgreSQL 16 |

Y alrededor:
- **RabbitMQ 3.13** — el broker de mensajes (intermediario asíncrono).
- **Nginx** — API Gateway, el "portero" del sistema (bonus).
- **Prometheus + Grafana + Jaeger** — stack de observabilidad.
- **React + Vite** — la interfaz web (bonus).

---

## 4. Los 3 microservicios en detalle

### 4.1 MS-ChargeOrchestrator (Núcleo)

**Responsabilidad:** orquestar el ciclo de vida de una sesión de carga. Es el único MS que conoce las 3 reglas de negocio.

**API REST que expone:**
- `POST /sessions/start` — inicia una sesión. Valida R1 (consulta remota a GridLoad) y R2 (consulta local a su DB). Devuelve `sessionId` o un error 422.
- `POST /sessions/{id}/stop` — cierra una sesión. Calcula kWh consumidos, persiste, publica evento.
- `GET /sessions/{id}` — consulta el estado de una sesión.
- `GET /sessions?userId=...` — historial de sesiones de un usuario.

**Dependencias que tiene hacia afuera:**
- Llamada REST síncrona → MS-GridLoad (usando Feign).
- Publica mensajes → RabbitMQ (evento `ChargeSessionCompleted`).
- Consume mensajes → RabbitMQ (evento `UserDebtUpdated` para mantener su tabla local de solvencia).
- Conexión JDBC → MySQL.

**Por qué hexagonal aquí:** este servicio concentra toda la lógica de negocio importante. Necesitamos poder probar las reglas R1, R2 y R3 **sin levantar Spring, sin conectar a MySQL, sin necesitar RabbitMQ**. La arquitectura hexagonal hace que eso sea trivial — el dominio es Java puro y testeable en milisegundos.

### 4.2 MS-GridLoad (Síncrono)

**Responsabilidad única:** responder rápidamente cuánta carga está consumiendo cada estación.

**API REST que expone:**
- `GET /grid/load?stationId={id}` — devuelve `{ stationId, currentLoadKw, timestamp }`. Es el endpoint que el Orquestador llama para validar R1.
- `POST /grid/load` — endpoint de administración para **simular** cambios en la carga de las estaciones.

**Aclaración sobre la simulación:**

No tenemos cargadores eléctricos reales conectados al proyecto. Necesitamos una forma de "decirle al sistema" qué carga tiene cada estación para poder probar la regla R1. El endpoint `POST /grid/load` nos permite hacer cosas como: "STN-001 ahora tiene 95 kW", luego intentar iniciar una sesión y verificar que el sistema la acepte; después subirla a 105 kW e intentar otra vez, comprobando que ahora rechaza con 422. **Esto es solo para demo y pruebas** — en un sistema real, los sensores de los cargadores enviarían esos datos automáticamente.

**Por qué Redis y no SQL:** este endpoint está en el "camino crítico" — cada vez que un usuario quiere iniciar una carga, pasa por aquí. Necesitamos respuesta **en menos de 10 milisegundos**. Una lectura de MySQL (con su round-trip al disco) sería más lenta. Redis guarda todo en memoria RAM, lectura sub-milisegundo.

**Cómo se ven los datos en Redis:**
```
station:STN-001:load_kw → 45.3
station:STN-002:load_kw → 92.8
station:STN-003:load_kw → 105.0   ← esta bloquea inicios
```

### 4.3 MS-Billing (Asíncrono)

**Responsabilidad:** generar facturas a partir de eventos de consumo, y gestionar deudas de usuarios.

**No expone endpoints REST públicos.** Solo se comunica vía broker (consume y produce mensajes).

**Flujos principales:**

1. **Consume el evento `ChargeSessionCompleted`** que publica el Orquestador al cerrar una sesión.
   → Calcula el monto a facturar (`kWh × tarifa`).
   → Crea una fila en la tabla `invoices` con estado `PENDING`.
   → Si pasa el plazo sin pago, la factura cambia a `OVERDUE` y se publica un evento `UserDebtUpdated`.

2. **Publica el evento `UserDebtUpdated`** cada vez que cambia la situación de deuda de un usuario.
   Ejemplo: `{ userId: "U-42", overdueDays: 35, hasActivePaymentMethod: true }`.
   El Orquestador consume estos eventos para actualizar su tabla local de usuarios.

**Idempotencia:** la columna `session_id` en la tabla `invoices` tiene una restricción `UNIQUE`. Si por algún motivo el mismo evento llega dos veces (por reintento del broker), el segundo `INSERT` falla por la restricción y se descarta el mensaje. Resultado: **una sola factura por sesión**, sin importar cuántas veces se entregue el evento.

---

## 5. Comunicación entre servicios (sync y async)

### Síncrona — REST con Feign

**Se usa en un solo lugar:** `Orchestrator → GridLoad` durante `POST /sessions/start`, para validar la regla R1.

**¿Por qué es síncrona aquí?** Porque el caso de estudio dice que la verificación es "síncrona". El usuario está esperando una respuesta de "¿puedo cargar o no?". No podemos diferir esa decisión a un proceso en background.

**Qué es Feign:** una librería de Spring que te permite llamar a otros servicios REST **escribiendo solo una interfaz Java** con anotaciones. Spring se encarga de generar el código HTTP por debajo. Ejemplo: en vez de escribir manualmente un cliente HTTP con headers, JSON, manejo de errores, etc., escribes:

```java
@FeignClient(name = "grid-load", url = "${grid.load.url}")
public interface GridLoadClient {
    @GetMapping("/grid/load")
    GridLoadResponse getLoad(@RequestParam String stationId);
}
```

Y listo, Spring crea automáticamente la implementación.

**Mitigaciones cuando GridLoad falla:**
- **Timeout corto (500 ms)** — si GridLoad no responde en medio segundo, asumimos que está caído y devolvemos `503 GridUnavailable`. **Política: en duda, rechazar la carga** — preferimos negar una carga que dejar pasar una sobrecarga real de la red.
- **Circuit Breaker (Resilience4j)** — es la analogía del "breaker" eléctrico de tu casa. Si GridLoad falla N veces seguidas (por ejemplo, 5 errores en 30 segundos), el "circuito se abre": durante los próximos 30 segundos, el Orquestador **ni siquiera intenta** llamar a GridLoad — devuelve error inmediatamente. Esto evita que mil peticiones de usuarios se acumulen esperando una respuesta que no va a llegar. Después del periodo, el breaker prueba "una de tanteo": si responde bien, se cierra y vuelve a permitir el tráfico normal.

### Asíncrona — RabbitMQ con Spring AMQP

**Se usa en:**
- `Orchestrator → Billing` — evento `ChargeSessionCompleted` al cerrar sesión (R3).
- `Billing → Orchestrator` — evento `UserDebtUpdated` cuando una factura vence o se paga.

**Qué es AMQP:** "Advanced Message Queuing Protocol", el lenguaje que habla RabbitMQ. Tiene dos roles:
- **Producer (productor):** el que publica mensajes al broker.
- **Consumer (consumidor):** el que los lee.

**Estructura de RabbitMQ en nuestro proyecto:**
- **Exchange:** `voltnet.events` (un "router" de mensajes, tipo `topic`).
- **Routing keys:** `session.completed` y `user.debt.updated` (etiquetas que distinguen los mensajes).
- **Colas:**
  - `session.completed.q` (consumida por Billing).
  - `user.debt.updated.q` (consumida por Orchestrator).
- **Dead Letter Queues (DLQ):** cada cola tiene una "cola hermana" para mensajes muertos. Si un consumidor falla repetidamente al procesar un mensaje (5 intentos por ejemplo), el mensaje termina en la DLQ en lugar de seguir reintentándose para siempre. Un humano puede inspeccionar la DLQ y decidir qué hacer.

**Patrón Transactional Outbox** en el productor — explicado en R3 arriba. Garantiza "si se commiteó la sesión en DB, el evento se publicará al broker, sí o sí, eventualmente".

---

## 6. Persistencia políglota — por qué cada MS usa la DB que usa

La rúbrica del profe exige **persistencia políglota (SQL + NoSQL)**. Más allá del requisito formal, cada elección tiene su razón técnica:

| MS | DB | Razón |
|---|---|---|
| **Orchestrator** | **MySQL 8** | Datos transaccionales (sesiones, outbox) con relaciones simples. Necesitamos ACID para el patrón outbox (escribir sesión + evento en la misma transacción atómica). MySQL es la opción más conocida y suficiente para este caso. |
| **GridLoad** | **Redis 7** | Carga de estación = clave-valor puro, lectura intensiva, latencia sub-ms crítica. Sin necesidad de esquema relacional. Redis es el estándar de facto para datos en memoria. |
| **Billing** | **PostgreSQL 16** | Datos financieros (facturas, deudas) con consultas analíticas potenciales. PostgreSQL tiene mejor soporte que MySQL para tipos numéricos precisos (`NUMERIC`), `JSONB`, y operaciones financieras complejas. |

**Cada MS es dueño de su propia DB. No se comparten esquemas ni instancias.** Esto es el patrón "Database-per-Service" de microservicios. Si Billing necesita saber algo del Orchestrator, lo recibe vía broker, **nunca lee directo de la DB del otro**.

---

## 7. Arquitectura hexagonal — cómo está organizado el código

### Una analogía simple

Imagina que tu lógica de negocio es un **castillo amurallado**. El castillo tiene **puertas** (interfaces) por las que entra y sale información. Lo de afuera del castillo (la DB, el cliente REST, el broker) son **adaptadores** que se conectan a esas puertas.

- El **castillo (dominio)** no sabe nada de Spring, ni de MySQL, ni de HTTP. Es Java puro.
- Los **adaptadores** sí saben de esas tecnologías, pero se conectan al castillo a través de las puertas estandarizadas.
- Si mañana cambiamos MySQL por PostgreSQL, **solo cambiamos el adaptador** — el castillo ni se entera.

### Estructura de carpetas (idéntica en los 3 microservicios)

```
src/main/java/com/voltnet/<service>/
├── domain/              ← EL CASTILLO. Java puro. Sin Spring, sin JPA.
│   ├── model/           ← Entidades (ChargeSession, User, Invoice)
│   ├── valueobject/     ← Valores inmutables (Kwh, StationId, UserId)
│   ├── event/           ← Eventos de dominio (ChargeSessionCompleted)
│   └── port/            ← LAS PUERTAS DEL CASTILLO
│       ├── in/          ← Casos de uso que el exterior invoca
│       └── out/         ← Lo que el dominio necesita pedir afuera
│
├── application/         ← CASOS DE USO. Orquesta el dominio.
│   └── service/         ← Implementaciones de los puertos `in`
│
└── infrastructure/      ← ADAPTADORES. Lo "técnico".
    ├── rest/            ← Controllers + DTOs (adaptador REST entrante)
    ├── persistence/     ← Entidades JPA, repositorios (adaptador DB)
    ├── client/          ← Clientes Feign (adaptador REST saliente)
    └── messaging/       ← Producers y consumers AMQP (adaptador broker)
```

### Regla de oro de las dependencias

`domain` no importa **nada** de `application` ni de `infrastructure`.
`application` puede importar de `domain`.
`infrastructure` puede importar de `application` y `domain`.

**Nunca al revés.** Las flechas siempre apuntan hacia el dominio. Esto se valida automáticamente con **ArchUnit** en CI (un test corre cada vez que pusheas a Git y falla si alguien rompió la regla).

### Por qué hexagonal nos conviene

- **Testabilidad:** las reglas R1, R2, R3 se prueban con JUnit puro, sin Spring, en milisegundos. Inyectamos puertos "falsos" (mocks) y verificamos comportamiento.
- **Cambio de tecnología:** si mañana migramos de MySQL a PostgreSQL, solo tocamos `infrastructure/persistence`. El dominio sigue igual.
- **Lectura del código:** un desarrollador nuevo abre el proyecto y entiende en 5 minutos qué es lógica de negocio y qué es plomería técnica, porque están en carpetas separadas.

---

## 8. Patrones GoF aplicados

La rúbrica exige **mínimo 3 patrones GoF**. Implementamos **4** para tener margen y poder responder con seguridad en la sustentación oral.

### 8.1 Strategy — Políticas de validación al iniciar carga

**Dónde vive:** `domain/policy/` en MS-ChargeOrchestrator.

**Qué resuelve:** Cuando se va a iniciar una carga, hay que validar **dos reglas independientes** (R1 capacidad de red, R2 solvencia). Podríamos meter ambas validaciones en un gran `if/else` dentro del caso de uso… pero eso es feo, difícil de testear, y violaría el principio Open/Closed de SOLID.

**Cómo lo resolvemos con Strategy:** cada regla es una **estrategia** independiente que implementa la misma interfaz `ChargeStartPolicy`. El caso de uso recibe la lista de políticas y las evalúa una por una.

```java
// domain/policy/ChargeStartPolicy.java
public interface ChargeStartPolicy {
    PolicyResult evaluate(ChargeStartContext ctx);
}

// domain/policy/GridCapacityPolicy.java
public class GridCapacityPolicy implements ChargeStartPolicy {
    private final GridLoadPort gridLoad;

    public PolicyResult evaluate(ChargeStartContext ctx) {
        double loadKw = gridLoad.currentLoadKw(ctx.stationId());
        return loadKw > 100.0
            ? PolicyResult.deny("GRID_OVERLOADED", "Carga actual: " + loadKw + " kW")
            : PolicyResult.allow();
    }
}

// domain/policy/UserSolvencyPolicy.java
public class UserSolvencyPolicy implements ChargeStartPolicy {
    private final UserRepository userRepo;

    public PolicyResult evaluate(ChargeStartContext ctx) {
        User user = userRepo.findById(ctx.userId()).orElseThrow();
        if (!user.hasActivePaymentMethod())
            return PolicyResult.deny("NO_PAYMENT_METHOD", "Sin método de pago activo");
        if (user.overdueDays() > 30)
            return PolicyResult.deny("OVERDUE_DEBT", user.overdueDays() + " días vencidos");
        return PolicyResult.allow();
    }
}

// application/service/StartChargeSessionUseCase.java
public ChargeSession execute(StartChargeCommand cmd) {
    var ctx = new ChargeStartContext(cmd.userId(), cmd.stationId());
    for (ChargeStartPolicy policy : policies) {
        PolicyResult result = policy.evaluate(ctx);
        if (result.isDeny()) throw new ChargeStartDeniedException(result);
    }
    return sessionFactory.create(ctx);
}
```

**Beneficios concretos:**
- Cada política se testea aisladamente con un test unitario de 10 líneas.
- Si el profesor preguntara "¿y si mañana una regulación obliga a validar X?" — respuesta tranquila: "agregamos una nueva clase que implemente `ChargeStartPolicy` y la inyectamos; el caso de uso no se toca". Esto es exactamente lo que predica el principio Open/Closed.

### 8.2 Adapter — Cliente Feign de GridLoad

**Dónde vive:** `infrastructure/client/GridLoadFeignAdapter.java`.

**Qué resuelve:** el dominio necesita "preguntar la carga actual de una estación". No queremos que el dominio sepa que esa información viene por HTTP, ni que se usa Feign, ni que la respuesta es JSON. Si mañana cambiamos a gRPC, el dominio no debería enterarse.

**Cómo lo resolvemos con Adapter:** definimos un **puerto** (interfaz) en el dominio que expresa la necesidad de forma abstracta. La implementación concreta vive en infrastructure y es la que sabe de Feign.

```java
// domain/port/out/GridLoadPort.java   ← el dominio solo ve esto
public interface GridLoadPort {
    double currentLoadKw(StationId stationId);
}

// infrastructure/client/GridLoadFeignClient.java   ← Feign específico
@FeignClient(name = "grid-load", url = "${grid.load.url}")
public interface GridLoadFeignClient {
    @GetMapping("/grid/load")
    GridLoadResponseDto getLoad(@RequestParam String stationId);
}

// infrastructure/client/GridLoadFeignAdapter.java   ← el ADAPTADOR
@Component
public class GridLoadFeignAdapter implements GridLoadPort {
    private final GridLoadFeignClient feignClient;

    public double currentLoadKw(StationId stationId) {
        GridLoadResponseDto dto = feignClient.getLoad(stationId.value());
        return dto.currentLoadKw();
    }
}
```

**Beneficios concretos:**
- El dominio no conoce Feign. Para tests, inyectamos un `GridLoadPort` falso que retorna lo que queramos.
- Si cambiamos a otro cliente HTTP (WebClient, OkHttp), solo se reescribe el adaptador. El dominio queda intacto.

### 8.3 Factory — Creación de ChargeSession

**Dónde vive:** `domain/model/ChargeSessionFactory.java`.

**Qué resuelve:** crear una `ChargeSession` tiene varias invariantes que cumplir: generar un UUID único, registrar el timestamp con un `Clock` (para que los tests puedan controlar el tiempo), poner el status inicial en `ACTIVE`, validar que los IDs no sean nulos, etc. Si esto lo dispersamos en cada caso de uso que cree sesiones, vamos a tener bugs.

**Cómo lo resolvemos con Factory:** centralizamos toda la creación en una fábrica.

```java
// domain/model/ChargeSession.java
public class ChargeSession {
    private final SessionId id;
    private final UserId userId;
    private final StationId stationId;
    private final Instant startedAt;
    private SessionStatus status;
    private Kwh consumedKwh;

    // Constructor package-private — solo la fábrica puede instanciar
    ChargeSession(SessionId id, UserId userId, StationId stationId,
                  Instant startedAt, SessionStatus status) {
        this.id = id;
        this.userId = userId;
        this.stationId = stationId;
        this.startedAt = startedAt;
        this.status = status;
    }
    // ... métodos de comportamiento (stop(), calculateKwh(), etc.)
}

// domain/model/ChargeSessionFactory.java
public class ChargeSessionFactory {
    private final Clock clock;          // inyectable, para tests
    private final IdGenerator ids;      // genera UUIDs

    public ChargeSession create(UserId userId, StationId stationId) {
        Objects.requireNonNull(userId, "userId requerido");
        Objects.requireNonNull(stationId, "stationId requerido");
        return new ChargeSession(
            SessionId.of(ids.newUuid()),
            userId,
            stationId,
            clock.instant(),
            SessionStatus.ACTIVE
        );
    }
}
```

**Beneficios concretos:**
- Imposible crear una `ChargeSession` con status inválido o sin ID, porque el constructor no es público.
- Si mañana agregamos un campo obligatorio (ej. `tariffPlan`), lo añadimos solo en la fábrica. Los casos de uso no cambian de firma.
- En tests, inyectamos un `Clock.fixed(...)` y obtenemos timestamps deterministas.

### 8.4 Observer / Publisher-Subscriber — Eventos de dominio

**Dónde vive:** `domain/event/`, `application/event/`, e `infrastructure/messaging/`.

**Qué resuelve:** cuando se cierra una sesión de carga, varias cosas tienen que pasar: persistirla en MySQL, escribir a la outbox, eventualmente publicar el evento al broker para que Billing factore. **No queremos** que el caso de uso `StopChargeSessionUseCase` sepa de la existencia de Billing, ni de RabbitMQ. Eso sería acoplamiento.

**Cómo lo resolvemos con Observer:** el dominio levanta un **evento de dominio** cuando algo importante pasa. El caso de uso lo publica a un `DomainEventPublisher` (puerto). El adaptador en `infrastructure/messaging/` es el "observador" que escucha esos eventos y los manda al broker.

```java
// domain/event/ChargeSessionCompleted.java
public record ChargeSessionCompleted(
    SessionId sessionId,
    UserId userId,
    StationId stationId,
    Kwh consumedKwh,
    Instant completedAt
) implements DomainEvent {}

// domain/port/out/DomainEventPublisher.java
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}

// application/service/StopChargeSessionUseCase.java
@Transactional
public void execute(StopChargeCommand cmd) {
    ChargeSession session = repo.findById(cmd.sessionId()).orElseThrow();
    session.stop(cmd.finalKwh(), clock.instant());
    repo.save(session);

    eventPublisher.publish(new ChargeSessionCompleted(
        session.id(), session.userId(), session.stationId(),
        session.consumedKwh(), session.completedAt()
    ));
}

// infrastructure/messaging/OutboxEventPublisher.java
@Component
public class OutboxEventPublisher implements DomainEventPublisher {
    private final OutboxRepository outboxRepo;

    public void publish(DomainEvent event) {
        // Persistimos el evento en la outbox EN LA MISMA TRANSACCIÓN que el caso de uso
        outboxRepo.save(OutboxRecord.from(event));
    }
}

// infrastructure/messaging/OutboxPublishWorker.java
@Scheduled(fixedDelay = 1000)
public void publishPending() {
    List<OutboxRecord> pending = outboxRepo.findPending();
    for (OutboxRecord rec : pending) {
        rabbitTemplate.convertAndSend("voltnet.events", rec.routingKey(), rec.payload());
        outboxRepo.markAsPublished(rec.id());
    }
}
```

**Beneficios concretos:**
- El caso de uso (`StopChargeSessionUseCase`) **no conoce a RabbitMQ ni a Billing**. Solo publica un evento de dominio.
- Mañana podemos agregar 3 nuevos observadores (ej. un sistema de analytics, un servicio de notificaciones push) sin tocar el caso de uso.
- Como la publicación al outbox vive en la misma transacción del caso de uso, garantizamos atomicidad: si la sesión se commitea, el evento queda en outbox; si no, ninguna de las dos cosas pasa.

### Patrón adicional mencionable (no GoF estricto)

**Circuit Breaker (Resilience4j)** — patrón de resiliencia para protegerse de fallos de servicios externos. No es GoF, pero es de los patrones más conocidos en arquitectura de microservicios. Vale la pena nombrarlo en la sustentación oral como muestra de criterio técnico.

---

## 9. Principios SOLID — dónde y cómo

Cada principio aplicado con un ejemplo concreto del código de VoltNet:

### S — Single Responsibility (Responsabilidad Única)

Cada clase tiene una sola razón para cambiar.

- `StartChargeSessionUseCase` se ocupa **solo** de iniciar sesiones. El cierre vive en `StopChargeSessionUseCase`. La consulta en `GetChargeSessionUseCase`. Si la regla de inicio cambia, solo se toca una clase.
- `GridCapacityPolicy` valida **solo** R1. `UserSolvencyPolicy` valida **solo** R2. No hay una clase "ValidadorDeTodo".

### O — Open/Closed (Abierto a extensión, cerrado a modificación)

Las clases existentes no deben modificarse para añadir comportamiento nuevo; debe poderse extender.

- Las políticas de inicio son `ChargeStartPolicy`. El caso de uso `StartChargeSessionUseCase` itera sobre una lista de políticas inyectadas por Spring. Si se quisiera añadir una validación adicional, se crearía una nueva clase y se registraría como Bean — **el caso de uso no se modifica**.
- Lo mismo aplica al `DomainEventPublisher`: agregar un nuevo observador no requiere tocar al productor.

### L — Liskov (Sustitución)

Cualquier implementación de una interfaz debe poder usarse en lugar de otra sin romper el sistema.

- `GridLoadPort` puede ser implementado por `GridLoadFeignAdapter` (producción) o por `InMemoryGridLoadAdapter` (tests). Ambos cumplen el contrato: `currentLoadKw(StationId) → double`. El caso de uso no nota la diferencia.
- Lo mismo con `ChargeSessionRepository` — implementación JPA en producción, implementación en memoria en tests.

### I — Interface Segregation (Segregación de Interfaces)

Los clientes no deben depender de métodos que no usan. Mejor varias interfaces pequeñas que una grande.

- `GridLoadPort` tiene **un solo método**: `currentLoadKw(...)`. No hay un mega-`ExternalSystemsPort` con 20 métodos del que el dominio solo use uno.
- Los puertos del dominio se nombran por intención (`ChargeSessionRepository`, `UserRepository`, `DomainEventPublisher`) y cada uno expone solo lo necesario.

### D — Dependency Inversion (Inversión de Dependencias)

Los módulos de alto nivel no deben depender de los de bajo nivel. Ambos deben depender de abstracciones.

- El **dominio** define interfaces (`GridLoadPort`, `ChargeSessionRepository`, `DomainEventPublisher`).
- La **infraestructura** implementa esas interfaces (`GridLoadFeignAdapter`, `JpaChargeSessionRepository`, `OutboxEventPublisher`).
- Spring inyecta las implementaciones concretas en tiempo de arranque vía constructor injection. **El dominio nunca importa una clase de infraestructura.**

**Argumento oral fuerte:**

> *"La inversión de dependencias es la columna vertebral de toda nuestra arquitectura hexagonal. El dominio no sabe que existe Spring, ni MySQL, ni Feign. Es código Java puro que define interfaces; la infraestructura las implementa. Esta inversión es lo que nos permite testear las reglas de negocio sin levantar absolutamente nada."*

---

## 10. Observabilidad

**Observabilidad** es la capacidad de **ver hacia adentro** del sistema para entender qué está pasando, especialmente cuando algo falla. Se construye con tres pilares:

| Pilar | Qué es | Herramienta |
|---|---|---|
| **Métricas** | Números que se acumulan en el tiempo (peticiones por segundo, latencia, errores) | Prometheus + Micrometer |
| **Trazas** | Seguimiento del recorrido de una petición individual a través de los servicios | Jaeger + OpenTelemetry |
| **Visualización** | Gráficas y paneles para ver todo en tiempo real | Grafana |

### Cómo funciona cada pieza

- **Prometheus** es el "recolector": cada 15 segundos visita a cada microservicio en su endpoint `/actuator/prometheus` y se trae los números actuales (RPS, latencia p95, errores 5xx, uso de memoria, etc.). Los guarda en su propia base de datos de series temporales.
- **Grafana** es el "panel de control": toma los datos de Prometheus y los pinta en gráficas. Configuramos dashboards con un row por cada MS: un panel de RPS, otro de latencia, otro de errores.
- **Jaeger** es el "rastreador": cuando una petición entra por Nginx → Orquestador → llama a GridLoad → consulta MySQL → responde, Jaeger registra cada salto con su duración. Si algo va lento, sabemos exactamente en qué punto.

### Cómo se instrumenta el código

- **Métricas:** Spring Boot expone `/actuator/prometheus` automáticamente cuando incluimos la librería **Micrometer**. No hay que escribir código.
- **Trazas:** el **OpenTelemetry Java Agent** se adjunta al proceso Java como `-javaagent:opentelemetry.jar` y envía traces a Jaeger sin código adicional. Es magia, pero magia bien documentada.

**Argumento oral:**

> *"La observabilidad no es un afterthought, está integrada desde el arranque. Cualquier petición es trazeable de extremo a extremo en Jaeger, y la salud de cada servicio se ve en Grafana en tiempo real."*

---

## 11. Atributos de calidad y trade-offs

Esta sección sigue la **clasificación estándar de atributos de calidad** que vimos en clase (Performance/Eficiencia, Fiabilidad, Mantenibilidad, etc.). Para VoltNet identificamos como **críticos** estos tres:

### 11.1 Rendimiento / Eficiencia

**Definición:** velocidad de respuesta, capacidad de carga, utilización eficiente de recursos.

**Qué garantizamos:** `POST /sessions/start` responde con latencia p95 < 300 ms en condiciones normales.

**Cómo lo logramos:**
- **Redis** en GridLoad para respuesta sub-milisegundo del lado del servidor (R1).
- **Proyección local de solvencia** en Orchestrator (MySQL local en lugar de llamada REST a Billing) para R2.
- **Feign con timeout agresivo** (500 ms) y **Circuit Breaker** para fallar rápido si GridLoad degrada (en lugar de acumular peticiones colgadas).

**Qué sacrificamos:** **frescura absoluta del estado de deuda**. La tabla local de solvencia en Orchestrator puede tener segundos de atraso respecto a lo que Billing ya sabe. Si un usuario paga AHORA MISMO y trata de cargar 5 segundos después, podría aún ser rechazado.

**Por qué vale la pena el sacrificio:** el escenario es marginal (basta con reintentar en 30 segundos), y la alternativa (consultar a Billing por REST en cada inicio) duplicaría la latencia e introduciría un punto de fallo adicional.

### 11.2 Fiabilidad

**Definición:** madurez, disponibilidad y tolerancia a fallos.

**Qué garantizamos:** un usuario nunca queda "atrapado" en la estación. `POST /sessions/{id}/stop` responde con éxito aunque Billing y/o RabbitMQ estén caídos.

**Cómo lo logramos:**
- **Patrón Transactional Outbox**: el evento se persiste en la misma transacción que cierra la sesión. Un worker en background lo publica al broker después; si falla, reintenta sin afectar la respuesta al usuario.
- **Aislamiento de fallos**: Orchestrator nunca depende de la disponibilidad de Billing para responder al usuario final.
- **Idempotencia exactly-once-effective**: cada sesión genera exactamente una factura, incluso si el broker reentrega el mensaje, gracias al constraint `UNIQUE(session_id)` en `invoices`.
- **Dead Letter Queues**: mensajes que fallan repetidamente se aíslan en una cola separada en lugar de perderse o reintentar para siempre.

**Qué sacrificamos:** **consistencia inmediata** entre la sesión cerrada y la factura emitida. Hay una ventana (segundos, a veces minutos) donde la sesión está cerrada pero la factura todavía no se ha creado. A esto se le llama **consistencia eventual**.

**Por qué vale la pena el sacrificio:** el caso de estudio lo exige textualmente: *"la sesión se cierra exitosamente aunque el sistema de facturación esté fuera de línea"*. El daño de bloquear a un conductor en la calle es enorme; el daño de facturar con segundos de retraso es invisible al usuario.

### 11.3 Mantenibilidad

**Definición:** facilidad para modificar, actualizar o corregir el sistema.

**Qué garantizamos:** un desarrollador nuevo puede entender la lógica de negocio en horas, no días. Cambios de tecnología (DB, broker, framework) tocan partes localizadas del código, nunca todo el sistema.

**Cómo lo logramos:**
- **Arquitectura hexagonal** con separación estricta dominio / aplicación / infraestructura.
- **Principios SOLID** aplicados en código, no solo en teoría.
- **Patrones GoF (Strategy, Adapter, Factory, Observer)** que hacen el código predecible.
- **Tests del dominio** que corren sin Spring ni DB, en milisegundos.
- **ArchUnit** en CI valida automáticamente que las reglas de dependencias no se rompan.

**Qué sacrificamos:** **simplicidad inicial**. Un CRUD plano sería más rápido de escribir; nuestra arquitectura tiene más carpetas, más interfaces, más clases.

**Por qué vale la pena el sacrificio:** el código se escribe una vez pero se lee mil veces. Una arquitectura clara se paga sola en el primer cambio importante.

### Tabla resumen de los principales trade-offs

| Decisión | Ganamos | Sacrificamos |
|---|---|---|
| Microservicios sobre monolito | Escalado y disponibilidad independientes por servicio | Complejidad operacional (más DBs, broker, observabilidad) |
| Comunicación asíncrona Orch ↔ Billing | Desacoplamiento temporal: Billing puede estar caído sin afectar al usuario | Consistencia eventual |
| Transactional Outbox | Entrega garantizada de eventos sin transacciones distribuidas | Una tabla extra + worker en background |
| Proyección local de solvencia | Latencia sub-ms en R2, sin punto de fallo adicional | Stale reads en ventanas muy recientes (segundos) |
| Redis para GridLoad | Latencia sub-ms en el camino crítico de R1 | No hay historial persistente de carga (no es requerido) |
| RabbitMQ sobre Kafka | Simplicidad operacional, UI de admin | No event sourcing ni replays masivos (no se necesitan) |
| Hexagonal estricto | Testabilidad y mantenibilidad altas | Curva de aprendizaje, más carpetas que un CRUD |
| "En duda, rechazar" si GridLoad cae | Seguridad de la red eléctrica preservada | Falsos negativos durante fallos de GridLoad |

**Trade-off central que define la arquitectura:**

> Sacrificamos **consistencia inmediata** del estado de facturación a cambio de **fiabilidad** del flujo de cierre de carga. Esto está alineado directamente con la regla R3 del enunciado, que exige textualmente que la sesión cierre aunque facturación esté caída.

---

## 12. Guion de sustentación oral

Esta sección es la que vas a leer 3 veces antes de la sustentación. Son **6 argumentos centrales** que cualquier miembro del equipo debe poder defender.

### Argumento 1 — "¿Por qué microservicios y no un monolito?"

> *"Porque las tres responsabilidades del sistema tienen perfiles muy distintos. GridLoad debe responder en milisegundos a un volumen alto. Billing tiene cargas pesadas pero diferibles. Orchestrator es transaccional clásico. Si los pusiéramos en un monolito, una facturación pesada haría picos de CPU que ralentizarían un inicio de carga. La separación nos permite escalar cada uno independientemente y darles SLAs distintos."*

### Argumento 2 — "¿Por qué hexagonal?"

> *"Porque el corazón de este sistema son tres reglas de negocio que deben ser testables sin levantar Spring, sin DB, sin broker. Hexagonal nos da eso: el dominio es Java puro, los adaptadores son intercambiables. Si mañana cambiamos MySQL por PostgreSQL en Orchestrator, no se toca una sola línea de dominio. Eso es resiliencia al cambio tecnológico, no solo a fallos."*

### Argumento 3 — "¿Por qué Billing es totalmente asíncrono?"

> *"Porque el caso de estudio dice que la sesión se cierra exitosamente aunque facturación esté fuera de línea. Si Billing expusiera REST y el Orchestrator dependiera de eso para cerrar, esa regla se rompería. Al ir 100% por broker con outbox, garantizamos que el Orchestrator nunca depende de la disponibilidad de Billing. Eso es desacoplamiento temporal."*

### Argumento 4 — "¿Por qué Redis en GridLoad?"

> *"Porque GridLoad está en el camino crítico de cada intento de inicio. Una lectura de SQL con su round-trip al disco no nos sirve si queremos p95 menor a 300 milisegundos en `/sessions/start`. Redis nos da lectura sub-milisegundo. No hay esquema relacional aquí — es clave-valor puro. Usar SQL sería matar moscas a cañonazos."*

### Argumento 5 — "¿Cómo garantizan que no facturen dos veces?"

> *"Combinamos tres cosas: primero, Transactional Outbox en el productor — el evento se publica solo si la sesión se commiteó. Segundo, cada evento lleva un eventId UUID único. Tercero, en Billing la tabla invoices tiene UNIQUE sobre session_id. Si llega un duplicado, el INSERT falla por la restricción y descartamos el mensaje. Eso es exactly-once-effective sin necesidad de transacciones distribuidas."*

### Argumento 6 — "¿Qué pasa si GridLoad está caído al iniciar una carga?"

> *"Tenemos Circuit Breaker con Resilience4j y timeout de 500ms. Si GridLoad falla repetidamente, el circuito se abre por 30 segundos y devolvemos 503 inmediatamente sin acumular peticiones colgadas. La política es 'en duda, rechazar': preferimos negar una carga que dejar pasar una sobrecarga real de la red. Para esta regla específica, seguridad sobre disponibilidad."*

---

## 13. Glosario

### Términos del dominio del problema
- **kWh** — kiloWatt-hora. Unidad de energía consumida. Si un cargador entrega 22 kW durante 30 minutos, el consumo es 11 kWh.
- **Estación de carga** — ubicación física que puede tener varios cargadores. Identificada por `stationId`.
- **Sesión de carga (ChargeSession)** — el periodo durante el cual un auto está enchufado y consumiendo energía. Tiene un inicio, un fin y un total de kWh consumidos.

### Arquitectura y patrones
- **Hexagonal (Ports & Adapters)** — estilo arquitectónico donde el dominio define interfaces ("puertos") y la infraestructura las implementa ("adaptadores"). El dominio nunca depende de detalles técnicos.
- **Puerto (Port)** — interface que el dominio declara para expresar una necesidad ("necesito conocer la carga de la estación", "necesito guardar una sesión").
- **Adaptador (Adapter)** — clase que implementa un puerto usando una tecnología específica (Feign, JPA, RabbitMQ).
- **Caso de uso (Use Case)** — clase que orquesta una operación de negocio. Recibe comandos, valida con el dominio, persiste, publica eventos.
- **Entidad de dominio** — objeto con identidad propia (`ChargeSession`, `User`, `Invoice`) que vive a lo largo del tiempo.
- **Value Object** — objeto inmutable definido solo por su valor (`Kwh`, `StationId`, `UserId`). No tiene identidad.
- **Evento de dominio** — algo importante que pasó en el dominio (`ChargeSessionCompleted`, `UserDebtUpdated`). Se publica para que otros reaccionen.
- **CQRS lite** — separar el modelo de escritura (Billing emite eventos) del modelo de lectura (Orchestrator mantiene una tabla local). Hacemos una versión simple de esto.

### Comunicación
- **REST** — protocolo de comunicación HTTP donde cada petición es síncrona: cliente pide, servidor responde, cliente espera. Es lo que hace tu navegador al cargar una página.
- **Sincrónico (sync)** — comunicación donde el llamador espera la respuesta antes de continuar. Si el otro lado tarda, tú tardas.
- **Asíncrono (async)** — comunicación donde el llamador envía un mensaje y sigue su vida. El receptor lo procesa cuando puede.
- **Broker (de mensajes)** — intermediario que recibe mensajes de productores y los entrega a consumidores. Permite comunicación asíncrona y desacoplada. En este proyecto: **RabbitMQ**.
- **AMQP (Advanced Message Queuing Protocol)** — el "lenguaje" que habla RabbitMQ.
- **Producer** — el que publica mensajes al broker.
- **Consumer** — el que recibe mensajes del broker.
- **Exchange** — componente de RabbitMQ que enruta mensajes a las colas correctas según su `routing key`.
- **Cola (Queue)** — buzón donde los mensajes esperan ser consumidos.
- **DLQ (Dead Letter Queue)** — cola especial para mensajes que han fallado al procesarse N veces. Aísla mensajes problemáticos sin perderlos.
- **at-least-once delivery** — garantía de entrega del broker: cada mensaje se entrega **al menos** una vez. Puede entregarse más de una vez si hay reintentos.
- **exactly-once-effective** — el efecto neto es como si el mensaje se hubiera entregado una sola vez, gracias a idempotencia en el consumidor (aunque el broker entregue varias).
- **Idempotencia** — propiedad de una operación que se puede repetir N veces con el mismo efecto que ejecutarla una sola vez. Crítico para consumidores de broker.
- **Outbox (patrón Transactional Outbox)** — tabla en la DB del productor donde se guardan los eventos pendientes de publicar. Un worker los lee y los manda al broker. Garantiza "si la transacción se commitea, el evento se publicará".

### Tecnologías concretas
- **Java 25 LTS** — versión actual de soporte largo de Java (Long-Term Support).
- **Spring Boot 3.4** — framework de Java para construir aplicaciones de forma rápida y opinionada. Provee servidor web, inyección de dependencias, integración con DBs y brokers.
- **Maven** — herramienta que gestiona dependencias y compila proyectos Java.
- **JDBC (Java Database Connectivity)** — la API estándar de Java para conectarse a bases de datos SQL.
- **JPA (Java Persistence API)** — capa de abstracción sobre JDBC que permite mapear objetos Java a tablas SQL. Implementada por Hibernate.
- **Feign** — librería de Spring Cloud para llamar servicios REST escribiendo solo una interfaz Java con anotaciones.
- **Spring AMQP** — librería de Spring para hablar con RabbitMQ de forma declarativa.
- **Resilience4j** — librería que provee patrones de resiliencia: Circuit Breaker, Retry, Rate Limiter, etc.
- **Circuit Breaker (cortacircuitos)** — patrón de resiliencia. Si un servicio externo falla N veces seguidas, el circuit breaker "se abre" y rechaza llamadas inmediatamente durante un periodo, evitando saturación. Después se "cierra" si vuelve a funcionar.
- **Redis** — base de datos NoSQL clave-valor **en memoria RAM**. Latencia sub-milisegundo. Ideal para caches y datos en tiempo real.
- **MySQL** — base de datos SQL relacional, ACID, ampliamente usada.
- **PostgreSQL** — base de datos SQL relacional, ACID, con soporte avanzado para tipos numéricos precisos, JSONB y queries analíticas.
- **ACID** — cuatro garantías que da una base de datos SQL en transacciones:
  - **A**tomicity (Atomicidad): todo o nada.
  - **C**onsistency (Consistencia): la DB siempre queda en estado válido.
  - **I**solation (Aislamiento): transacciones simultáneas no se pisan.
  - **D**urability (Durabilidad): commits sobreviven aunque se caiga la máquina.
- **NoSQL** — bases de datos que no siguen el modelo relacional. Redis es una.
- **Nginx** — servidor web ligero usado como API Gateway: recibe peticiones del exterior y las redirige al microservicio correspondiente según la URL.
- **Docker** — plataforma para empaquetar aplicaciones en "contenedores" portables.
- **Docker Compose** — herramienta para levantar varios contenedores conectados con un solo comando.
- **React** — librería de JavaScript (de Facebook) para construir interfaces web mediante componentes.
- **Vite** — herramienta moderna que arranca el servidor de desarrollo de React en menos de un segundo. Reemplaza a Webpack.

### Observabilidad
- **Observabilidad** — capacidad de "ver hacia adentro" del sistema para entender su comportamiento. Tres pilares: métricas, trazas, logs.
- **Métricas** — números agregados en el tiempo (peticiones por segundo, latencia, errores).
- **Traza (Trace)** — el recorrido completo de una petición a través de varios servicios.
- **Span** — un tramo de una traza (ej. "tiempo gastado en MySQL").
- **OpenTelemetry (OTel)** — estándar abierto para instrumentar aplicaciones con métricas, trazas y logs.
- **OTLP (OpenTelemetry Protocol)** — protocolo que usa OpenTelemetry para enviar datos a backends como Jaeger.
- **Prometheus** — sistema que recolecta métricas periódicamente de los servicios y las guarda en una base de datos de series temporales.
- **Grafana** — sistema de dashboards que visualiza métricas (de Prometheus u otras fuentes) en gráficas.
- **Jaeger** — sistema que recibe trazas distribuidas y permite visualizar cómo viaja una petición entre microservicios.
- **Micrometer** — librería Java que abstrae la exposición de métricas. Spring Boot la usa para exponer `/actuator/prometheus`.
- **p50 / p95 / p99** — percentiles de latencia. Si p95 = 300ms, significa que 95% de las peticiones responden en menos de 300ms.

### HTTP / API
- **422 Unprocessable Entity** — código HTTP que significa "entendí tu petición, pero una regla de negocio impide procesarla". Usado para violaciones de reglas de dominio.
- **400 Bad Request** — la petición está mal formada (ej. JSON inválido).
- **404 Not Found** — el recurso solicitado no existe.
- **503 Service Unavailable** — un servicio del que dependo está caído.
- **Swagger / OpenAPI** — estándar para documentar APIs REST. Spring Boot lo expone automáticamente con `springdoc-openapi`.

### Calidad de software
- **Atributos de calidad** — características no funcionales del sistema (rendimiento, fiabilidad, mantenibilidad, seguridad, escalabilidad, etc.).
- **Trade-off** — sacrificio aceptado: para ganar X, sacrificamos Y.
- **SLA (Service Level Agreement)** — promesa formal sobre el comportamiento de un servicio (ej. "99.9% de uptime").
- **Consistencia eventual** — el sistema converge al estado correcto, pero no instantáneamente. Lo opuesto a "consistencia fuerte".
- **Stale read** — lectura de un dato que estaba correcto pero ya no lo está (por consistencia eventual).

### Procesos de desarrollo
- **CI (Continuous Integration)** — práctica donde cada push a Git dispara la ejecución automática de tests. Sistemas: GitHub Actions, Jenkins.
- **ArchUnit** — librería Java para escribir tests que validan **reglas de arquitectura** (ej: "ninguna clase de `domain` puede importar de `infrastructure`"). Si alguien rompe la regla, el test falla en CI.
- **JUnit** — framework de tests unitarios para Java.
- **Mock** — objeto falso usado en tests, que simula el comportamiento de un componente real.

---

> **Última actualización:** Fase 1 — diseño documentado antes de empezar a codear.
