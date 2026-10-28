package ar.utn.donatrack.donaciones.models.entidad;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de EntidadBeneficiaria (modelo rico).
 *
 * La entidad es la dueña de sus campañas: sabe agregarlas, contar cuántas
 * necesidades compatibles tiene con una subcategoría dada (la entrada del
 * algoritmo de asignación "por afinidad") e imputarles lo que recibe.
 *
 * También elige el contacto por el que se le avisa de asignaciones y entregas:
 * su email si lo tiene, o su primer contacto.
 */
@DisplayName("EntidadBeneficiaria - comportamiento del modelo rico")
class EntidadBeneficiariaTest {

    private EntidadBeneficiaria entidad;

    @BeforeEach
    void crearEntidad() {
        entidad = EntidadBeneficiaria.builder()
                .id(UUID.randomUUID())
                .razonSocial("Comedor Los Pibes")
                .contactos(new ArrayList<>())
                .build();
    }

    /** Campaña vacía con id propio, lista para recibir necesidades. */
    private Campania campaniaVacia(String descripcion) {
        Campania campania = new Campania();
        campania.setIdCampania(UUID.randomUUID());
        campania.setIdEntidad(entidad.getId());
        campania.setDescripcionGeneral(descripcion);
        campania.setFechaInicio(LocalDate.of(2026, 3, 1));
        campania.setFechaFin(LocalDate.of(2026, 4, 1));
        return campania;
    }

    private Necesidad necesidad(String nombre, String descripcion) {
        NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
        necesidad.setNombre(nombre);
        necesidad.setDescripcion(descripcion);
        necesidad.setCantidadObjetivo(100);
        return necesidad;
    }

    @Nested
    @DisplayName("Gestión de campañas")
    class GestionDeCampanias {

