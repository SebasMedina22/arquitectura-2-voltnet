package com.voltnet.billing.infrastructure.rest;

import com.voltnet.billing.application.port.in.QueryInvoicesUseCase;
import com.voltnet.billing.infrastructure.rest.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de consulta - solo demo/observabilidad.
 * Billing es un MS asincrono: no expone APIs de negocio a clientes externos.
 * Estos endpoints sirven para visualizar el estado en sustentacion y para
 * cumplir el requisito "Swagger/OpenAPI funcional por MS" de la rubrica.
 */
@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoices (consulta)", description = "Endpoints de consulta interna. Billing recibe trabajo via AMQP.")
public class InvoiceQueryController {

    private final QueryInvoicesUseCase queryInvoices;

    public InvoiceQueryController(QueryInvoicesUseCase queryInvoices) {
        this.queryInvoices = queryInvoices;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una factura por su id")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable UUID id) {
        return queryInvoices.findById(id)
                .map(InvoiceResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Lista facturas por usuario")
    public ResponseEntity<List<InvoiceResponse>> listByUser(@RequestParam String userId) {
        List<InvoiceResponse> body = queryInvoices.findByUserId(userId).stream()
                .map(InvoiceResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(body);
    }
}
