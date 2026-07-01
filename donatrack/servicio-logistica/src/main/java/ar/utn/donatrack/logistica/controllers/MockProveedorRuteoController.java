package ar.utn.donatrack.logistica.controllers;

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
public class MockProveedorRuteoController {

    private static final Logger log = LoggerFactory.getLogger(MockProveedorRuteoController.class);

    @PostMapping("/planificar")
    public ResponseEntity<Void> planificar(@RequestBody Map<String, Object> payload) {
        log.info("[MockProveedorRuteoController] Lote recibido para planificar: loteId={}, tokenCorrelacion={}, callbackUrl={}",
                payload.get("loteId"), payload.get("tokenCorrelacion"), payload.get("callbackUrl"));
        return ResponseEntity.ok().build();
    }
}