        @Test
        @DisplayName("Una entidad recién creada arranca sin campañas (lista vacía, no null)")
        void arrancaSinCampanias() {
            assertThat(entidad.getCampanias()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("agregarCampania() suma la campaña a la entidad")
        void agregarCampania() {
            entidad.agregarCampania(campaniaVacia("Colecta post inundación"));

            assertThat(entidad.getCampanias()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("contarNecesidadesCompatiblesCon() - insumo del algoritmo de asignación")
    class ContarNecesidadesCompatibles {

        /** Un día dentro de las campañas de prueba (1 de marzo a 1 de abril de 2026). */
        private final LocalDate durante = LocalDate.of(2026, 3, 15);

        @Test
        @DisplayName("Cuenta las necesidades compatibles a lo largo de TODAS las campañas")
        void cuentaEnTodasLasCampanias() {
            // El puntaje de afinidad de una entidad no mira una sola campaña:
            // suma todas las necesidades abiertas que matcheen la subcategoría.
            Campania primera = campaniaVacia("Colecta post inundación");
            Campania segunda = campaniaVacia("Campaña permanente");
            entidad.agregarCampania(primera);
            entidad.agregarCampania(segunda);

            primera.agregarNecesidad(necesidad("Arroz", "Bolsas de arroz"));
            primera.agregarNecesidad(necesidad("Colchones", "Colchones de una plaza"));
            segunda.agregarNecesidad(necesidad("Más arroz", "Arroz para el guiso"));

            assertThat(entidad.contarNecesidadesCompatiblesCon(new Subcategoria("arroz"), durante)).isEqualTo(2);
        }

        @Test
        @DisplayName("Devuelve 0 cuando ninguna necesidad coincide")
        void ceroSiNoHayCoincidencias() {
            Campania campania = campaniaVacia("Colecta post inundación");
            entidad.agregarCampania(campania);
            campania.agregarNecesidad(necesidad("Colchones", "Colchones de una plaza"));

            assertThat(entidad.contarNecesidadesCompatiblesCon(new Subcategoria("arroz"), durante)).isZero();
        }

        @Test
        @DisplayName("Devuelve 0 cuando la entidad no tiene ninguna campaña")
        void ceroSinCampanias() {
            assertThat(entidad.contarNecesidadesCompatiblesCon(new Subcategoria("arroz"), durante)).isZero();
        }

        @Test
        @DisplayName("No cuenta las campañas que no están vigentes a la fecha")
        void noCuentaCampaniasTerminadas() {
            Campania campania = campaniaVacia("Colecta post inundación");
            entidad.agregarCampania(campania);
            campania.agregarNecesidad(necesidad("Arroz", "Bolsas de arroz"));

            assertThat(entidad.contarNecesidadesCompatiblesCon(new Subcategoria("arroz"), LocalDate.of(2026, 10, 1)))
                    .isZero();
        }

        @Test
        @DisplayName("El conteo ignora mayúsculas y minúsculas")
        void conteoIgnoraCase() {
            Campania campania = campaniaVacia("Colecta post inundación");
            entidad.agregarCampania(campania);
            campania.agregarNecesidad(necesidad("Arroz", "Bolsas de arroz"));

            assertThat(entidad.contarNecesidadesCompatiblesCon(new Subcategoria("ARROZ"), durante)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("recibirDonacion() - lo entregado se imputa a las necesidades")
    class RecibirDonacion {

        private final LocalDate durante = LocalDate.of(2026, 3, 15);
        private Necesidad arroz;
        private Necesidad masArroz;

        @BeforeEach
        void cargar() {
            Campania campania = campaniaVacia("Colecta post inundación");
            entidad.agregarCampania(campania);
            arroz = necesidad("Arroz", "Bolsas de arroz");             // objetivo 100
            masArroz = necesidad("Más arroz", "Arroz para el guiso");  // objetivo 100
            campania.agregarNecesidad(arroz);
            campania.agregarNecesidad(necesidad("Colchones", "Colchones de una plaza"));
            campania.agregarNecesidad(masArroz);
        }

        @Test
        @DisplayName("Llena las necesidades compatibles en orden, cada una hasta su objetivo")
        void llenaEnOrden() {
            int imputado = entidad.recibirDonacion(new Subcategoria("arroz"), 130, durante);

            assertThat(imputado).isEqualTo(130);
            assertThat(arroz.getCantidadRecibida()).isEqualTo(100);
            assertThat(masArroz.getCantidadRecibida()).isEqualTo(30);
        }

        @Test
        @DisplayName("Si sobra después de cubrir todas, el excedente queda en la última")
        void excedenteEnLaUltima() {
            entidad.recibirDonacion(new Subcategoria("arroz"), 250, durante);

            assertThat(arroz.getCantidadRecibida()).isEqualTo(100);
            assertThat(masArroz.getCantidadRecibida()).isEqualTo(150);
        }

        @Test
        @DisplayName("Las satisfechas no reciben más: va a la siguiente abierta")
        void saltaLasSatisfechas() {
            arroz.recibir(100, durante);

            entidad.recibirDonacion(new Subcategoria("arroz"), 40, durante);

            assertThat(arroz.getCantidadRecibida()).isEqualTo(100);
            assertThat(masArroz.getCantidadRecibida()).isEqualTo(40);
        }

        @Test
        @DisplayName("Sin necesidades abiertas compatibles no se imputa nada")
        void sinCompatibles() {
            assertThat(entidad.recibirDonacion(new Subcategoria("bicicletas"), 10, durante)).isZero();
            assertThat(entidad.recibirDonacion(new Subcategoria("arroz"), 10, LocalDate.of(2026, 10, 1))).isZero();
            assertThat(arroz.getCantidadRecibida()).isZero();
        }
    }

    @Nested
    @DisplayName("contactoParaNotificar() - canal para avisarle a la entidad")
    class ContactoParaNotificar {

        @Test
        @DisplayName("Prefiere su email aunque no sea el primer contacto")
        void prefiereElEmail() {
            entidad.reemplazarContactos(List.of(
                    Telefono.builder().valor("1155667788").build(),
                    Email.builder().valor("comedor@lospibes.org").build()
            ));

            assertThat(entidad.contactoParaNotificar()).map(MedioDeContacto::getValor).contains("comedor@lospibes.org");
        }

        @Test
        @DisplayName("Sin email, usa su primer contacto")
        void sinEmailUsaElPrimero() {
            entidad.reemplazarContactos(List.of(Telefono.builder().valor("1155667788").build()));

            assertThat(entidad.contactoParaNotificar()).map(MedioDeContacto::getValor).contains("1155667788");
        }

        @Test
        @DisplayName("Sin contactos (o con la lista en null) no hay a quién avisar")
        void sinContactos() {
            assertThat(entidad.contactoParaNotificar()).isEmpty();

            entidad = EntidadBeneficiaria.builder().id(UUID.randomUUID()).razonSocial("Comedor").contactos(null).build();

            assertThat(entidad.contactoParaNotificar()).isEmpty();
        }

        @Test
        @DisplayName("Si hay varios emails usa el primero")
        void variosEmailsUsaElPrimero() {
            entidad.reemplazarContactos(List.of(
                    Email.builder().valor("principal@lospibes.org").build(),
                    Email.builder().valor("secundario@lospibes.org").build()
            ));

            assertThat(entidad.contactoParaNotificar()).map(MedioDeContacto::getValor).contains("principal@lospibes.org");
        }
    }
}
