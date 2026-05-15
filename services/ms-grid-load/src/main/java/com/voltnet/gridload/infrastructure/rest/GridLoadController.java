package com.voltnet.gridload.infrastructure.rest;

import com.voltnet.gridload.application.port.in.GetStationLoadUseCase;
import com.voltnet.gridload.application.port.in.SetStationLoadUseCase;
import com.voltnet.gridload.domain.model.StationLoad;
import com.voltnet.gridload.infrastructure.rest.dto.GridLoadResponse;
import com.voltnet.gridload.infrastructure.rest.dto.SetGridLoadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grid/load")
@Tag(name = "GridLoad", description = "Consulta y simulacion de carga por estacion")
public class GridLoadController {

    private final GetStationLoadUseCase getStationLoad;
    private final SetStationLoadUseCase setStationLoad;

    public GridLoadController(GetStationLoadUseCase getStationLoad,
                              SetStationLoadUseCase setStationLoad) {
        this.getStationLoad = getStationLoad;
        this.setStationLoad = setStationLoad;
    }

    @GetMapping
    @Operation(summary = "Obtiene la carga actual de una estacion")
    public ResponseEntity<GridLoadResponse> get(@RequestParam String stationId) {
        StationLoad load = getStationLoad.getLoad(stationId);
        return ResponseEntity.ok(GridLoadResponse.fromDomain(load));
    }

    @PostMapping
    @Operation(summary = "Setea o simula la carga de una estacion (admin)")
    public ResponseEntity<GridLoadResponse> set(@Valid @RequestBody SetGridLoadRequest request) {
        StationLoad load = setStationLoad.setLoad(request.stationId(), request.currentLoadKw());
        return ResponseEntity.ok(GridLoadResponse.fromDomain(load));
    }
}
