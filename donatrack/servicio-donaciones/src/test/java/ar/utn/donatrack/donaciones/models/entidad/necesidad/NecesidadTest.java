package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la jerarquía de Necesidades de una entidad beneficiaria.
 *
 * El enunciado distingue dos tipos:
 *   - NecesidadExtraordinaria: surge por una situación puntual (inundación,
 *     mudanza). Se satisface cuando se junta la cantidad objetivo. No vence.
 *   - NecesidadRecurrente: forma parte del funcionamiento habitual (ej: 100
 *     paquetes de fideos por semana). Se satisface DENTRO de cada período y
 *     lo recibido deja de contar cuando el período vence.
 *
 * Además se prueba esCompatibleCon(), que es la base del algoritmo de asignación:
 * decide si una donación de cierta subcategoría sirve para esta necesidad.
 */
@DisplayName("Necesidad - necesidades extraordinarias y recurrentes")
class NecesidadTest {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 4);

    private static Subcategoria sub(String tipo) {
        return new Subcategoria(tipo);
    }

    @Nested
    @DisplayName("recibir() - acumulación de lo recibido")
    class Recibir {

        @Test
        @DisplayName("Recibir una donación suma a la cantidad ya recibida")
        void sumaLaCantidad() {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            necesidad.setCantidadObjetivo(100);

            necesidad.recibir(30, HOY);

            assertThat(necesidad.getCantidadRecibida()).isEqualTo(30);
            assertThat(necesidad.faltanteAl(HOY)).isEqualTo(70);
        }

        @Test
        @DisplayName("Varias donaciones se acumulan")
        void acumulaVariasDonaciones() {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            necesidad.setCantidadObjetivo(100);

            necesidad.recibir(30, HOY);
            necesidad.recibir(45, HOY);
            necesidad.recibir(10, HOY);

            assertThat(necesidad.getCantidadRecibida()).isEqualTo(85);
        }

        @Test
        @DisplayName("Lo que falta nunca es negativo")
        void faltanteNoNegativo() {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            necesidad.setCantidadObjetivo(100);

            necesidad.recibir(150, HOY);

            assertThat(necesidad.faltanteAl(HOY)).isZero();
        }
    }

    @Nested
    @DisplayName("esCompatibleCon() - matching entre donación y necesidad")
    class EsCompatibleCon {

        /** Necesidad de ejemplo sin subcategoría: el matching mira nombre y descripción. */
        private NecesidadExtraordinaria necesidadDeArroz() {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            necesidad.setNombre("Arroz para el comedor");
            necesidad.setDescripcion("Necesitamos bolsas de fideos y arroz");
            necesidad.setCantidadObjetivo(100);
            return necesidad;
        }

        @Test
        @DisplayName("Sin subcategoría declarada, es compatible si aparece en el NOMBRE")
        void matchPorNombre() {
            assertThat(necesidadDeArroz().esCompatibleCon(sub("arroz"))).isTrue();
        }

        @Test
        @DisplayName("Sin subcategoría declarada, es compatible si aparece en la DESCRIPCIÓN")
        void matchPorDescripcion() {
            // "fideos" no está en el nombre pero sí en la descripción.
            assertThat(necesidadDeArroz().esCompatibleCon(sub("fideos"))).isTrue();
        }

        @Test
        @DisplayName("El matching ignora mayúsculas, acentos y espacios de más")
        void matchNormalizado() {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            necesidad.setNombre("Ropa de abrigo para el invierno");

            assertThat(necesidadDeArroz().esCompatibleCon(sub("ARROZ"))).isTrue();
            assertThat(necesidad.esCompatibleCon(sub("  Rópa   DE abrigo "))).isTrue();
        }

        @Test
        @DisplayName("NO es compatible con una subcategoría que no aparece en ningún texto")
        void sinMatch() {
            assertThat(necesidadDeArroz().esCompatibleCon(sub("ropa"))).isFalse();
        }

        @Test
        @DisplayName("Con subcategoría declarada, compara contra ella y no contra los textos")
        void matchPorSubcategoriaDeclarada() {
            // El enunciado pide que la necesidad indique su subcategoría: si la
            // tiene, es la que manda (y "arroz" en la descripción ya no alcanza).
            NecesidadExtraordinaria necesidad = necesidadDeArroz();
            necesidad.setSubcategoria(sub("Fideos"));

            assertThat(necesidad.esCompatibleCon(sub("fideos"))).isTrue();
            assertThat(necesidad.esCompatibleCon(sub("arroz"))).isFalse();
        }

        @Test
        @DisplayName("Una subcategoría nula o en blanco nunca es compatible")
        void subcategoriaNulaOEnBlanco() {
            // Caso borde: si aceptáramos el blanco, contains("") daría true y toda
            // necesidad sería compatible con cualquier donación.
            assertThat(necesidadDeArroz().esCompatibleCon(null)).isFalse();
            assertThat(necesidadDeArroz().esCompatibleCon(sub("   "))).isFalse();
        }

        @Test
        @DisplayName("Una necesidad sin nombre ni descripción no es compatible con nada")
        void necesidadSinTextos() {
            NecesidadExtraordinaria vacia = new NecesidadExtraordinaria();

            assertThat(vacia.esCompatibleCon(sub("arroz"))).isFalse();
        }
    }

    @Nested
    @DisplayName("NecesidadExtraordinaria - se satisface al alcanzar el objetivo")
    class Extraordinaria {

        private NecesidadExtraordinaria necesidadDe100() {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            necesidad.setNombre("Colchones tras la inundación");
            necesidad.setCantidadObjetivo(100);
            return necesidad;
        }

        @Test
        @DisplayName("No está satisfecha mientras falte para el objetivo")
        void noSatisfechaSiFalta() {
            NecesidadExtraordinaria necesidad = necesidadDe100();
            necesidad.recibir(99, HOY);

            assertThat(necesidad.estaSatisfechaAl(HOY)).isFalse();
        }

        @Test
        @DisplayName("Queda satisfecha al alcanzar exactamente el objetivo")
        void satisfechaAlAlcanzarExacto() {
            NecesidadExtraordinaria necesidad = necesidadDe100();
            necesidad.recibir(100, HOY);

            assertThat(necesidad.estaSatisfechaAl(HOY)).isTrue();
        }

        @Test
        @DisplayName("Queda satisfecha si se supera el objetivo")
        void satisfechaAlSuperar() {
            NecesidadExtraordinaria necesidad = necesidadDe100();
            necesidad.recibir(150, HOY);

            assertThat(necesidad.estaSatisfechaAl(HOY)).isTrue();
        }

        @Test
        @DisplayName("Una vez satisfecha no vence: no depende de ninguna fecha")
        void noVence() {
            // A diferencia de la recurrente, la extraordinaria no se reinicia:
            // cubierta la emergencia, la necesidad queda cerrada.
            NecesidadExtraordinaria necesidad = necesidadDe100();
            necesidad.setFechaRegistro(HOY.minusYears(3));
            necesidad.recibir(100, HOY.minusYears(3));

            assertThat(necesidad.estaSatisfechaAl(HOY)).isTrue();
            assertThat(necesidad.tipo()).isEqualTo(TipoNecesidad.EXTRAORDINARIA);
        }
    }

    @Nested
    @DisplayName("NecesidadRecurrente - períodos y reinicio del contador")
    class Recurrente {

        /** Necesidad semanal de 100 unidades cuyo período arrancó en la fecha dada. */
        private NecesidadRecurrente semanalDesde(LocalDate inicioPeriodo) {
            NecesidadRecurrente necesidad = new NecesidadRecurrente();
            necesidad.setNombre("Fideos semanales");
            necesidad.setCantidadObjetivo(100);
            necesidad.setPeriodo(Periodicidad.SEMANAL);
            necesidad.setFechaInicioPeriodo(inicioPeriodo);
            return necesidad;
        }

        @Test
        @DisplayName("El período NO está vencido dentro de la semana")
        void periodoVigente() {
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));

            assertThat(necesidad.periodoVencido(LocalDate.of(2026, 3, 4))).isFalse();
        }

        @Test
        @DisplayName("El último día de la semana todavía es del período")
        void ultimoDiaEsVigente() {
            // El período es [inicio, inicio + 7 días): arrancó el 1, el 7 es su
            // último día y el 8 empieza el siguiente.
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));

            assertThat(necesidad.periodoVencido(LocalDate.of(2026, 3, 7))).isFalse();
        }

        @Test
        @DisplayName("El día en que empieza el período siguiente, el anterior ya venció")
        void periodoVencido() {
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));

            assertThat(necesidad.periodoVencido(LocalDate.of(2026, 3, 8))).isTrue();
        }

        @Test
        @DisplayName("Una necesidad sin fecha de inicio de período nunca se considera vencida")
        void sinFechaInicioNoVence() {
            NecesidadRecurrente necesidad = semanalDesde(null);

            assertThat(necesidad.periodoVencido(LocalDate.of(2030, 1, 1))).isFalse();
        }

        @Test
        @DisplayName("actualizarPeriodo() reinicia lo recibido y avanza de a semanas enteras")
        void periodoVencidoReiniciaElContador() {
            // Regla del enunciado: lo recibido la semana pasada no cuenta para la
            // semana actual. El período nuevo sigue empezando el mismo día de la
            // semana (domingo 1 → domingo 15), no el día en que se consulta.
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));
            necesidad.recibir(80, LocalDate.of(2026, 3, 2));

            necesidad.actualizarPeriodo(LocalDate.of(2026, 3, 17));

            assertThat(necesidad.getCantidadRecibida()).isZero();
            assertThat(necesidad.getFechaInicioPeriodo()).isEqualTo(LocalDate.of(2026, 3, 15));
        }

        @Test
        @DisplayName("actualizarPeriodo() NO toca nada si el período sigue vigente")
        void periodoVigenteNoReinicia() {
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));
            necesidad.recibir(80, LocalDate.of(2026, 3, 2));

            necesidad.actualizarPeriodo(LocalDate.of(2026, 3, 4));

            assertThat(necesidad.getCantidadRecibida()).isEqualTo(80);
            assertThat(necesidad.getFechaInicioPeriodo()).isEqualTo(LocalDate.of(2026, 3, 1));
        }

        @Test
        @DisplayName("Recibir en un período nuevo arranca de cero")
        void recibirEnPeriodoNuevo() {
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));
            necesidad.recibir(80, LocalDate.of(2026, 3, 2));

            necesidad.recibir(30, LocalDate.of(2026, 3, 9));

            assertThat(necesidad.getCantidadRecibida()).isEqualTo(30);
            assertThat(necesidad.getFechaInicioPeriodo()).isEqualTo(LocalDate.of(2026, 3, 8));
        }

        @Test
        @DisplayName("Está satisfecha si alcanzó el objetivo y el período sigue vigente")
        void satisfechaDentroDelPeriodo() {
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));
            necesidad.recibir(100, LocalDate.of(2026, 3, 2));

            assertThat(necesidad.estaSatisfechaAl(LocalDate.of(2026, 3, 4))).isTrue();
        }

        @Test
        @DisplayName("NO está satisfecha si alcanzó el objetivo pero el período ya venció")
        void objetivoAlcanzadoPeroPeriodoVencido() {
            // Diferencia central con la extraordinaria: aunque el contador diga 100,
            // como el período venció la necesidad vuelve a estar abierta.
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));
            necesidad.recibir(100, LocalDate.of(2026, 3, 2));

            assertThat(necesidad.estaSatisfechaAl(LocalDate.of(2026, 5, 1))).isFalse();
            assertThat(necesidad.cantidadRecibidaAl(LocalDate.of(2026, 5, 1))).isZero();
            assertThat(necesidad.faltanteAl(LocalDate.of(2026, 5, 1))).isEqualTo(100);
        }

        @Test
        @DisplayName("estaSatisfecha() sin fecha usa el día de hoy")
        void satisfechaHoy() {
            NecesidadRecurrente necesidad = semanalDesde(FechaHoraArgentina.hoy());
            necesidad.recibir(100, FechaHoraArgentina.hoy());

            assertThat(necesidad.estaSatisfecha()).isTrue();
        }

        @Test
        @DisplayName("NO está satisfecha si el período está vigente pero falta cantidad")
        void faltaCantidadDentroDelPeriodo() {
            NecesidadRecurrente necesidad = semanalDesde(LocalDate.of(2026, 3, 1));
            necesidad.recibir(50, LocalDate.of(2026, 3, 2));

            assertThat(necesidad.estaSatisfechaAl(LocalDate.of(2026, 3, 4))).isFalse();
            assertThat(necesidad.tipo()).isEqualTo(TipoNecesidad.RECURRENTE);
        }
    }

    @Nested
    @DisplayName("Periodicidad - duración de calendario")
    class Periodicidades {

        @Test
        @DisplayName("Cada periodicidad es un Period de calendario y no una cantidad fija de días")
        void duracionDeCadaPeriodicidad() {
            // Los valores salen del enunciado: semanal, mensual, cuatrimestral y anual.
            assertThat(Periodicidad.SEMANAL.getDuracion()).isEqualTo(Period.ofWeeks(1));
            assertThat(Periodicidad.MENSUAL.getDuracion()).isEqualTo(Period.ofMonths(1));
            assertThat(Periodicidad.CUATRIMESTRAL.getDuracion()).isEqualTo(Period.ofMonths(4));
            assertThat(Periodicidad.ANUAL.getDuracion()).isEqualTo(Period.ofYears(1));
        }

        @Test
        @DisplayName("Un período mensual que arranca el 1 de marzo termina el 1 de abril")
        void mesDeCalendario() {
            // Con 30 días fijos, terminaba el 31 de marzo.
            assertThat(Periodicidad.MENSUAL.finDelPeriodo(LocalDate.of(2026, 3, 1))).isEqualTo(LocalDate.of(2026, 4, 1));
        }

        @Test
        @DisplayName("Un período mensual que arrancó un 31 vuelve al 31 en los meses que lo tienen")
        void mesesCortos() {
            LocalDate inicio = LocalDate.of(2026, 1, 31);

            assertThat(Periodicidad.MENSUAL.inicioDelPeriodoVigente(inicio, LocalDate.of(2026, 3, 5)))
                    .isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(Periodicidad.MENSUAL.inicioDelPeriodoVigente(inicio, LocalDate.of(2026, 4, 1)))
                    .isEqualTo(LocalDate.of(2026, 3, 31));
        }
    }

    @Nested
    @DisplayName("Identidad de la necesidad")
    class Identidad {

        @Test
        @DisplayName("Toda necesidad recibe un id propio al crearse")
        void idAutogenerado() {
            // El id se usa para buscar y eliminar la necesidad dentro de la campaña.
            Necesidad una = new NecesidadExtraordinaria();
            Necesidad otra = new NecesidadExtraordinaria();

            assertThat(una.getId()).isNotNull();
            assertThat(otra.getId()).isNotNull();
            assertThat(una.getId()).isNotEqualTo(otra.getId());
        }
    }
}
