package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.model.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.model.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.model.entidad.Direccion;
import ar.utn.donatrack.donaciones.model.donante.Genero;
import ar.utn.donatrack.donaciones.model.contacto.MedioDeContacto;


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
        String[] partes = dto.nombreORazonSocial().split(" ", 2);
        String nombre   = partes[0];
        String apellido = partes.length > 1 ? partes[1] : "-";

        // El modelo actual tiene un constructor simple (nombre, apellido, edad, documento)
        return new PersonaHumana(nombre, apellido, 0, dto.documento(), Genero.PREFIERO_NO_DECIR, new Direccion("Pendiente", 0 , "Pendiente", "Pendiente", "Pendiente"), List.of());
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
        // El modelo actual de PersonaJuridica acepta (razonSocial, rubro)
        return new PersonaJuridica(dto.nombreORazonSocial(), "No especificado");
    }
    // Helpers: no-op por ahora; el dominio actual es mínimo.
}
