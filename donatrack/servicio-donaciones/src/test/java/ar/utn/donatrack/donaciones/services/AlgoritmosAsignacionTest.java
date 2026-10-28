package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioRecepcionesEntidad;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de los dos algoritmos de asignación y del Template Method que comparten.
 *
 * El enunciado pide poder recomendar entidades beneficiarias para una donación
 * mediante distintos criterios, y que agregar un criterio nuevo no obligue a
 * tocar el resto. Eso se resuelve con AlgoritmoAsignacionBase (Template Method):
 * la clase base define el esqueleto —puntuar, filtrar, ordenar y recortar al
 * top 10— y cada algoritmo concreto solo implementa cómo puntúa.
 *
 * Los dos criterios implementados son:
 *   - AlgoritmoCompatibilidadSemantica: puntúa por cantidad de necesidades que
 *     mencionan la subcategoría donada.
 *   - AlgoritmoPrioridadSubAtendidos: puntúa más alto a quien menos recibió en
 *     el último trimestre, para repartir mejor las donaciones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Algoritmos de asignación - Template Method y criterios concretos")
class AlgoritmosAsignacionTest {

    @Mock
    private RepositorioRecepcionesEntidad recepciones;

    private AlgoritmoCompatibilidadSemantica algoritmoSemantico;
    private AlgoritmoPrioridadSubAtendidos algoritmoSubAtendidos;

    @BeforeEach
    void crearAlgoritmos() {
        algoritmoSemantico = new AlgoritmoCompatibilidadSemantica();
        algoritmoSubAtendidos = new AlgoritmoPrioridadSubAtendidos(recepciones);
    }

    /** Donación de la subcategoría indicada, lista para evaluar. */
    private Donacion donacionDe(String subcategoria) {
        Donacion donacion = new Donacion();
        donacion.setSubcategoria(subcategoria == null ? null : new Subcategoria(subcategoria));
        return donacion;
    }

    /** Entidad con una campaña que contiene las necesidades indicadas por nombre. */
    private EntidadBeneficiaria entidadCon(String razonSocial, String... nombresDeNecesidades) {
        EntidadBeneficiaria entidad = EntidadBeneficiaria.builder()
                .id(UUID.randomUUID())
                .razonSocial(razonSocial)
                .campanias(new ArrayList<>())
                .build();

        Campania campania = new Campania();
        campania.setIdCampania(UUID.randomUUID());
        campania.setIdEntidad(entidad.getId());
        entidad.agregarCampania(campania);

        for (String nombre : nombresDeNecesidades) {
            Necesidad necesidad = new NecesidadExtraordinaria();
            necesidad.setNombre(nombre);
            necesidad.setCantidadObjetivo(100);
            campania.agregarNecesidad(necesidad);
        }
        return entidad;
    }

    @Nested
    @DisplayName("AlgoritmoCompatibilidadSemantica - matching por necesidades declaradas")
    class CompatibilidadSemantica {

