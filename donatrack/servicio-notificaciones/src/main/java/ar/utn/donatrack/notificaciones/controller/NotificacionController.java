package ar.utn.donatrack.notificaciones.controller;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.dto.response.NotificacionResponse;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificacionServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Expone el componente notificador solicitado por el enunciado:
 * "dado un destinatario, un mensaje y un medio de notificación,
 * pueda realizar el envío".
 * Es consumido vía HTTP REST por el Servicio de Donaciones y el
 * Servicio de Incentivos cuando ocurren los eventos relevantes.
 */
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionServiceInterface notificacionService;

    @PostMapping
    public ResponseEntity<Void> enviar(@RequestBody @Valid SolicitudNotificacionDto solicitud) {
        notificacionService.enviar(solicitud);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> obtenerTodas() {
        List<NotificacionResponse> respuesta = notificacionService.obtenerTodas().stream()
                .map(NotificacionResponse::desde)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(NotificacionResponse.desde(notificacionService.obtenerPorId(id)));
    }
}
