package com.voltnet.orchestrator.infrastructure.rest;

import com.voltnet.orchestrator.application.usecase.GetChargeSessionUseCase;
import com.voltnet.orchestrator.application.usecase.ListUserSessionsUseCase;
import com.voltnet.orchestrator.application.usecase.StartChargeSessionUseCase;
import com.voltnet.orchestrator.application.usecase.StopChargeSessionUseCase;
import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.Kwh;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.infrastructure.rest.dto.ChargeSessionResponse;
import com.voltnet.orchestrator.infrastructure.rest.dto.StartSessionRequest;
import com.voltnet.orchestrator.infrastructure.rest.dto.StopSessionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@Tag(name = "ChargeSessions", description = "Orquestador del ciclo de vida de sesiones de carga (R1, R2, R3)")
public class ChargeSessionController {

    private final StartChargeSessionUseCase start;
    private final StopChargeSessionUseCase stop;
    private final GetChargeSessionUseCase get;
    private final ListUserSessionsUseCase list;

    public ChargeSessionController(StartChargeSessionUseCase start,
                                   StopChargeSessionUseCase stop,
                                   GetChargeSessionUseCase get,
                                   ListUserSessionsUseCase list) {
        this.start = start;
        this.stop = stop;
        this.get = get;
        this.list = list;
    }

    @Operation(summary = "Inicia una sesion de carga aplicando R1 (capacidad) y R2 (solvencia)")
    @PostMapping("/start")
    @Transactional
    public ResponseEntity<ChargeSessionResponse> start(@Valid @RequestBody StartSessionRequest req) {
        ChargeSession s = start.execute(new UserId(req.userId()), new StationId(req.stationId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ChargeSessionResponse.from(s));
    }

    @Operation(summary = "Cierra una sesion y publica el evento al outbox (R3, cierre resiliente)")
    @PostMapping("/{id}/stop")
    @Transactional
    public ResponseEntity<ChargeSessionResponse> stop(@PathVariable("id") String id,
                                                      @Valid @RequestBody StopSessionRequest req) {
        ChargeSession s = stop.execute(new SessionId(id), new Kwh(req.kwhConsumed()));
        return ResponseEntity.ok(ChargeSessionResponse.from(s));
    }

    @Operation(summary = "Consulta una sesion por id")
    @GetMapping("/{id}")
    public ResponseEntity<ChargeSessionResponse> getOne(@PathVariable("id") String id) {
        return ResponseEntity.ok(ChargeSessionResponse.from(get.execute(new SessionId(id))));
    }

    @Operation(summary = "Lista sesiones de un usuario")
    @GetMapping
    public List<ChargeSessionResponse> listByUser(@RequestParam("userId") String userId) {
        return list.execute(new UserId(userId)).stream().map(ChargeSessionResponse::from).toList();
    }
}
