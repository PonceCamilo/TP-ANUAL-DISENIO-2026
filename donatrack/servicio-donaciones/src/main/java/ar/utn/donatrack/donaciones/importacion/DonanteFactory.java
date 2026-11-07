package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.models.donante.Documento;
import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import org.springframework.stereotype.Component;

/**
 * Factory que construye instancias de dominio a partir de los datos parciales
 * disponibles en el CSV de importación.
 *
 * El CSV no incluye todos los campos del dominio (edad, género, dirección, tipo
 * jurídico, rubro, representantes). Esta factory completa esos campos con valores
 * neutros para satisfacer los invariantes del dominio. Un administrador deberá
 * completar la información faltante desde la interfaz web.
 *
 * Lo que el CSV sí trae para contactar a la persona se guarda (Entrega 5): el
 * teléfono queda como contacto TELEFONO y el email como medio predeterminado.
 * Antes el teléfono se descartaba y el donante importado quedaba sin medio
 * predeterminado, así que nunca recibía el aviso de inactividad.
 */

@Component
public class DonanteFactory {

    public PersonaDonante crearPersona(DonanteImportDto dto) {
        PersonaDonante persona;

        if ("JURIDICA".equals(dto.tipoPersona())) {
            persona = PersonaJuridica.builder()
                .documento(Documento.deOrganizacion(dto.tipoDoc(), dto.documento()))
                .razonSocial(dto.nombreORazonSocial())
                .email(dto.email())
                .build();
        } else {
            // HUMANA: inferir nombre y apellido separando por el primer espacio
            String[] partes = dto.nombreORazonSocial().split(" ", 2);
            String nombre = partes[0];
            String apellido = partes.length > 1 ? partes[1] : "-";

            persona = PersonaHumana.builder()
                .documento(Documento.de(dto.tipoDoc(), dto.documento()))
                .email(dto.email())
                .nombre(nombre)
                .apellido(apellido)
                .build();
        }

        persona.definirContactoPredeterminado(Email.builder().valor(dto.email()).build());
        if (dto.telefono() != null && !dto.telefono().isBlank()) {
            persona.reemplazarContacto(Telefono.builder().valor(dto.telefono()).build());
        }

        // Todo donante importado arranca ACTIVO (el estado inicial) y con la
        // interacción inicial registrada.
        persona.registrarInteraccion();
        return persona;
    }
}
