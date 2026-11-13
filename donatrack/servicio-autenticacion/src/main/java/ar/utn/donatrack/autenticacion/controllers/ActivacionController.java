package ar.utn.donatrack.autenticacion.controllers;

import ar.utn.donatrack.autenticacion.dtos.request.ConfirmacionActivacion;
import ar.utn.donatrack.autenticacion.dtos.request.SolicitudActivacion;
import ar.utn.donatrack.autenticacion.dtos.response.TokenResponse;
import ar.utn.donatrack.autenticacion.seguridad.ClavesCompartidas;
import ar.utn.donatrack.autenticacion.services.ActivacionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Enlaces de activación. Los dos endpoints los llama solo el frontend (con su
 * clave), así nadie de afuera los usa para mandar correos a cualquier dirección.
 *
 * - POST /activaciones: manda el enlace si hay una cuenta con ese email y rol.
 *   Responde 202 siempre, sin decir si la cuenta existe.
 * - POST /activaciones/confirmar: guarda la contraseña y devuelve la sesión.
 *   404 enlace desconocido o reemplazado, 409 ya usado, 410 vencido.
 */
@RestController
@RequestMapping("/api/autenticacion/activaciones")
public class ActivacionController {
    private static final String SOLO_FRONTEND = "La activación de cuentas requiere un frontend autorizado.";

    private final ActivacionService activaciones;
    private final ClavesCompartidas claves;

    ActivacionController(ActivacionService activaciones, ClavesCompartidas claves) {
        this.activaciones = activaciones;
        this.claves = claves;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, String> solicitar(@RequestHeader(value = ClavesCompartidas.CABECERA_FRONTEND, defaultValue = "") String clave,
                                  @Valid @RequestBody SolicitudActivacion pedido) {
        claves.exigirFrontend(clave, SOLO_FRONTEND);
        activaciones.solicitar(pedido.email(), pedido.rolOPorDefecto());
        return Map.of("message", "Si hay una cuenta con ese email, te mandamos un enlace para elegir tu contraseña.");
    }

    @PostMapping("/confirmar")
    TokenResponse confirmar(@RequestHeader(value = ClavesCompartidas.CABECERA_FRONTEND, defaultValue = "") String clave,
                            @Valid @RequestBody ConfirmacionActivacion datos) {
        claves.exigirFrontend(clave, SOLO_FRONTEND);
        return activaciones.confirmar(datos.codigo(), datos.password());
    }
}
