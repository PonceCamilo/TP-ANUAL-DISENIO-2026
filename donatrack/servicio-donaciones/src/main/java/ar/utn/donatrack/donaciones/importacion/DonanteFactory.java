package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.springframework.stereotype.Component;

/**
 * Factory que construye instancias de dominio a partir de los datos parciales
 * disponibles en el CSV de importación.
 *
 * El CSV no incluye todos los campos del dominio (edad, género, dirección, tipo
 * jurídico, rubro, representantes). Esta factory completa esos campos con valores
 * neutros para satisfacer los invariantes del dominio. Un administrador deberá
 * completar la información faltante desde la interfaz web.
 */

@Component
public class DonanteFactory {

    public PersonaDonante crearPersona(DonanteImportDto dto) {
        PersonaDonante persona;

        if ("JURIDICA".equals(dto.tipoPersona())) {
            persona = PersonaJuridica.builder()
                .tipoDocumento(dto.tipoDoc())
                .numeroDocumento(dto.documento())
                .razonSocial(dto.nombreORazonSocial())
                .email(dto.email())
                .build();
        } else {
            // HUMANA: inferir nombre y apellido separando por el primer espacio
            String[] partes = dto.nombreORazonSocial().split(" ", 2);
            String nombre = partes[0];
            String apellido = partes.length > 1 ? partes[1] : "-";

            persona = PersonaHumana.builder()
                .tipoDocumento(dto.tipoDoc())
                .numeroDocumento(dto.documento())
                .email(dto.email())
                .nombre(nombre)
                .apellido(apellido)
                .build();
        }

        // Todo donante importado arranca ACTIVO y con interacción inicial registrada
        persona.setEstado(EstadoDonante.ACTIVO);
        persona.setUltimaInteraccion(FechaHoraArgentina.ahora());
        return persona;
    }
}
