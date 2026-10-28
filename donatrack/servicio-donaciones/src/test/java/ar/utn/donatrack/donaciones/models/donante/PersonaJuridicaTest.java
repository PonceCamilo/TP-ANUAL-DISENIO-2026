package ar.utn.donatrack.donaciones.models.donante;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de PersonaJuridica, en particular del alta/actualización de representantes.
 *
 * El enunciado permite que una organización donante designe representantes
 * habilitados para operar en su nombre. La regla de negocio es que el email
 * identifica al representante: volver a agregar a alguien con el mismo email
 * actualiza sus datos en lugar de duplicarlo en la lista.
 */
@DisplayName("PersonaJuridica - gestión de representantes")
class PersonaJuridicaTest {

    private PersonaJuridica organizacion;

    @BeforeEach
    void crearOrganizacion() {
        organizacion = PersonaJuridica.builder()
                .id(UUID.randomUUID())
                .razonSocial("Fundación Ejemplo")
                .rubro("Alimentos")
                .documento(Documento.de("CUIT", "30-11122233-5"))
                .email("contacto@fundacionejemplo.org")
                .build();
    }

    private Representante representante(String nombre, String email) {
        return Representante.builder()
                .nombre(nombre)
                .apellido("Gómez")
                .email(email)
                .build();
    }

    @Nested
    @DisplayName("agregarRepresentante()")
    class AgregarRepresentante {

        @Test
        @DisplayName("Una organización recién creada arranca sin representantes (lista vacía, no null)")
        void arrancaSinRepresentantes() {
            assertThat(organizacion.getRepresentantes()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Agregar dos representantes con emails distintos suma ambos")
        void emailsDistintosSeSuman() {
            organizacion.agregarRepresentante(representante("Ana", "ana@fundacion.org"));
            organizacion.agregarRepresentante(representante("Beto", "beto@fundacion.org"));

            assertThat(organizacion.getRepresentantes())
                    .hasSize(2)
                    .extracting(Representante::getNombre)
                    .containsExactly("Ana", "Beto");
        }

        @Test
        @DisplayName("Agregar un representante con un email ya registrado REEMPLAZA al anterior")
        void mismoEmailReemplaza() {
            // El email es la identidad del representante: si vuelve a llegar el mismo
            // email con datos nuevos, se actualiza en vez de duplicarse.
            organizacion.agregarRepresentante(representante("Ana", "ana@fundacion.org"));
            organizacion.agregarRepresentante(representante("Ana María", "ana@fundacion.org"));

            assertThat(organizacion.getRepresentantes()).hasSize(1);
            assertThat(organizacion.getRepresentantes().getFirst().getNombre()).isEqualTo("Ana María");
        }

        @Test
        @DisplayName("La comparación de emails ignora mayúsculas y minúsculas")
        void comparacionDeEmailIgnoraCase() {
            // Los emails no distinguen case en la práctica; si no lo contemplamos,
            // "Ana@..." y "ana@..." quedarían como dos representantes distintos.
            organizacion.agregarRepresentante(representante("Ana", "ana@fundacion.org"));
            organizacion.agregarRepresentante(representante("Ana María", "ANA@FUNDACION.ORG"));

            assertThat(organizacion.getRepresentantes()).hasSize(1);
            assertThat(organizacion.getRepresentantes().getFirst().getNombre()).isEqualTo("Ana María");
        }

        @Test
        @DisplayName("Un representante sin email no desplaza a los ya cargados")
        void representanteSinEmailNoDesplaza() {
            // Caso borde: si el email es null no puede usarse como identidad,
            // así que el representante simplemente se agrega al final.
            organizacion.agregarRepresentante(representante("Ana", "ana@fundacion.org"));
            organizacion.agregarRepresentante(representante("Sin Email", null));

            assertThat(organizacion.getRepresentantes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Datos propios de la persona jurídica")
    class DatosPropios {

        @Test
        @DisplayName("Conserva razón social y rubro, y hereda el comportamiento de PersonaDonante")
        void heredaDePersonaDonante() {
            assertThat(organizacion.getRazonSocial()).isEqualTo("Fundación Ejemplo");
            assertThat(organizacion.getRubro()).isEqualTo("Alimentos");
            // obtenerEmail() viene de la clase padre PersonaDonante.
            assertThat(organizacion.obtenerEmail()).isEqualTo("contacto@fundacionejemplo.org");
        }

        @Test
        @DisplayName("Una persona jurídica también puede cambiar de estado")
        void tambienCambiaDeEstado() {
            organizacion.cambiarEstado("INACTIVO", null);

            assertThat(organizacion.getEstado().nombre()).isEqualTo("INACTIVO");
        }
    }
}
