package ar.utn.donatrack.autenticacion.controllers;

import ar.utn.donatrack.autenticacion.dtos.request.AltaChofer;
import ar.utn.donatrack.autenticacion.dtos.request.Asignacion;
import ar.utn.donatrack.autenticacion.dtos.request.Login;
import ar.utn.donatrack.autenticacion.dtos.request.Registro;
import ar.utn.donatrack.autenticacion.dtos.request.TokenSocial;
import ar.utn.donatrack.autenticacion.dtos.request.Vinculo;
import ar.utn.donatrack.autenticacion.dtos.response.PerfilSesion;
import ar.utn.donatrack.autenticacion.dtos.response.TokenResponse;
import ar.utn.donatrack.autenticacion.models.Rol;
import ar.utn.donatrack.autenticacion.seguridad.ClavesCompartidas;
import ar.utn.donatrack.autenticacion.seguridad.Tokens;
import ar.utn.donatrack.autenticacion.services.AutenticacionService;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/autenticacion")
public class AutenticacionController {
    private static final String PREFIJO_BEARER = "Bearer ";

    private final AutenticacionService cuentas;
    private final Tokens tokens;
    private final ClavesCompartidas claves;

    AutenticacionController(AutenticacionService cuentas, Tokens tokens, ClavesCompartidas claves) {
        this.cuentas = cuentas;
        this.tokens = tokens;
        this.claves = claves;
    }

    @PostMapping("/registro") // las cuentas de choferes solo pueden ser creadas por un administrador, no por el chofer mismo.
    ResponseEntity<TokenResponse> registrar(@Valid @RequestBody Registro datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentas.registrar(datos.email(), datos.nombre(),
                datos.password(), datos.rol(), datos.tipoDonante()));
    }

    @PostMapping("/ingreso")
    TokenResponse ingresar(@Valid @RequestBody Login datos) {
        return cuentas.ingresar(datos.email(), datos.password(), datos.rol());
    }

    @PostMapping("/ingreso-social")
    TokenResponse canjear(@Valid @RequestBody TokenSocial datos) {
        if (!"google".equals(datos.proveedor())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proveedor de identidad inválido.");
        }
        return cuentas.ingresarConGoogle(datos.idToken(), datos.rol(), datos.tipoDonante());
    }

    @GetMapping("/sesion")
    PerfilSesion consultarSesion(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera) {
        Jwt token = verificarToken(cabecera);
        UUID cuentaId = UUID.fromString(token.getSubject());
        return PerfilSesion.crearPerfilPara(cuentas.buscarCuenta(cuentaId));
    }

    @GetMapping("/choferes")
    List<PerfilSesion> choferes(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera) {
        exigirAdministrador(cabecera);
        return cuentas.choferes().stream().map(PerfilSesion::crearPerfilPara).toList();
    }

    @PutMapping("/choferes/{id}/camion")
    PerfilSesion asignarCamion(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera, @PathVariable UUID id, @RequestBody Asignacion datos) {
        exigirAdministrador(cabecera);
        return PerfilSesion.crearPerfilPara(cuentas.asignarCamion(id, datos.camionId()));
    }

    @PostMapping("/choferes")
    @ResponseStatus(HttpStatus.CREATED)
    PerfilSesion crearChofer(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera, @Valid @RequestBody AltaChofer datos) {
        exigirAdministrador(cabecera);
        return PerfilSesion.crearPerfilPara(cuentas.crearChofer(datos.email(), datos.nombre(), datos.password()));
    }

    private void exigirAdministrador(String cabecera) {
        if (!Rol.ADMINISTRADOR.name().equals(verificarToken(cabecera).getClaimAsString("rol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta acción requiere una cuenta administradora.");
        }
    }

    @PostMapping("/perfil")
    TokenResponse vincularPerfil(@RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecera, 
                                @RequestHeader(value = ClavesCompartidas.CABECERA_FRONTEND, defaultValue = "") String clave, @Valid @RequestBody Vinculo datos) {
        claves.exigirFrontend(clave, "La vinculación requiere un frontend autorizado.");
        Jwt token = verificarToken(cabecera);
        UUID cuentaId = UUID.fromString(token.getSubject());
        return cuentas.vincularPerfil(cuentaId, datos.perfilId());
    }

    private Jwt verificarToken(String cabecera) {
        if (!cabecera.startsWith(PREFIJO_BEARER)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere un token.");
        }
        try {
            return tokens.verificar(cabecera.substring(PREFIJO_BEARER.length()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o vencido.");
        }
    }

    @GetMapping("/claves-publicas")
    Map<String, Object> clavesPublicas() {
        return tokens.clavesPublicas();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> cuentaDuplicada() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Ya existe una cuenta para ese perfil."));
    }

}
