package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la factory que convierte una fila del CSV en un objeto de dominio.
 *
 * El CSV es deliberadamente pobre: solo trae tipo de persona, documento, nombre,
 * email y teléfono. La factory completa el resto con valores neutros para que el
 * donante quede consistente, y un administrador termina de cargarlo después
 * desde la interfaz web.
 *
 * Dos decisiones que se prueban acá:
 *   - Para una persona humana, el nombre completo se parte en nombre y apellido
 *     por el PRIMER espacio (el resto queda como apellido compuesto).
 *   - Todo donante importado nace ACTIVO y con una interacción ya registrada,
 *     así no lo toma la tarea de inactividad al día siguiente.
 */
@DisplayName("DonanteFactory - construcción del donante desde una fila del CSV")
class DonanteFactoryTest {

    private DonanteFactory factory;

    @BeforeEach
    void crearFactory() {
        factory = new DonanteFactory();
    }

    private DonanteImportDto dto(String tipoPersona, String tipoDoc, String documento, String nombre, String email) {
        return new DonanteImportDto(2, tipoPersona, tipoDoc, documento, nombre, email, null);
    }

    @Nested
    @DisplayName("Persona humana")
    class Humana {

        @Test
        @DisplayName("Crea una PersonaHumana con documento y email del CSV")
        void creaPersonaHumana() {
            PersonaDonante persona = factory.crearPersona(
                    dto("HUMANA", "DNI", "30111222", "Juan Pérez", "juan@example.com"));

            assertThat(persona).isInstanceOf(PersonaHumana.class);
            assertThat(persona.getTipoDocumento()).isEqualTo("DNI");
            assertThat(persona.getNumeroDocumento()).isEqualTo("30111222");
            assertThat(persona.getEmail()).isEqualTo("juan@example.com");
        }

        @Test
        @DisplayName("Separa nombre y apellido por el primer espacio")
        void separaNombreYApellido() {
            PersonaHumana persona = (PersonaHumana) factory.crearPersona(
                    dto("HUMANA", "DNI", "30111222", "Juan Pérez", "juan@example.com"));

            assertThat(persona.getNombre()).isEqualTo("Juan");
            assertThat(persona.getApellido()).isEqualTo("Pérez");
        }

        @Test
        @DisplayName("Un apellido compuesto queda entero: solo se parte por el PRIMER espacio")
        void apellidoCompuesto() {
            // "María Fernanda López Gómez" -> nombre "María", apellido "Fernanda López Gómez".
            // No es perfecto, pero es determinístico y el administrador puede corregirlo.
            PersonaHumana persona = (PersonaHumana) factory.crearPersona(
                    dto("HUMANA", "DNI", "30111222", "María Fernanda López Gómez", "maria@example.com"));

            assertThat(persona.getNombre()).isEqualTo("María");
            assertThat(persona.getApellido()).isEqualTo("Fernanda López Gómez");
        }

        @Test
        @DisplayName("Un nombre de una sola palabra deja el apellido como guion")
        void nombreSinApellido() {
            // Caso borde: el CSV traía solo un nombre. Se usa "-" como marcador
            // de dato faltante en lugar de dejar null.
            PersonaHumana persona = (PersonaHumana) factory.crearPersona(
                    dto("HUMANA", "DNI", "30111222", "Juan", "juan@example.com"));

            assertThat(persona.getNombre()).isEqualTo("Juan");
            assertThat(persona.getApellido()).isEqualTo("-");
        }

        @Test
        @DisplayName("Un tipo de persona desconocido cae por defecto en PersonaHumana")
        void tipoDesconocidoCaeEnHumana() {
            // La factory solo distingue JURIDICA; cualquier otro valor es humana.
            // En la práctica el parser ya filtró los tipos inválidos antes de llegar acá.
            assertThat(factory.crearPersona(dto("OTRO", "DNI", "30111222", "Juan Pérez", "juan@example.com")))
                    .isInstanceOf(PersonaHumana.class);
        }
    }

    @Nested
    @DisplayName("Persona jurídica")
    class Juridica {

        @Test
        @DisplayName("Crea una PersonaJuridica usando el nombre del CSV como razón social")
        void creaPersonaJuridica() {
            PersonaDonante persona = factory.crearPersona(
                    dto("JURIDICA", "CUIT", "30-11122233-4", "Fundación Ejemplo", "contacto@fundacion.org"));

            assertThat(persona).isInstanceOf(PersonaJuridica.class);
            assertThat(((PersonaJuridica) persona).getRazonSocial()).isEqualTo("Fundación Ejemplo");
            assertThat(persona.getTipoDocumento()).isEqualTo("CUIT");
            assertThat(persona.getNumeroDocumento()).isEqualTo("30-11122233-4");
        }

        @Test
        @DisplayName("La razón social NO se parte por espacios, a diferencia del nombre de una persona humana")
        void razonSocialNoSeParte() {
            PersonaJuridica persona = (PersonaJuridica) factory.crearPersona(
                    dto("JURIDICA", "CUIT", "30-11122233-4", "Asociación Civil Manos Unidas", "info@manos.org"));

            assertThat(persona.getRazonSocial()).isEqualTo("Asociación Civil Manos Unidas");
        }

        @Test
        @DisplayName("Arranca sin representantes cargados (lista vacía, no null)")
        void arrancaSinRepresentantes() {
            PersonaJuridica persona = (PersonaJuridica) factory.crearPersona(
                    dto("JURIDICA", "CUIT", "30-11122233-4", "Fundación Ejemplo", "contacto@fundacion.org"));

            assertThat(persona.getRepresentantes()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Estado inicial común a todo donante importado")
    class EstadoInicial {

        @Test
        @DisplayName("Todo donante importado nace ACTIVO")
        void naceActivo() {
            PersonaDonante persona = factory.crearPersona(
                    dto("HUMANA", "DNI", "30111222", "Juan Pérez", "juan@example.com"));

            assertThat(persona.getEstado()).isInstanceOf(ActivoState.class);
        }

        @Test
        @DisplayName("Todo donante importado queda con una interacción inicial registrada")
        void tieneInteraccionInicial() {
            // Sin esto, la tarea diaria de inactividad los detectaría como
            // inactivos apenas se importan y les mandaría un recordatorio absurdo.
            PersonaDonante persona = factory.crearPersona(
                    dto("JURIDICA", "CUIT", "30-11122233-4", "Fundación Ejemplo", "contacto@fundacion.org"));

            assertThat(persona.getUltimaInteraccion()).isNotNull();
        }
    }
}
