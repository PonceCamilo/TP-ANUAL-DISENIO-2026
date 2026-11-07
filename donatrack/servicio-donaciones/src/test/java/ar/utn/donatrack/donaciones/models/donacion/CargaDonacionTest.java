package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la segmentación automática de una carga de donación.
 *
 * Es una de las reglas centrales del enunciado (Entrega 1) y uno de los puntos
 * que la cátedra marcó para revisar en Entrega 3: "que la segmentación sea una
 * responsabilidad de la donación o una fachada". Por eso vive en CargaDonacion
 * (el modelo) y no en el service.
 *
 * Reglas que se verifican:
 *   1. Se agrupa obligatoriamente por subcategoría (unidad mínima de asignación).
 *   2. Dentro de una subcategoría, los perecederos se separan por fecha de vencimiento.
 *   3. Dentro de una subcategoría, los bienes con estado se separan por nuevo/usado.
 *   4. Los bienes genéricos de una misma subcategoría quedan juntos en una sola donación.
 */
@DisplayName("CargaDonacion.segmentar() - segmentación automática de la carga")
class CargaDonacionTest {

    private static final UUID ID_DONANTE = UUID.randomUUID();
    private static final String DESCRIPCION = "Donación de prueba";

    /** Atajo para construir un bien perecedero con su fecha de vencimiento. */
    private BienPerecible perecible(String subcategoria, String descripcion, LocalDate vencimiento) {
        return BienPerecible.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion(descripcion)
                .cantidad(1)
                .unidad("kg")
                .fechaVencimiento(vencimiento)
                .build();
    }

    /** Atajo para construir un bien cuyo estado (nuevo/usado) es relevante. */
    private BienConEstado conEstado(String subcategoria, String descripcion, boolean esNuevo) {
        return BienConEstado.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion(descripcion)
                .cantidad(1)
                .unidad("unidades")
                .esNuevo(esNuevo)
                .build();
    }

    /** Atajo para construir un bien genérico (ni perecedero ni con estado). */
    private BienGenerico generico(String subcategoria, String descripcion) {
        return BienGenerico.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion(descripcion)
                .cantidad(1)
                .unidad("unidades")
                .build();
    }

    private CargaDonacion cargaCon(Bien... bienes) {
        return new CargaDonacion(ID_DONANTE, DESCRIPCION, List.of(bienes));
    }

    @Nested
    @DisplayName("Agrupación por subcategoría")
    class AgrupacionPorSubcategoria {

