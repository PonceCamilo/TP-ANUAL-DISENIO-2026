package ar.utn.donatrack.notificaciones.controller;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificacionServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Void> enviar(@RequestBody SolicitudNotificacionDto solicitud) {
        notificacionService.enviar(solicitud);
        return ResponseEntity.ok().build();
    }
    /** Este metodo lo que hace es recivir la soli y pasarla al service que va a ser el que lo haga */
}
