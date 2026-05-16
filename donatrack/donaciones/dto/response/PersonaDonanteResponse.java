package com.donatrack.donaciones.dto.response;

import com.donatrack.donaciones.dominio.*;
import com.donatrack.donaciones.dominio.contacto.MedioDeContacto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta unificado para personas donantes.
 * Aplana la jerarquía de dominio para el cliente REST.
 *
 * PATRÓN STATE: expone el estado como String ("ACTIVO" / "INACTIVO")
 * en lugar de un boolean, para que el cliente REST reciba el valor
 * nombrado y no tenga que interpretar un true/false.
 */
public record PersonaDonanteResponse(

        UUID id,
        String tipoPersona,

        // (STATE): se expone el nombre del estado, no un boolean
        String estado,           // ← "ACTIVO" o "INACTIVO" — viene de EstadoDonante.name()

        // Campos de PersonaHumana (null si es jurídica)
        String nombre,
        String apellido,
        Integer edad,
        String numeroDocumento,
        String genero,
        DireccionResponse direccion,

        // Campos de PersonaJuridica (null si es humana)
        String razonSocial,
        String tipoPersonaJuridica,
        String rubro,
        List<RepresentanteResponse> representantes,

        // Comunes
        List<MedioDeContactoResponse> mediosDeContacto,
        String medioContactoPredeterminado
) {

    // ─── Métodos de fábrica estáticos ─────────────────────────────────────────

    public static PersonaDonanteResponse desdePersonaHumana(PersonaHumana p) {
        return new PersonaDonanteResponse(
                p.getId(),
                "HUMANA",
                p.getEstado().name(),          // ← PATRÓN STATE: getEstado() en lugar de isActivo()
                p.getNombre(),
                p.getApellido(),
                p.getEdad(),
                p.getNumeroDocumento(),
                p.getGenero().name(),
                DireccionResponse.desde(p.getDireccion()),
                null, null, null, null,
                mapearMedios(p.getMediosDeContacto()),
                p.getMedioContactoPredeterminado().getValor()
        );
    }

    public static PersonaDonanteResponse desdePersonaJuridica(PersonaJuridica p) {
        return new PersonaDonanteResponse(
                p.getId(),
                "JURIDICA",
                p.getEstado().name(),          // ← PATRÓN STATE: getEstado() en lugar de isActivo()
                null, null, null, null, null, null,
                p.getRazonSocial(),
                p.getTipo().name(),
                p.getRubro(),
                p.getRepresentantes().stream().map(RepresentanteResponse::desde).toList(),
                mapearMedios(p.getMediosDeContacto()),
                p.getMedioContactoPredeterminado().getValor()
        );
    }

    public static PersonaDonanteResponse desde(PersonaDonante p) {
        if (p instanceof PersonaHumana ph) return desdePersonaHumana(ph);
        if (p instanceof PersonaJuridica pj) return desdePersonaJuridica(pj);
        throw new IllegalArgumentException("Tipo de persona donante desconocido: " + p.getClass());
    }

    private static List<MedioDeContactoResponse> mapearMedios(List<MedioDeContacto> medios) {
        return medios.stream()
                .map(m -> new MedioDeContactoResponse(m.getTipo().name(), m.getValor()))
                .toList();
    }

    // ─── Records anidados ─────────────────────────────────────────────────────

    public record DireccionResponse(String calle, String numero, String localidad,
                                    String provincia, String codigoPostal) {
        public static DireccionResponse desde(Direccion d) {
            return new DireccionResponse(d.getCalle(), d.getNumero(),
                    d.getLocalidad(), d.getProvincia(), d.getCodigoPostal());
        }
    }

    public record MedioDeContactoResponse(String tipo, String valor) {}

    public record RepresentanteResponse(String nombre, String apellido,
                                        String email, String telefono) {
        public static RepresentanteResponse desde(Representante r) {
            return new RepresentanteResponse(r.getNombre(), r.getApellido(),
                    r.getEmail(), r.getTelefono());
        }
    }
}
