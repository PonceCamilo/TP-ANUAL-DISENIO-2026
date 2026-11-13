package ar.utn.donatrack.autenticacion.services;

import ar.utn.donatrack.autenticacion.dtos.response.TokenResponse;
import ar.utn.donatrack.autenticacion.models.Cuenta;
import ar.utn.donatrack.autenticacion.models.CuentaAdministrador;
import ar.utn.donatrack.autenticacion.models.CuentaChofer;
import ar.utn.donatrack.autenticacion.models.CuentaDonante;
import ar.utn.donatrack.autenticacion.models.CuentaEntidadBeneficiaria;
import ar.utn.donatrack.autenticacion.models.Rol;
import ar.utn.donatrack.autenticacion.models.TipoDonante;
import ar.utn.donatrack.autenticacion.repositories.Activaciones;
import ar.utn.donatrack.autenticacion.repositories.Cuentas;
import ar.utn.donatrack.autenticacion.seguridad.Contrasenias;
import ar.utn.donatrack.autenticacion.seguridad.GoogleIdentidad;
import ar.utn.donatrack.autenticacion.seguridad.Tokens;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.List;
import java.util.UUID;

@Service
public class AutenticacionService {
    private final Cuentas cuentas;
    private final Activaciones activaciones;
    private final Tokens tokens;
    private final GoogleIdentidad google;
    private final Contrasenias claves;
    private final String hashDeRespaldo;

    AutenticacionService(Cuentas cuentas, Activaciones activaciones, Tokens tokens, GoogleIdentidad google, Contrasenias claves) {
        this.cuentas = cuentas;
        this.activaciones = activaciones;
        this.tokens = tokens;
        this.google = google;
        this.claves = claves;
        this.hashDeRespaldo = claves.codificar("contraseña de respaldo para una cuenta inexistente");
    }

    @Transactional
    public TokenResponse registrar(String email, String nombre, String password, Rol rol, TipoDonante tipo) {
        exigirAltaPublica(rol);
        return tokens.emitir(crearConClave(email, nombre, password, rol, tipo));
    }

    @Transactional(readOnly = true)
    public TokenResponse ingresar(String email, String password, Rol rol) {
        Cuenta cuenta = cuentas.findByEmailAndRol(normalizar(email), rol).orElse(null);
        String hash = cuenta == null || cuenta.passwordHash() == null ? hashDeRespaldo : cuenta.passwordHash();
        if (!claves.coincide(password, hash) || cuenta == null || cuenta.passwordHash() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email, contraseña o perfil incorrectos.");
        }
        return tokens.emitir(cuenta);
    }

    @Transactional
    public TokenResponse ingresarConGoogle(String idToken, Rol rol, TipoDonante tipo) {
        exigirAltaPublica(rol);
        var identidad = google.verificar(idToken);
        String googleSub = identidad.getSubject();
        var existente = rol == Rol.DONANTE
                ? cuentas.buscarDonantePorGoogleSub(googleSub)
                : cuentas.buscarEntidadPorGoogleSub(googleSub);
        if (existente.isPresent()) {
            return tokens.emitir(existente.get());
        }
        String email = normalizar(identidad.getClaimAsString("email"));
        comprobarCuentaNueva(email, rol);
        String nombre = identidad.getClaimAsString("name");
        if (nombre == null || nombre.isBlank()) {
            nombre = email;
        }
        nombre = nombre.substring(0, Math.min(100, nombre.length()));
        Cuenta nueva = crearCuenta(email, nombre, rol, tipo, null, googleSub);
        return tokens.emitir(cuentas.saveAndFlush(nueva));
    }

