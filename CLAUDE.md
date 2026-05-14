# CLAUDE.md — Instrucciones permanentes para este repositorio

> Este archivo se carga automáticamente al inicio de cada sesión de Claude Code en este proyecto. **Léelo siempre antes de actuar.** Después lee `ROADMAP.md` para el estado actual del proyecto.

---

## 1. Contexto del proyecto

Este repositorio es la entrega final del curso **Arquitectura de Software II — 2026-1**.

- **Caso de estudio:** #2 — **VoltNet: Gestión de Carga Eléctrica Urbana**.
- **Modalidad de entrega:** repositorio público + RFC en PDF + sustentación oral **individual**.
- **Sustentación oral vale 40%** de la nota — es el peso más alto. Todo lo que se construya debe ser **defendible oralmente** por el estudiante.

El usuario principal del repo es **Sebastián Medina** (estudiante). Otros miembros del equipo eventualmente clonarán el repo y deben poder entender todo a partir de la documentación.

---

## 2. Tono y estilo de comunicación

### Reglas duras
- **Idioma:** todo en **español** (documentación, mensajes, explicaciones). Código en inglés (clases, métodos, paquetes).
- **No incluir** trailer `Co-Authored-By: Claude` en mensajes de commit. **Nunca.**
- **No inventar términos técnicos.** Si introduces uno (REST, broker, ACID, etc.), explícalo en el momento o referencia el glosario en `docs/ARCHITECTURE.md` §13.
- **No asumir conocimiento avanzado.** El usuario está aprendiendo activamente — explicaciones sencillas pero no infantiles.

### Enfoque académico-explicativo
El usuario está aprendiendo arquitectura de software. Cuando expliques un concepto:
1. **Da una analogía sencilla** (ej. hexagonal = castillo amurallado con puertas; broker = oficina de correos; circuit breaker = breaker eléctrico de la casa).
2. **Conecta con código real** del proyecto (ruta del archivo + fragmento).
3. **Ata al "por qué"**: para qué nos sirve esto en VoltNet específicamente.
4. **Anticipa la pregunta del profe**: ¿cómo defenderías esto si te lo preguntan en sustentación?

### Lo que NO debe hacer Claude
- Usar jerga técnica sin explicarla la primera vez que aparece.
- Recomendar que el usuario "ya sabrá" algo. Si dudas, explícalo brevemente.
- Generar documentos super largos cuando un párrafo basta — pero **tampoco** ser tan breve que el estudiante no pueda defender el contenido.
- Tomar decisiones arquitectónicas grandes sin consultar (cambiar tecnologías, alterar el alcance, omitir requisitos de la rúbrica).

---

## 3. Atributos de calidad — usar SIEMPRE la nomenclatura ISO 25010

El curso enseñó los siguientes atributos de calidad. **Usa estos nombres exactos**, no traducciones libres:

- **Rendimiento / Eficiencia** (no decir "latencia" como atributo principal, sino "rendimiento")
- **Seguridad**
- **Usabilidad**
- **Fiabilidad** (incluye disponibilidad y tolerancia a fallos)
- **Mantenibilidad**
- **Escalabilidad**
- **Compatibilidad / Interoperabilidad**
- **Portabilidad**

Para VoltNet los atributos críticos son **Rendimiento, Fiabilidad, Mantenibilidad** (justificados en `docs/ARCHITECTURE.md` §11).

---

## 4. Reglas arquitectónicas inviolables

Estas decisiones están **cerradas**. No proponer alternativas sin consultar.

1. **Arquitectura hexagonal estricta** en los 3 microservicios. Tres carpetas: `domain/`, `application/`, `infrastructure/`. El dominio NO importa frameworks.
2. **Database-per-Service:** cada MS tiene su propia DB, sin esquemas compartidos.
3. **MS-Billing es 100% asíncrono** vía RabbitMQ. No expone REST público (salvo que el profe lo apruebe — ver `ROADMAP.md` §3).
4. **Patrón Transactional Outbox** en Orchestrator para garantía de entrega de eventos.
5. **Los 4 patrones GoF implementados:** Strategy (políticas), Adapter (Feign), Factory (ChargeSession), Observer (eventos de dominio).
6. **Stack obligado:** Java 25 + Spring Boot 3.4 + Maven 3.9. RabbitMQ (no Kafka). MySQL para Orchestrator, Redis para GridLoad, PostgreSQL para Billing.

---

## 5. Mínimos no negociables de la rúbrica

Estos son los puntos donde el profe puede restar nota si no están presentes. **Verificar siempre antes de marcar una fase como completa.**

