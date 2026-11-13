package ar.utn.donatrack.notificaciones.controller;

import ar.utn.donatrack.notificaciones.dto.MarcarLeidasDto;
import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.dto.response.NotificacionResponse;
import ar.utn.donatrack.notificaciones.dto.response.PaginaNotificacionesResponse;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificacionServiceInterface;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.seguridad.ControlDeAcceso;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Expone el componente notificador solicitado por el enunciado:
 * "dado un destinatario, un mensaje y un medio de notificación,
 * pueda realizar el envío".
 * Es consumido vía HTTP REST por el Servicio de Donaciones y el
 * Servicio de Incentivos cuando ocurren los eventos relevantes.
 *
 * Entrega 6: leer pide el JWT de Autenticación y cada persona ve solo lo suyo
 * (ver ControlDeAcceso); el listado se filtra por destinatario acá, paginado
 * desde 1, en lugar de devolver todas para que las filtre el front.
 */
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionServiceInterface notificacionService;
    private final ControlDeAcceso acceso;

    @PostMapping
    public ResponseEntity<Void> enviar(
            @RequestHeader(value = ControlDeAcceso.CABECERA_SERVICIO, defaultValue = "") String clave,
            @RequestBody @Valid SolicitudNotificacionDto solicitud) {
        acceso.exigirServicio(clave);
        notificacionService.enviar(solicitud);
        return ResponseEntity.ok().build();
    }

    /**
     * Las notificaciones de esos destinatarios (o, sin destinatario, las de la
     * persona; para la administración, todas), de la más nueva a la más vieja.
     */
    @GetMapping
    public PaginaNotificacionesResponse listar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera,
            @RequestHeader(value = ControlDeAcceso.CABECERA_FRONTEND, defaultValue = "") String claveDelFront,
            @RequestParam(required = false) List<String> destinatario,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<String> destinatarios = acceso.destinatariosPermitidos(acceso.lector(cabecera), claveDelFront, destinatario);
        return PaginaNotificacionesResponse.desde(notificacionService.buscar(destinatarios, page, size));
    }

    /** Cuántas sin leer: para el numerito de la campanita, sin traer las notificaciones. */
    @GetMapping("/no-leidas")
    public Map<String, Long> noLeidas(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera,
            @RequestHeader(value = ControlDeAcceso.CABECERA_FRONTEND, defaultValue = "") String claveDelFront,
            @RequestParam(required = false) List<String> destinatario) {
        List<String> destinatarios = acceso.destinatariosPermitidos(acceso.lector(cabecera), claveDelFront, destinatario);
        return Map.of("cantidad", notificacionService.contarNoLeidas(destinatarios));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponse> obtenerPorId(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera,
            @PathVariable UUID id) {
        ControlDeAcceso.Lector lector = acceso.lector(cabecera);
        Notificacion notificacion = notificacionService.obtenerPorId(id);
        acceso.exigirPropia(lector, notificacion.getDestinatario());
        return ResponseEntity.ok(NotificacionResponse.desde(notificacion));
    }

    /** Marca una notificación puntual como leída. */
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera,
            @PathVariable UUID id) {
        ControlDeAcceso.Lector lector = acceso.lector(cabecera);
        acceso.exigirPropia(lector, notificacionService.obtenerPorId(id).getDestinatario());
        notificacionService.marcarLeida(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Marca como leídas todas las notificaciones pendientes de los
     * destinatarios recibidos (el frontend lo llama al abrir la pantalla de
     * notificaciones, para que desaparezca el contador de la campanita).
     */
    @PatchMapping("/leidas")
    public ResponseEntity<Void> marcarLeidas(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera,
            @RequestHeader(value = ControlDeAcceso.CABECERA_FRONTEND, defaultValue = "") String claveDelFront,
            @RequestBody MarcarLeidasDto solicitud) {
        ControlDeAcceso.Lector lector = acceso.lector(cabecera);
        List<String> destinatarios = acceso.destinatariosPermitidos(lector, claveDelFront, solicitud.destinatarios());
        // La administración sin destinatarios no marca todas las del sistema: solo las suyas.
        notificacionService.marcarLeidas(destinatarios == null ? List.of(lector.email()) : destinatarios);
        return ResponseEntity.ok().build();
    }
}