    @Transactional
    public TokenResponse vincularPerfil(UUID cuentaId, UUID perfilId) {
        Cuenta cuenta = buscarCuenta(cuentaId);
        switch (cuenta) {
            case CuentaDonante donante -> {
                comprobarVinculo(donante.donanteId(), perfilId);
                donante.vincularDonante(perfilId);
            }
            case CuentaEntidadBeneficiaria entidad -> {
                comprobarVinculo(entidad.entidadBeneficiariaId(), perfilId);
                entidad.vincularEntidadBeneficiaria(perfilId);
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Este rol no puede vincularse a un perfil de Donaciones.");
        }
        return tokens.emitir(cuentas.saveAndFlush(cuenta));
    }

    private static void comprobarVinculo(UUID perfilActual, UUID perfilSolicitado) {
        if (perfilActual != null && !perfilActual.equals(perfilSolicitado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya está vinculada a un perfil.");
        }
    }

    @Transactional(readOnly = true)
    public Cuenta buscarCuenta(UUID id) {
        return cuentas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta no existe."));
    }

    @Transactional(readOnly = true)
    public List<Cuenta> choferes() {
        return cuentas.findByRolOrderByNombreAsc(Rol.CHOFER);
    }

    @Transactional
    public Cuenta crearChofer(String email, String nombre, String password) {
        return crearConClave(email, nombre, password, Rol.CHOFER, null);
    }

    @Transactional
    public Cuenta asignarCamion(UUID choferId, UUID camionId) {
        CuentaChofer chofer = cuentas.findById(choferId)
                .filter(CuentaChofer.class::isInstance)
                .map(CuentaChofer.class::cast)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese chofer."));
        chofer.asignarCamion(camionId);
        return chofer;
    }

    @Transactional
    public TokenResponse renombrar(UUID cuentaId, String nombre) {
        Cuenta cuenta = buscarCuenta(cuentaId);
        try {
            cuenta.renombrar(nombre);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return tokens.emitir(cuentas.saveAndFlush(cuenta));
    }

    @Transactional
    public void eliminarChofer(UUID choferId) {
        Cuenta cuenta = cuentas.findById(choferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos ese chofer."));
        if (cuenta.rol() != Rol.CHOFER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden eliminar cuentas de chofer.");
        }
        activaciones.borrarDe(cuenta);
        cuentas.delete(cuenta);
    }

    @Transactional
    public void crearAdministradorInicial(String email, String password) {
        if (!email.isBlank() && cuentas.findByEmailAndRol(normalizar(email), Rol.ADMINISTRADOR).isEmpty()) {
            crearConClave(email, "Administración", password, Rol.ADMINISTRADOR, null);
        }
    }

    private Cuenta crearConClave(String email, String nombre, String password, Rol rol, TipoDonante tipo) {
        email = normalizar(email);
        comprobarCuentaNueva(email, rol);
        Cuenta cuenta = crearCuenta(email, nombre.strip(), rol, tipo, claves.codificar(password), null);
        return cuentas.saveAndFlush(cuenta);
    }

    private void comprobarCuentaNueva(String email, Rol rol) {
        var existente = cuentas.findByEmailAndRol(email, rol);
        if (existente.isPresent() && existente.get().pendienteDeActivacion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La administración ya cargó tus datos con ese email. "
                    + "Activá tu cuenta pidiendo el enlace de activación.");
        }
        if (existente.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta para ese email y perfil. Ingresá con el método original.");
        }
    }

    private static void exigirAltaPublica(Rol rol) {
        if (!rol.esDonante()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ese perfil requiere una cuenta provisionada.");
        }
    }

    private static Cuenta crearCuenta(String email, String nombre, Rol rol, TipoDonante tipo, String passwordHash, String googleSub) {
        return switch (rol) {
            case DONANTE -> new CuentaDonante(email, nombre, passwordHash, tipo, googleSub);
            case ENTIDAD_BENEFICIARIA -> new CuentaEntidadBeneficiaria(email, nombre, passwordHash, googleSub);
            case CHOFER -> new CuentaChofer(email, nombre, passwordHash);
            case ADMINISTRADOR -> new CuentaAdministrador(email, nombre, passwordHash);
        };
    }

    private static String normalizar(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
