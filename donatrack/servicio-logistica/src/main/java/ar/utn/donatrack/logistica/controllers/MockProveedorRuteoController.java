package ar.utn.donatrack.logistica.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Mock del proveedor externo de ruteo. Reemplaza al servicio real
 * (que normalmente viviria en otro host) para poder probar en local el flujo
 * de planificacion sin un 'Connection Refused'.
 *
 * Recibe el mismo payload que arma ProveedorRuteoExternoAdapter
 * (loteId, tokenCorrelacion, callbackUrl, camiones, donaciones), loguea que
 * llegó el lote y responde 200 OK simulando que aceptó la solicitud.
 */
@RestController
@RequestMapping("/ruteo")
@Tag(name = "Mock Proveedor Ruteo", description = "Stub del proveedor externo de ruteo para pruebas locales")
public class MockProveedorRuteoController {

    private static final Logger log = LoggerFactory.getLogger(MockProveedorRuteoController.class);

    @Operation(
            summary = "Recibir lote a planificar (mock)",
            description = "Simula al proveedor externo: loguea el lote recibido y responde 200 OK. No calcula rutas reales.")
    @PostMapping("/planificar")
    public ResponseEntity<Void> planificar(@RequestBody Map<String, Object> payload) {
        log.info("[MockProveedorRuteoController] Lote recibido para planificar: loteId={}, tokenCorrelacion={}, callbackUrl={}",
                payload.get("loteId"), payload.get("tokenCorrelacion"), payload.get("callbackUrl"));
        return ResponseEntity.ok().build();
    }
}
