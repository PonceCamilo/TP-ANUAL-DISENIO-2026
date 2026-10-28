package ar.utn.donatrack.donaciones.dtos;

import ar.utn.donatrack.donaciones.dtos.request.NecesidadRecurrenteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaHumanaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaJuridicaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.WhatsappRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.EmailResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.ErrorResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadRecurrenteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaHumanaResponseDTO;
import ar.utn.donatrack.donaciones.models.donante.Genero;
import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El JSON de los DTOs polimórficos, con el ObjectMapper de la aplicación: el
 * campo "tipo" elige la subclase al leer un pedido y aparece al escribir una
 * respuesta, y las fechas viajan como texto ISO-8601.
 */
@JsonTest
@DisplayName("JSON de los DTOs: subtipos por \"tipo\" y fechas ISO")
class JsonDeDtosTest {

    @Autowired private JacksonTester<PersonaDonanteRequestDTO> pedidoDeDonante;
    @Autowired private JacksonTester<NecesidadRequestDTO> pedidoDeNecesidad;
    @Autowired private JacksonTester<PersonaDonanteResponseDTO> donante;
    @Autowired private JacksonTester<NecesidadResponseDTO> necesidad;
    @Autowired private JacksonTester<ErrorResponseDTO> error;

    @Nested
    @DisplayName("Pedidos: el tipo elige la subclase")
    class Pedidos {

        @Test
        @DisplayName("Una persona HUMANA, con su género, su fecha y un contacto de WhatsApp")
        void personaHumana() throws Exception {
            PersonaDonanteRequestDTO leido = pedidoDeDonante.parseObject("""
                    {"tipo":"HUMANA","tipoDocumento":"DNI","numeroDocumento":"30111222","email":"ana@mail.com",
                     "nombre":"Ana","apellido":"Pérez","fechaNacimiento":"1990-05-10","genero":"NO_BINARIO",
                     "medioContactoPredeterminado":{"tipo":"WHATSAPP","valor":"1155667788"}}
                    """);

            assertThat(leido).isInstanceOf(PersonaHumanaRequestDTO.class);
            PersonaHumanaRequestDTO humana = (PersonaHumanaRequestDTO) leido;
            assertThat(humana.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 10));
            assertThat(humana.getGenero()).isEqualTo(Genero.NO_BINARIO);
            assertThat(humana.getMedioContactoPredeterminado()).isInstanceOf(WhatsappRequestDTO.class);
        }

        @Test
        @DisplayName("Una organización JURIDICA con su tipo de organización")
        void personaJuridica() throws Exception {
            PersonaDonanteRequestDTO leido = pedidoDeDonante.parseObject("""
                    {"tipo":"JURIDICA","tipoDocumento":"CUIT","numeroDocumento":"30-11122233-5",
                     "email":"info@fundacion.org","razonSocial":"Fundación","rubro":"Alimentos","tipoOrganizacion":"ONG"}
                    """);

            assertThat(leido).isInstanceOfSatisfying(PersonaJuridicaRequestDTO.class,
                    juridica -> assertThat(juridica.getTipoOrganizacion()).isEqualTo(TipoPersonaJuridica.ONG));
        }

        @Test
        @DisplayName("Una necesidad RECURRENTE con su periodicidad")
        void necesidadRecurrente() throws Exception {
            NecesidadRequestDTO leida = pedidoDeNecesidad.parseObject("""
                    {"tipo":"RECURRENTE","nombre":"Leche","subcategoria":"Lácteos","cantidadObjetivo":40,
                     "periodo":"MENSUAL"}
                    """);

            assertThat(leida).isInstanceOfSatisfying(NecesidadRecurrenteRequestDTO.class,
                    recurrente -> assertThat(recurrente.getPeriodo()).isEqualTo(Periodicidad.MENSUAL));
            assertThat(leida.getCantidadObjetivo()).isEqualTo(40);
        }

        @Test
        @DisplayName("Un tipo que no existe no se puede leer (el handler lo responde como 400)")
        void tipoDesconocido() {
            assertThatThrownBy(() -> pedidoDeDonante.parseObject("{\"tipo\":\"ROBOT\",\"email\":\"r@mail.com\"}"))
                    .isInstanceOf(InvalidTypeIdException.class);
        }
    }

    @Nested
    @DisplayName("Respuestas: el tipo sale en el JSON")
    class Respuestas {

        @Test
        @DisplayName("Una persona humana sale con tipo HUMANA y sus contactos con su propio tipo")
        void personaHumana() throws Exception {
            PersonaDonanteResponseDTO respuesta = PersonaHumanaResponseDTO.builder()
                    .id(UUID.randomUUID()).nombre("Ana").genero(Genero.FEMENINO).edad(36)
                    .contactos(List.of(EmailResponseDTO.builder().valor("ana@mail.com").build()))
                    .build();

            JsonContent<PersonaDonanteResponseDTO> json = donante.write(respuesta);

            assertThat(json).extractingJsonPathStringValue("$.tipo").isEqualTo("HUMANA");
            assertThat(json).extractingJsonPathStringValue("$.genero").isEqualTo("FEMENINO");
            assertThat(json).extractingJsonPathStringValue("$.contactos[0].tipo").isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("Una necesidad recurrente sale con tipo, cantidades enteras y la fecha como texto ISO")
        void necesidadRecurrente() throws Exception {
            NecesidadResponseDTO respuesta = NecesidadRecurrenteResponseDTO.builder()
                    .id(UUID.randomUUID()).nombre("Leche").cantidadObjetivo(40).cantidadRecibida(10)
                    .periodo(Periodicidad.MENSUAL).fechaInicioPeriodo(LocalDate.of(2026, 3, 1))
                    .build();

            JsonContent<NecesidadResponseDTO> json = necesidad.write(respuesta);

            assertThat(json).extractingJsonPathStringValue("$.tipo").isEqualTo("RECURRENTE");
            assertThat(json).extractingJsonPathStringValue("$.fechaInicioPeriodo").isEqualTo("2026-03-01");
            assertThat(json.getJson()).contains("\"cantidadObjetivo\":40", "\"cantidadRecibida\":10");
        }

        @Test
        @DisplayName("El error lleva la fecha y hora como texto ISO, no como arreglo")
        void error() throws Exception {
            ErrorResponseDTO respuesta = ErrorResponseDTO.builder()
                    .timestamp(LocalDateTime.of(2026, 10, 28, 9, 30)).status(404).error("Not Found").message("no está")
                    .build();

            assertThat(error.write(respuesta)).extractingJsonPathStringValue("$.timestamp")
                    .isEqualTo("2026-10-28T09:30:00");
        }
    }
}