        @Test
        @DisplayName("El puntaje es la cantidad de necesidades que mencionan la subcategoría donada")
        void puntajeEsLaCantidadDeCoincidencias() {
            EntidadBeneficiaria dosCoincidencias = entidadCon("Comedor A", "Arroz para el guiso", "Más arroz", "Colchones");

            List<ResultadoAsignacion> ranking = algoritmoSemantico.evaluar(donacionDe("arroz"), List.of(dosCoincidencias));

            assertThat(ranking).hasSize(1);
            assertThat(ranking.getFirst().getPuntaje()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Las entidades sin ninguna necesidad compatible quedan FUERA del ranking")
        void entidadesSinCoincidenciasSeDescartan() {
            // Es el hook incluir() sobreescrito: no tiene sentido recomendar una
            // entidad que no declaró ninguna necesidad relacionada con lo donado.
            EntidadBeneficiaria conCoincidencia = entidadCon("Comedor A", "Arroz para el guiso");
            EntidadBeneficiaria sinCoincidencia = entidadCon("Comedor B", "Colchones", "Frazadas");

            List<ResultadoAsignacion> ranking = algoritmoSemantico
                    .evaluar(donacionDe("arroz"), List.of(conCoincidencia, sinCoincidencia));

            assertThat(ranking)
                    .hasSize(1)
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(conCoincidencia.getId());
        }

        @Test
        @DisplayName("El ranking se ordena de mayor a menor puntaje")
        void rankingOrdenadoDescendente() {
            EntidadBeneficiaria unaCoincidencia = entidadCon("Comedor A", "Arroz");
            EntidadBeneficiaria tresCoincidencias = entidadCon("Comedor B", "Arroz", "Arroz integral", "Arroz doble carolina");
            EntidadBeneficiaria dosCoincidencias = entidadCon("Comedor C", "Arroz", "Arroz largo fino");

            List<ResultadoAsignacion> ranking = algoritmoSemantico.evaluar(
                    donacionDe("arroz"),
                    List.of(unaCoincidencia, tresCoincidencias, dosCoincidencias));

            assertThat(ranking)
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(tresCoincidencias.getId(), dosCoincidencias.getId(), unaCoincidencia.getId());
        }

        @Test
        @DisplayName("Una donación sin subcategoría no genera candidatas")
        void sinSubcategoriaNoHayCandidatas() {
            // Caso defensivo: sin subcategoría el puntaje es 0 para todas y el
            // hook incluir() las descarta a todas.
            EntidadBeneficiaria entidad = entidadCon("Comedor A", "Arroz");

            assertThat(algoritmoSemantico.evaluar(donacionDe(null), List.of(entidad))).isEmpty();
        }

        @Test
        @DisplayName("Sin entidades cargadas el ranking es vacío, no null")
        void sinEntidadesRankingVacio() {
            assertThat(algoritmoSemantico.evaluar(donacionDe("arroz"), List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("AlgoritmoPrioridadSubAtendidos - reparto equitativo")
    class PrioridadSubAtendidos {

        // Desde la Entrega 5 las recepciones salen del contador desnormalizado
        // entidad_recepcion_diaria. Qué cuenta como recepción (la ventana de 3
        // meses, solo la entidad asignada, solo donaciones con fecha) lo prueban
        // los tests de persistencia del contador contra H2.

        @Test
        @DisplayName("Una entidad que no recibió nada obtiene el puntaje máximo (1.0)")
        void sinDonacionesRecibidasPuntajeMaximo() {
            // El puntaje es 1 / (1 + recibidas): con 0 recibidas da exactamente 1.
            EntidadBeneficiaria entidad = entidadCon("Comedor A");
            when(recepciones.contarRecepcionesPorEntidadDesde(any(LocalDate.class))).thenReturn(Map.of());

            List<ResultadoAsignacion> ranking = algoritmoSubAtendidos.evaluar(donacionDe("arroz"), List.of(entidad));

            assertThat(ranking.getFirst().getPuntaje()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("A más donaciones recibidas en el trimestre, menor puntaje")
        void masRecibidasMenorPuntaje() {
            EntidadBeneficiaria entidad = entidadCon("Comedor A");
            when(recepciones.contarRecepcionesPorEntidadDesde(any(LocalDate.class))).thenReturn(Map.of(entidad.getId(), 3L));

            List<ResultadoAsignacion> ranking = algoritmoSubAtendidos.evaluar(donacionDe("arroz"), List.of(entidad));

            // 1 / (1 + 3) = 0.25
            assertThat(ranking.getFirst().getPuntaje()).isCloseTo(0.25, within(0.0001));
        }

        @Test
        @DisplayName("La entidad menos atendida encabeza el ranking")
        void laMenosAtendidaVaPrimero() {
            // Éste es el objetivo del criterio: repartir en vez de concentrar.
            EntidadBeneficiaria muyAtendida = entidadCon("Comedor A");
            EntidadBeneficiaria pocoAtendida = entidadCon("Comedor B");
            when(recepciones.contarRecepcionesPorEntidadDesde(any(LocalDate.class)))
                    .thenReturn(Map.of(muyAtendida.getId(), 2L));

            List<ResultadoAsignacion> ranking = algoritmoSubAtendidos
                    .evaluar(donacionDe("arroz"), List.of(muyAtendida, pocoAtendida));

            assertThat(ranking)
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(pocoAtendida.getId(), muyAtendida.getId());
        }

        @Test
        @DisplayName("Consulta las recepciones de los últimos 3 meses una sola vez para todas las entidades")
        void unaConsultaConLaVentanaDelTrimestre() {
            // Antes se leían todas las donaciones una vez POR ENTIDAD.
            when(recepciones.contarRecepcionesPorEntidadDesde(any(LocalDate.class))).thenReturn(Map.of());

            algoritmoSubAtendidos.evaluar(donacionDe("arroz"),
                    List.of(entidadCon("A"), entidadCon("B"), entidadCon("C")));

            verify(recepciones, times(1)).contarRecepcionesPorEntidadDesde(FechaHoraArgentina.hoy().minusMonths(3));
        }

        @Test
        @DisplayName("A diferencia del semántico, este criterio NO descarta entidades")
        void nuncaDescartaEntidades() {
            // No sobreescribe el hook incluir(), así que toda entidad tiene una
            // chance: incluso una muy atendida sigue en el ranking, pero al final.
            EntidadBeneficiaria sinNecesidades = entidadCon("Comedor A");
            when(recepciones.contarRecepcionesPorEntidadDesde(any(LocalDate.class))).thenReturn(Map.of());

            assertThat(algoritmoSubAtendidos.evaluar(donacionDe("arroz"), List.of(sinNecesidades))).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Template Method - comportamiento común a todo algoritmo")
    class TemplateMethod {

        @Test
        @DisplayName("El ranking se recorta al top 10 aunque haya más entidades candidatas")
        void recorteAlTop10() {
            // TOP_N = 10 en AlgoritmoAsignacionBase. Con 15 entidades candidatas
            // el ranking debe devolver solo las 10 mejores.
            List<EntidadBeneficiaria> quinceEntidades = IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> entidadCon("Comedor " + i, "Arroz"))
                    .toList();

            List<ResultadoAsignacion> ranking = algoritmoSemantico.evaluar(donacionDe("arroz"), quinceEntidades);

            assertThat(ranking).hasSize(10);
        }

        @Test
        @DisplayName("El recorte se aplica DESPUÉS de ordenar: quedan las 10 mejores, no las 10 primeras")
        void recorteDespuesDeOrdenar() {
            // Se arma una lista donde la mejor candidata está al final, para
            // comprobar que el orden del ranking no depende del orden de entrada.
            List<EntidadBeneficiaria> entidades = new ArrayList<>(IntStream.rangeClosed(1, 12)
                    .mapToObj(i -> entidadCon("Comedor " + i, "Arroz"))
                    .toList());
            EntidadBeneficiaria laMejor = entidadCon("La mejor", "Arroz", "Arroz integral", "Arroz largo fino");
            entidades.add(laMejor);

            List<ResultadoAsignacion> ranking = algoritmoSemantico.evaluar(donacionDe("arroz"), entidades);

            assertThat(ranking).hasSize(10);
            assertThat(ranking.getFirst().getIdEntidad()).isEqualTo(laMejor.getId());
        }
    }
}
