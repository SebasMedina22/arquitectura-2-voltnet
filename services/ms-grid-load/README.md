# MS-GridLoad

Microservicio síncrono que expone la carga eléctrica actual de cada estación. Es la fuente de verdad consultada por **MS-ChargeOrchestrator** vía Feign para validar la regla **R1** (no iniciar carga si la estación supera 100 kW).

## Stack

- Java 25, Spring Boot 3.5
- Spring Data Redis (Redis 7)
- springdoc-openapi (Swagger UI)
- Micrometer + Prometheus, OpenTelemetry Java Agent

## Arquitectura

Hexagonal estricta:

- `domain/` — Java puro. `Kw`, `StationId`, `StationLoad`, `StationLoadPort`.
- `application/` — casos de uso (`GetStationLoadService`, `SetStationLoadService`).
- `infrastructure/` — REST controller, adaptador Redis (Adapter GoF), Swagger, seed.

### Reglas de negocio en el dominio

1. **RG-1** — `Kw` rechaza negativos, `NaN`, `Infinity`.
2. **RG-2** — `StationId` exige formato `STN-XXX`.
3. **RG-3** — `StationLoad.isOverloaded()` true cuando `kw > 100`.

## Endpoints

| Método | Path | Descripción |
|---|---|---|
| GET | `/grid/load?stationId=STN-001` | Carga actual |
| POST | `/grid/load` | Setear carga (admin/simulación) |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Métricas |
| GET | `/swagger-ui.html` | Swagger UI |

## Cómo correr local

```powershell
# 1) Redis
docker run -d --name redis-gridload -p 6379:6379 redis:7-alpine

# 2) Build + run
mvn clean package
java -jar target\ms-grid-load-0.1.0.jar
```

Puerto: **8081**. Swagger: http://localhost:8081/swagger-ui.html

## Datos seed

Al arrancar con `gridload.seed.enabled=true` se cargan tres estaciones:

| stationId | kW iniciales |
|---|---|
| STN-001 | 45.3 |
| STN-002 | 92.8 |
| STN-003 | 105.0 (sobrecargada) |

## Tests

```powershell
mvn test
```

Tests del dominio y de aplicación corren **sin Redis** (adapter en memoria).
