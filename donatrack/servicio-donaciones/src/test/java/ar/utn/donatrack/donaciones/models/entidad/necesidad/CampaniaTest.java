package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de Campania, el agrupador de necesidades de una entidad beneficiaria.
 *
 * El enunciado (Entrega 2) pide que una entidad pueda registrar varias necesidades
 * de una sola vez bajo una misma campaña, por ejemplo tras una inundación.
 * La campaña es la dueña de sus necesidades: sabe agregarlas, buscarlas,
 * eliminarlas y filtrarlas por compatibilidad con una subcategoría.
 */
@DisplayName("Campania - agrupador de necesidades de la entidad")
class CampaniaTest {

    private Campania campania;

    @BeforeEach
    void crearCampania() {
        campania = new Campania();
        campania.setIdCampania(UUID.randomUUID());
        campania.setIdEntidad(UUID.randomUUID());
        campania.setDescripcionGeneral("Colecta post inundación");
        campania.setFechaInicio(LocalDate.of(2026, 3, 1));
        campania.setFechaFin(LocalDate.of(2026, 4, 1));
    }

    /** Necesidad extraordinaria de ejemplo, con nombre y descripción configurables. */
    private NecesidadExtraordinaria necesidad(String nombre, String descripcion) {
        NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
        necesidad.setNombre(nombre);
        necesidad.setDescripcion(descripcion);
        necesidad.setCantidadObjetivo(100);
        return necesidad;
    }

    @Nested
    @DisplayName("agregarNecesidad()")
    class AgregarNecesidad {

        @Test
        @DisplayName("Una campaña recién creada arranca sin necesidades")
        void arrancaVacia() {
            assertThat(campania.getNecesidades()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Se pueden agregar varias necesidades a la misma campaña")
        void agregaVarias() {
            campania.agregarNecesidad(necesidad("Arroz", "Bolsas de arroz"));
            campania.agregarNecesidad(necesidad("Colchones", "Colchones de una plaza"));

            assertThat(campania.getNecesidades()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("buscarNecesidad() - lectura por id")
    class BuscarNecesidad {

        @Test
        @DisplayName("Devuelve la necesidad cuando el id existe")
        void encuentraLaNecesidad() {
            Necesidad buscada = necesidad("Arroz", "Bolsas de arroz");
            campania.agregarNecesidad(buscada);
            campania.agregarNecesidad(necesidad("Colchones", "Colchones de una plaza"));

            Optional<Necesidad> resultado = campania.buscarNecesidad(buscada.getId());

            assertThat(resultado).containsSame(buscada);
        }

        @Test
        @DisplayName("Devuelve Optional vacío cuando el id no pertenece a la campaña")
        void noEncuentraIdInexistente() {
            // Devolver Optional en lugar de null obliga al service a decidir
            // explícitamente qué hacer (en este caso, tirar 404).
            campania.agregarNecesidad(necesidad("Arroz", "Bolsas de arroz"));

            assertThat(campania.buscarNecesidad(UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    @DisplayName("eliminarNecesidad() - baja por id")
    class EliminarNecesidad {

        @Test
        @DisplayName("Elimina la necesidad y devuelve true cuando existía")
        void eliminaYDevuelveTrue() {
            Necesidad aEliminar = necesidad("Arroz", "Bolsas de arroz");
            campania.agregarNecesidad(aEliminar);
            campania.agregarNecesidad(necesidad("Colchones", "Colchones de una plaza"));

            boolean eliminada = campania.eliminarNecesidad(aEliminar.getId());

            assertThat(eliminada).isTrue();
            assertThat(campania.getNecesidades()).hasSize(1);
            assertThat(campania.buscarNecesidad(aEliminar.getId())).isEmpty();
        }

        @Test
        @DisplayName("Devuelve false y no toca nada cuando el id no existe")
        void devuelveFalseSiNoExiste() {
            // El boolean es el que permite al controller responder 404 en vez de
            // un 204 mentiroso cuando el id no existía.
            campania.agregarNecesidad(necesidad("Arroz", "Bolsas de arroz"));

            boolean eliminada = campania.eliminarNecesidad(UUID.randomUUID());

            assertThat(eliminada).isFalse();
            assertThat(campania.getNecesidades()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("necesidadesAbiertasCompatiblesCon() - filtro usado por el algoritmo de asignación")
    class NecesidadesCompatibles {

        /** Un día dentro de la campaña (1 de marzo a 1 de abril de 2026). */
        private final LocalDate durante = LocalDate.of(2026, 3, 15);

        @BeforeEach
        void cargarNecesidades() {
            campania.agregarNecesidad(necesidad("Arroz para el comedor", "Bolsas de arroz de 1kg"));
            campania.agregarNecesidad(necesidad("Fideos", "Paquetes de fideos secos"));
            campania.agregarNecesidad(necesidad("Colchones", "Colchones de una plaza"));
        }

        @Test
        @DisplayName("Devuelve solo las necesidades que mencionan la subcategoría")
        void filtraPorSubcategoria() {
            assertThat(campania.necesidadesAbiertasCompatiblesCon(new Subcategoria("arroz"), durante))
                    .hasSize(1)
                    .allMatch(n -> n.getNombre().contains("Arroz"));
        }

        @Test
        @DisplayName("Devuelve lista vacía si ninguna necesidad coincide")
        void sinCoincidencias() {
            assertThat(campania.necesidadesAbiertasCompatiblesCon(new Subcategoria("bicicletas"), durante)).isEmpty();
        }

        @Test
        @DisplayName("El filtro ignora mayúsculas y minúsculas")
        void filtroIgnoraCase() {
            assertThat(campania.necesidadesAbiertasCompatiblesCon(new Subcategoria("COLCHONES"), durante)).hasSize(1);
        }

        @Test
        @DisplayName("Las necesidades ya satisfechas no cuentan")
        void satisfechasAfuera() {
            campania.getNecesidades().getFirst().recibir(100, durante);

            assertThat(campania.necesidadesAbiertasCompatiblesCon(new Subcategoria("arroz"), durante)).isEmpty();
        }

        @Test
        @DisplayName("Fuera de las fechas de la campaña no hay necesidades abiertas")
        void campaniaNoVigente() {
            assertThat(campania.estaVigenteAl(LocalDate.of(2026, 4, 1))).isTrue();
            assertThat(campania.estaVigenteAl(LocalDate.of(2026, 4, 2))).isFalse();
            assertThat(campania.necesidadesAbiertasCompatiblesCon(new Subcategoria("arroz"), LocalDate.of(2026, 5, 1)))
                    .isEmpty();
            assertThat(campania.necesidadesAbiertasCompatiblesCon(new Subcategoria("arroz"), LocalDate.of(2026, 2, 28)))
                    .isEmpty();
        }

        @Test
        @DisplayName("Una campaña sin fechas no está acotada")
        void sinFechas() {
            campania.setFechaInicio(null);
            campania.setFechaFin(null);

            assertThat(campania.estaVigenteAl(LocalDate.of(2030, 1, 1))).isTrue();
        }
    }
}
