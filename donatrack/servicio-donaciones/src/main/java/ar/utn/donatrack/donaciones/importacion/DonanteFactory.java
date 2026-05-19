package com.donatrack.donaciones.importacion;

import com.donatrack.donaciones.dominio.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory que construye instancias de dominio (PersonaHumana / PersonaJuridica)
 * a partir de los datos parciales disponibles en el CSV de importación.
 *
 * Decisión de diseño: el CSV no incluye todos los campos requeridos por el
 * dominio (edad, género, dirección, tipo de jurídica, rubro, representantes).
 * La factory completa esos campos con valores neutros que satisfacen los
 * invariantes del dominio y dejan en claro que deben completarse luego por
 * un administrador.
 */
public class DonanteFactory {

    /**
     * Crea una PersonaHumana desde el DTO de importación.
     *
     * - Nombre y apellido se infieren separando por el primer espacio.
     * - Edad: 0 (desconocida).
     * - Género: PREFIERO_NO_DECIR (valor neutro).
     * - Dirección: valores "Pendiente" para satisfacer el invariante.
     */
    public PersonaHumana crearHumana(DonanteImportDto dto) {
        List<MedioDeContacto> medios = construirMedios(dto);

        String[] partes = dto.nombreORazonSocial().split(" ", 2);
        String nombre   = partes[0];
        String apellido = partes.length > 1 ? partes[1] : "-";

        return new PersonaHumana(
                nombre,
                apellido,
                0,
                dto.documento(),
                Genero.PREFIERO_NO_DECIR,
                direccionVacia(),
                medios
        );
    }

    /**
     * Crea una PersonaJuridica desde el DTO de importación.
     *
     * - Tipo: EMPRESA por defecto (desconocido en el CSV).
     * - Rubro: "No especificado".
     * - Representante: placeholder con el email de la organización;
     *   debe completarse por un administrador.
     */
    public PersonaJuridica crearJuridica(DonanteImportDto dto) {
        List<MedioDeContacto> medios = construirMedios(dto);

        Representante repPlaceholder = new Representante(
                "Pendiente",
                "Completar",
                dto.email(),
                dto.telefono()
        );

        return new PersonaJuridica(
                dto.nombreORazonSocial(),
                TipoPersonaJuridica.EMPRESA,
                "No especificado",
                medios,
                List.of(repPlaceholder)
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<MedioDeContacto> construirMedios(DonanteImportDto dto) {
        List<MedioDeContacto> medios = new ArrayList<>();
        medios.add(new Email(dto.email()));
        if (dto.telefono() != null) {
            medios.add(new Telefono(dto.telefono()));
        }
        return medios;
    }

    private Direccion direccionVacia() {
        return new Direccion("Pendiente", "S/N", "Pendiente", "Pendiente", "0000");
    }
}
