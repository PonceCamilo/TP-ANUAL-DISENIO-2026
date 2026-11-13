package ar.utn.donatrack.autenticacion.services;

import ar.utn.donatrack.autenticacion.dtos.request.CuentasImportadas;
import ar.utn.donatrack.autenticacion.dtos.response.ResultadoImportacion;
import ar.utn.donatrack.autenticacion.models.Cuenta;
import ar.utn.donatrack.autenticacion.models.CuentaDonante;
import ar.utn.donatrack.autenticacion.models.Rol;
import ar.utn.donatrack.autenticacion.models.TipoDonante;
import ar.utn.donatrack.autenticacion.repositories.Cuentas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Cuentas de las personas que la administración cargó en Donaciones, por CSV o con
 * el alta manual (Entrega 6, requisito 20).
 *
 * Donaciones avisa de a tandas (su bandeja de salida reintenta si no respondemos).
 * Cada cuenta nace vinculada al perfil de la persona y sin contraseña: la persona
 * la activa con un enlace que pide por email (ActivacionService). No se usa BCrypt
 * acá, así que 10.000 personas se procesan rápido.
 *
 * Es idempotente por email: reimportar el mismo archivo no cambia nada. Si ya hay
 * una cuenta con ese email que no está vinculada a esa persona, no se toca: sería
 * entregarle el perfil a quien se registró primero sin probar que el email es suyo.
 */
@Service
public class CuentasImportadasService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Cuentas cuentas;

    CuentasImportadasService(Cuentas cuentas) {
        this.cuentas = cuentas;
    }

    @Transactional
    public ResultadoImportacion registrar(List<CuentasImportadas.Donante> donantes) {
        int creadas = 0;
        int existentes = 0;
        List<ResultadoImportacion.Rechazo> rechazadas = new ArrayList<>();
        for (CuentasImportadas.Donante donante : donantes) {
            String email = donante.email() == null ? "" : donante.email().strip().toLowerCase(Locale.ROOT);
            String motivo = motivoDeRechazo(donante, email);
            if (motivo != null) {
                rechazadas.add(new ResultadoImportacion.Rechazo(email, motivo));
                continue;
            }
            Cuenta existente = cuentas.findByEmailAndRol(email, Rol.DONANTE).orElse(null);
            if (existente instanceof CuentaDonante cuenta && Objects.equals(cuenta.donanteId(), donante.donanteId())) {
                existentes++;
            } else if (existente != null) {
                rechazadas.add(new ResultadoImportacion.Rechazo(email,
                        "Ya hay una cuenta con ese email que no está vinculada a esta persona."));
            } else if (cuentas.existeDonanteVinculadoA(donante.donanteId())) {
                rechazadas.add(new ResultadoImportacion.Rechazo(email,
                        "La persona ya tiene una cuenta con otro email."));
            } else {
                cuentas.save(CuentaDonante.importada(email, nombre(donante, email), tipo(donante), donante.donanteId()));
                creadas++;
            }
        }
        cuentas.flush();
        return new ResultadoImportacion(creadas, existentes, rechazadas);
    }

    private static String motivoDeRechazo(CuentasImportadas.Donante donante, String email) {
        if (donante.donanteId() == null) {
            return "Falta el id de la persona en Donaciones.";
        }
        if (email.isEmpty() || email.length() > 150 || !EMAIL.matcher(email).matches()) {
            return "El email no es válido.";
        }
        if (donante.tipoDonante() != null && !"HUMANA".equals(donante.tipoDonante())
                && !"JURIDICA".equals(donante.tipoDonante())) {
            return "El tipo de persona tiene que ser HUMANA o JURIDICA.";
        }
        return null;
    }

    private static String nombre(CuentasImportadas.Donante donante, String email) {
        String nombre = donante.nombre() == null || donante.nombre().isBlank() ? email : donante.nombre().strip();
        return nombre.substring(0, Math.min(100, nombre.length()));
    }

    private static TipoDonante tipo(CuentasImportadas.Donante donante) {
        return "JURIDICA".equals(donante.tipoDonante()) ? TipoDonante.JURIDICA : TipoDonante.HUMANA;
    }
}
