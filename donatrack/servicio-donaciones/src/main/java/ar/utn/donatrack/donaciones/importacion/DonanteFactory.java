package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.model.donante.PersonaHumanaDonante;
import ar.utn.donatrack.donaciones.model.donante.PersonaJuridicaDonante;

import java.util.Objects;

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
    public PersonaDonante crearPersona(DonanteImportDto dto) {

        if (Objects.equals(dto.tipoPersona(), "JURIDICA")) {
            return PersonaJuridicaDonante.builder()
                .tipoPersona(dto.tipoPersona())
                .tipoDocumento(dto.tipoDoc())
                .numeroDocumento(dto.documento())
                .razonSocial(dto.nombreORazonSocial())
                .email(dto.email())
                .telefono(dto.telefono())
                .build();
        }

        String[] partes = dto.nombreORazonSocial().split(" ", 2);
        String nombre = partes[0];
        String apellido = partes.length > 1 ? partes[1] : "-";

        return PersonaHumanaDonante.builder()
            .tipoPersona(dto.tipoPersona())
            .tipoDocumento(dto.tipoDoc())
            .numeroDocumento(dto.documento())
            .email(dto.email())
            .nombre(nombre)
            .apellido(apellido)
            .telefono(dto.telefono())
            .build();
    }
}
