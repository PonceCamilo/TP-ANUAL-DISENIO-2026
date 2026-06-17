package ar.utn.donatrack.notificaciones.controller;

import ar.utn.donatrack.notificaciones.dto.request.EnviarNotificacionRequest;
import ar.utn.donatrack.notificaciones.dto.response.NotificacionResponse;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.service.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST del Servicio de Notificaciones.
 *
 * POST  /api/notificaciones/enviar   → enviar una notificación
 * GET   /api/notificaciones          → historial (auditoría)
 */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    /**
     * Endpoint principal: recibe destinatario + mensaje + medio y envía (mock).
     * Este es el endpoint que llaman los demás servicios (donaciones, incentivos).
     */
    @PostMapping("/enviar")
    public ResponseEntity<NotificacionResponse> enviar(
            @Valid @RequestBody EnviarNotificacionRequest request) {

        Notificacion notificacion = service.enviar(
                request.destinatario(),
                request.mensaje(),
                request.medio()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NotificacionResponse.desde(notificacion));
    }

    /** Historial completo para auditoría. */
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listarTodas() {
        List<NotificacionResponse> respuesta = service.listarTodas()
                .stream()
                .map(NotificacionResponse::desde)
                .toList();
        return ResponseEntity.ok(respuesta);
    }
}