        @Test
        @DisplayName("Bienes de subcategorías distintas generan donaciones independientes")
        void subcategoriasDistintasSeSeparan() {
            // El enunciado exige que cada donación resultante quede asociada a una
            // única subcategoría, porque es la unidad mínima de asignación.
            CargaDonacion carga = cargaCon(
                    generico("arroz", "Arroz largo fino"),
                    generico("ropa", "Camperas de abrigo")
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(2);
            assertThat(donaciones)
                    .extracting(d -> d.getSubcategoria().getTipo())
                    .containsExactlyInAnyOrder("arroz", "ropa");
        }

        @Test
        @DisplayName("Bienes genéricos de la MISMA subcategoría quedan en una sola donación")
        void genericosDeMismaSubcategoriaSeAgrupan() {
            CargaDonacion carga = cargaCon(
                    generico("ropa", "Camperas"),
                    generico("ropa", "Pantalones"),
                    generico("ropa", "Remeras")
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(1);
            assertThat(donaciones.getFirst().getBienes()).hasSize(3);
        }

        @Test
        @DisplayName("La subcategoría es case-insensitive: 'Arroz' y 'arroz' se agrupan juntas")
        void subcategoriaEsCaseInsensitive() {
            // Subcategoria normaliza el tipo (trim + lowercase) y su equals se basa
            // en esa forma normalizada, justamente para no duplicar donaciones por
            // diferencias de tipeo.
            CargaDonacion carga = cargaCon(
                    generico("Arroz", "Arroz marca A"),
                    generico("arroz", "Arroz marca B"),
                    generico("  ARROZ  ", "Arroz marca C")
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(1);
            assertThat(donaciones.getFirst().getBienes()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Separación de perecederos por fecha de vencimiento")
    class PerecederosPorVencimiento {

        @Test
        @DisplayName("Dos perecederos de la misma subcategoría con vencimientos distintos generan 2 donaciones")
        void vencimientosDistintosSeSeparan() {
            // Regla del enunciado: "el sistema podrá generar donaciones separadas
            // cuando existan diferencias en la fecha de vencimiento".
            CargaDonacion carga = cargaCon(
                    perecible("arroz", "Arroz largo fino", LocalDate.of(2027, 1, 1)),
                    perecible("arroz", "Arroz integral", LocalDate.of(2027, 6, 1))
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(2);
            // Ambas comparten subcategoría, lo que las separa es el vencimiento.
            assertThat(donaciones)
                    .allMatch(d -> d.getSubcategoria().getTipo().equals("arroz"));
        }

        @Test
        @DisplayName("Dos perecederos con el MISMO vencimiento quedan en una sola donación")
        void mismoVencimientoSeAgrupa() {
            LocalDate mismoVencimiento = LocalDate.of(2027, 1, 1);

            CargaDonacion carga = cargaCon(
                    perecible("fideos", "Fideos tipo 1", mismoVencimiento),
                    perecible("fideos", "Fideos tipo 2", mismoVencimiento)
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(1);
            assertThat(donaciones.getFirst().getBienes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Separación de bienes con estado por nuevo/usado")
    class BienesConEstado {

        @Test
        @DisplayName("Un bien nuevo y uno usado de la misma subcategoría generan 2 donaciones")
        void nuevoYUsadoSeSeparan() {
            // El enunciado pide consignar si el artículo es nuevo o usado "a fin de
            // asegurar una correcta evaluación y asignación posterior", por lo que
            // no pueden mezclarse en la misma donación.
            CargaDonacion carga = cargaCon(
                    conEstado("sillas", "Sillas de oficina nuevas", true),
                    conEstado("sillas", "Sillas de oficina usadas", false)
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(2);
        }

        @Test
        @DisplayName("Dos bienes usados de la misma subcategoría quedan en una sola donación")
        void mismoEstadoSeAgrupa() {
            CargaDonacion carga = cargaCon(
                    conEstado("sillas", "Seis sillas", false),
                    conEstado("sillas", "Mesa rectangular", false)
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(1);
            assertThat(donaciones.getFirst().getBienes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Casos combinados y datos que se propagan")
    class CasosCombinados {

        @Test
        @DisplayName("Los tres tipos de bien conviven: cada criterio segmenta por separado")
        void escenarioCompleto() {
            // Escenario integral: 2 arroces perecederos con vencimientos distintos,
            // 1 silla usada y 1 prenda genérica. Se esperan 4 donaciones:
            //   arroz(2027-01-01), arroz(2027-06-01), sillas(usadas), ropa(genérica)
            CargaDonacion carga = cargaCon(
                    perecible("arroz", "Arroz largo fino", LocalDate.of(2027, 1, 1)),
                    perecible("arroz", "Arroz integral", LocalDate.of(2027, 6, 1)),
                    conEstado("sillas", "Sillas usadas", false),
                    generico("ropa", "Camperas")
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).hasSize(4);
            assertThat(donaciones)
                    .extracting(d -> d.getSubcategoria().getTipo())
                    .containsExactlyInAnyOrder("arroz", "arroz", "sillas", "ropa");
        }

        @Test
        @DisplayName("Cada donación resultante hereda el donante y la descripción de la carga")
        void datosDeLaCargaSePropagan() {
            CargaDonacion carga = cargaCon(
                    generico("arroz", "Arroz"),
                    generico("ropa", "Camperas")
            );

            List<Donacion> donaciones = carga.segmentar();

            assertThat(donaciones).allSatisfy(donacion -> {
                assertThat(donacion.getIdDonante()).isEqualTo(ID_DONANTE);
                assertThat(donacion.getDescripcion()).isEqualTo(DESCRIPCION);
            });
        }

        @Test
        @DisplayName("Toda donación segmentada nace EN_DEPOSITO y sin entidad asignada")
        void donacionesNacenEnDeposito() {
            // El enunciado indica que al registrarse la donación queda "En depósito",
            // disponible para ser asignada.
            List<Donacion> donaciones = cargaCon(generico("arroz", "Arroz")).segmentar();

            assertThat(donaciones.getFirst().estaEnEstado("EN_DEPOSITO")).isTrue();
            assertThat(donaciones.getFirst().getIdEntidadBeneficiaria()).isNull();
            assertThat(donaciones.getFirst().getHistorialEstados()).isEmpty();
        }

        @Test
        @DisplayName("Cada donación segmentada recibe un id propio")
        void cadaDonacionTieneIdPropio() {
            List<Donacion> donaciones = cargaCon(
                    generico("arroz", "Arroz"),
                    generico("ropa", "Camperas")
            ).segmentar();

            assertThat(donaciones).extracting(Donacion::getId).doesNotContainNull();
            assertThat(donaciones.get(0).getId()).isNotEqualTo(donaciones.get(1).getId());
        }

        @Test
        @DisplayName("Una carga sin bienes no genera ninguna donación")
        void cargaVaciaNoGeneraDonaciones() {
            CargaDonacion carga = new CargaDonacion(ID_DONANTE, DESCRIPCION, List.of());

            assertThat(carga.segmentar()).isEmpty();
        }
    }
}