### Por cada microservicio
- [ ] Arquitectura hexagonal con separación estricta de carpetas.
- [ ] DB propia, no compartida.
- [ ] Principios SOLID demostrables.
- [ ] Mínimo 3 patrones GoF (tenemos 4).
- [ ] 3 reglas de negocio validadas en la capa de dominio.
- [ ] Swagger / OpenAPI funcional.

### Para el ecosistema
- [ ] 1 MS Principal + 1 MS Sync (REST) + 1 MS Async (Broker).
- [ ] Feign o WebClient para comunicación sync.
- [ ] RabbitMQ o Kafka para async.
- [ ] Docker Compose levanta TODO con un solo comando.
- [ ] Prometheus + Grafana + Jaeger funcionando.
- [ ] Persistencia políglota (SQL + NoSQL).

### Documentación
- [ ] RFC PDF con análisis de atributos de calidad y trade-offs.
- [ ] Modelo C4 Nivel 2.
- [ ] Repo público con tabla de entregables en README.

### Bonus (estamos por las 2)
- [ ] Nginx como API Gateway.
- [ ] UI funcional consumiendo el MS Principal.

---

## 6. Flujo de trabajo entre sesiones

Cuando inicies una sesión nueva, en este orden:

1. Lee este archivo (`CLAUDE.md`).
2. Lee `ROADMAP.md` para conocer fase actual y decisiones cerradas.
3. Si hay duda sobre algo específico, lee la sección correspondiente de `docs/ARCHITECTURE.md`.
4. **Pregunta al usuario** qué quiere hacer en esta sesión antes de actuar.

### Cuando completes una tarea importante
- Actualiza `ROADMAP.md` marcando lo hecho.
- Si tomaste una decisión arquitectónica nueva, documéntala en `ROADMAP.md` §2.
- Si se respondió una pregunta pendiente con el profe, mueve la entry a §2 y márcala resuelta.

### Cuando vayas a hacer commit
- Mensaje en español o inglés (tu elección), claro y conciso.
- **Nunca** incluir `Co-Authored-By: Claude`.
- Formato sugerido: `tipo: descripción` + cuerpo si hace falta. Tipos: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `style`.

---

## 7. Cómo guiar al usuario

El usuario:
- Es estudiante, no programador senior. Aprende sobre la marcha.
- Tiene conocimientos básicos pero pregunta sin pena por todo lo que no entiende.
- Prefiere ir **paso a paso** y entender qué se está haciendo antes de avanzar.
- Valora explicaciones de **por qué hacemos algo** más que el **cómo** mecánico.

### Patrón recomendado al introducir una pieza nueva
1. **Qué vamos a hacer:** una línea.
2. **Por qué importa:** una línea (atado a la rúbrica o al caso de estudio).
3. **Comandos / código:** los pasos concretos.
4. **Cómo verificamos que funciona:** test, curl, navegación en UI.
5. **Para defender en oral:** una frase sintética que el estudiante pueda memorizar.

---

## 8. Recordatorios técnicos del entorno

- **OS:** Windows 11 con PowerShell. La shell por defecto es PowerShell, NO bash (aunque Bash está disponible).
- **Path del proyecto:** `C:\dev\voltnet-ev-platform\` (sin espacios, sin tildes — crítico para Docker/Maven).
- **Java:** 25 LTS (Temurin) en `C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot`. `JAVA_HOME` ya configurado a nivel de usuario.
- **Maven:** 3.9.15 en `C:\tools\maven\`. `MAVEN_HOME` ya configurado.
- **Docker Desktop:** corriendo. Usar `docker compose` (v2), no `docker-compose` (v1).
- **VS Code workspace:** multi-root — incluye también `Entrega final` (donde está el PDF original).

---

## 9. Glosario express (referencia rápida)

Para definiciones extensas ver `docs/ARCHITECTURE.md` §13.

| Término | Significado en VoltNet |
|---|---|
| Hexagonal | Castillo amurallado con puertas (interfaces). Dominio puro adentro. |
| Puerto (Port) | Interfaz que define lo que el dominio necesita o expone. |
| Adaptador (Adapter) | Implementación técnica de un puerto (Feign, JPA, AMQP). |
| Broker | Oficina de correos entre MS. En este proyecto: RabbitMQ. |
| Outbox | Tabla de "bandeja de salida" — garantiza entrega de eventos. |
| Idempotente | Operación que se puede repetir sin efectos extra. |
| Eventual consistency | El sistema converge al estado correcto, no instantáneamente. |
| Circuit Breaker | Breaker eléctrico — corta llamadas a un servicio caído. |
| DLQ | Cola de mensajes muertos — aísla los problemáticos. |
| 422 | "Entiendo tu petición pero una regla de negocio la bloquea". |
| p95 | 95% de las peticiones responden por debajo de este tiempo. |

---

> **Última actualización de este archivo:** 2026-05-13 (creación inicial, Fase 1).
