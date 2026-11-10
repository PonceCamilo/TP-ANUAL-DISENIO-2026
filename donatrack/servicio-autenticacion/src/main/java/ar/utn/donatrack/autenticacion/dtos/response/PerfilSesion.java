package ar.utn.donatrack.autenticacion.dtos.response;

import ar.utn.donatrack.autenticacion.models.Cuenta;
import ar.utn.donatrack.autenticacion.models.CuentaChofer;
import ar.utn.donatrack.autenticacion.models.CuentaDonante;
import ar.utn.donatrack.autenticacion.models.CuentaEntidadBeneficiaria;
import ar.utn.donatrack.autenticacion.models.Rol;
import ar.utn.donatrack.autenticacion.models.TipoDonante;

import java.util.UUID;

public record PerfilSesion(UUID id, String email, String nombre, Rol rol, TipoDonante tipoDonante, UUID perfilId, UUID camionId) {
    public static PerfilSesion crearPerfilPara(Cuenta cuenta) {
        TipoDonante tipo = cuenta instanceof CuentaDonante donante ? donante.tipoDonante() : null;
        UUID perfilId = switch (cuenta) {
            case CuentaDonante donante -> donante.donanteId();
            case CuentaEntidadBeneficiaria entidad -> entidad.entidadBeneficiariaId();
            default -> null;
        };
        UUID camionId = cuenta instanceof CuentaChofer chofer ? chofer.camionId() : null;
        return new PerfilSesion(cuenta.id(), cuenta.email(), cuenta.nombre(), cuenta.rol(), tipo, perfilId, camionId);
    }
}
