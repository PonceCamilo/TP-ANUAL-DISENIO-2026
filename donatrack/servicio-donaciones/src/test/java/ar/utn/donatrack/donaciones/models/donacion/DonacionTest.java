package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoModificableException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.BienNoEncontradoException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.BienIncompatibleConDonacionException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del comportamiento propio de la Donación (modelo rico).
 *
 * Tras el refactor de Entrega 3, la Donación dejó de ser un contenedor de datos:
 * ahora sabe cambiar su propio estado, registrar el historial de esos cambios,
 * asignarse a una entidad y responder preguntas sobre sí misma (si es perecedera,
 * si requiere estado nuevo/usado, si pertenece a una subcategoría, etc.).
 *
 * Estas pruebas cubren esos métodos. La lógica de qué transiciones son válidas
 * está probada aparte en EstadoDonacionBaseTest; acá se verifica cómo la
 * Donación *usa* esa máquina de estados.
 */
@DisplayName("Donacion - comportamiento del modelo rico")
class DonacionTest {

    private Donacion donacion;

    @BeforeEach
    void crearDonacionEnDeposito() {
        donacion = new Donacion();
        donacion.setIdDonante(UUID.randomUUID());
        donacion.setDescripcion("Donación de prueba");
        donacion.setSubcategoria(new Subcategoria("arroz"));
    }

    @Nested
    @DisplayName("reemplazarBien(): corregir un bien sin romper la segmentación")
    class ReemplazarBien {

        private Bien generico(String subcategoria) {
            return BienGenerico.builder().subcategoria(new Subcategoria(subcategoria))
                    .descripcion("bien").cantidad(1).unidad("kg").build();
        }

        private Bien perecedero(String subcategoria, LocalDate vence) {
            return BienPerecible.builder().subcategoria(new Subcategoria(subcategoria))
                    .descripcion("bien").cantidad(1).unidad("kg").fechaVencimiento(vence).build();
        }

        @Test
        @DisplayName("Reemplaza el bien indicado; el nuevo conserva su propio id")
        void reemplaza() {
            Bien original = generico("arroz");
            donacion.getBienes().add(original);
            Bien corregido = generico("arroz");

            donacion.reemplazarBien(original.getId(), corregido);

            assertThat(donacion.getBienes()).containsExactly(corregido);
        }

        @Test
        @DisplayName("Un id que no es de la donación es un 404")
        void bienInexistente() {
            donacion.getBienes().add(generico("arroz"));

            assertThatThrownBy(() -> donacion.reemplazarBien(UUID.randomUUID(), generico("arroz")))
                    .isInstanceOf(BienNoEncontradoException.class);
        }

        @Test
        @DisplayName("El bien no puede cambiar de subcategoría: la donación tiene una sola")
        void otraSubcategoria() {
            Bien original = generico("arroz");
            donacion.getBienes().add(original);

            assertThatThrownBy(() -> donacion.reemplazarBien(original.getId(), generico("fideos")))
                    .isInstanceOf(BienIncompatibleConDonacionException.class);
        }

        @Test
        @DisplayName("Con otros bienes, el corregido tiene que compartir su vencimiento")
        void rompeLaSegmentacion() {
            Bien uno = perecedero("arroz", LocalDate.of(2027, 1, 1));
            Bien otro = perecedero("arroz", LocalDate.of(2027, 1, 1));
            donacion.getBienes().add(uno);
            donacion.getBienes().add(otro);

            assertThatThrownBy(() -> donacion.reemplazarBien(uno.getId(), perecedero("arroz", LocalDate.of(2028, 1, 1))))
                    .isInstanceOf(BienIncompatibleConDonacionException.class);
        }

        @Test
        @DisplayName("Fuera del depósito los bienes ya no se modifican")
        void fueraDelDeposito() {
            Bien original = generico("arroz");
            donacion.getBienes().add(original);
            donacion.cambiarEstado("VENCIDA", "vencida", null);

            assertThatThrownBy(() -> donacion.reemplazarBien(original.getId(), generico("arroz")))
                    .isInstanceOf(DonacionNoModificableException.class);
        }
    }

    @Nested
    @DisplayName("tieneBienesVencidosAl(): perecederos vencidos")
    class BienesVencidos {

        @Test
        @DisplayName("Detecta un perecedero vencido antes de la fecha; vencer ese mismo día no cuenta")
        void detectaVencidos() {
            donacion.getBienes().add(BienPerecible.builder().subcategoria(new Subcategoria("arroz"))
                    .descripcion("arroz").cantidad(1).unidad("kg").fechaVencimiento(LocalDate.of(2026, 5, 10)).build());

            assertThat(donacion.tieneBienesVencidosAl(LocalDate.of(2026, 5, 11))).isTrue();
            assertThat(donacion.tieneBienesVencidosAl(LocalDate.of(2026, 5, 10))).isFalse();
        }

        @Test
        @DisplayName("Los bienes que no vencen nunca cuentan como vencidos")
        void noPerecederos() {
            donacion.getBienes().add(BienGenerico.builder().subcategoria(new Subcategoria("arroz"))
                    .descripcion("arroz").cantidad(1).unidad("kg").build());

            assertThat(donacion.tieneBienesVencidosAl(LocalDate.of(2099, 1, 1))).isFalse();
        }
    }

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicial {

        @Test
        @DisplayName("Una donación recién creada nace EN_DEPOSITO")
        void naceEnDeposito() {
            // Regla del enunciado: al registrarse, la donación queda "En depósito",
            // lista para ser evaluada por el algoritmo de asignación.
            assertThat(donacion.estaEnEstado("EN_DEPOSITO")).isTrue();
            assertThat(donacion.getEstado().nombre()).isEqualTo("EN_DEPOSITO");
        }

        @Test
        @DisplayName("Una donación recién creada tiene id, fecha y colecciones inicializadas")
        void camposInicializados() {
            // Se verifica que no haya nulls sorpresa: id y fechaDonacion se
            // autogeneran y las listas arrancan vacías (nunca null).
            assertThat(donacion.getId()).isNotNull();
            assertThat(donacion.getFechaDonacion()).isNotNull();
            assertThat(donacion.getBienes()).isEmpty();
            assertThat(donacion.getHistorialEstados()).isEmpty();
        }

        @Test
        @DisplayName("Una donación sin asignar no tiene entidad beneficiaria ni fecha de asignación")
        void sinAsignar() {
            assertThat(donacion.getIdEntidadBeneficiaria()).isNull();
            assertThat(donacion.getFechaAsignacion()).isNull();
        }
    }

    @Nested
    @DisplayName("cambiarEstado() - delega en el State y registra el historial")
    class CambiarEstado {

        @Test
        @DisplayName("Un cambio de estado válido actualiza el estado actual")
        void cambioValidoActualizaEstado() {
            donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", "Asignada a Comedor X");

            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Cada cambio deja una entrada en el historial con estado previo, nuevo y justificación")
        void cambioRegistraHistorial() {
            // La trazabilidad es un requisito explícito del enunciado: se debe poder
            // reconstruir todo el recorrido de la donación.
            donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", "Asignada a Comedor X");

            assertThat(donacion.getHistorialEstados()).hasSize(1);

            CambioEstado registro = donacion.getHistorialEstados().getFirst();
            assertThat(registro.getEstadoPrevio().nombre()).isEqualTo("EN_DEPOSITO");
            assertThat(registro.getEstado().nombre()).isEqualTo("ASIGNACION_REALIZADA");
            assertThat(registro.getNombreTransicion()).isEqualTo("asignar");
            assertThat(registro.getJustificacion()).isEqualTo("Asignada a Comedor X");
        }

        @Test
        @DisplayName("El historial acumula los cambios en orden cronológico")
        void historialAcumulaEnOrden() {
            donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            donacion.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar", null);
            donacion.cambiarEstado("EN_TRASLADO", "iniciar ruta", null);

            assertThat(donacion.getHistorialEstados())
                    .extracting(c -> c.getEstado().nombre())
                    .containsExactly("ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR", "EN_TRASLADO");
        }

        @Test
        @DisplayName("Una transición ilegal deja la donación intacta: ni cambia el estado ni suma historial")
        void transicionIlegalNoDejaRastro() {
            // Verificación importante: como el State lanza la excepción ANTES de que
            // se agregue el registro, la donación no queda en un estado inconsistente.
            assertThatThrownBy(() -> donacion.cambiarEstado("ENTREGADA", "entregar", null))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);

            assertThat(donacion.estaEnEstado("EN_DEPOSITO")).isTrue();
            assertThat(donacion.getHistorialEstados()).isEmpty();
        }

        @Test
        @DisplayName("Marcar ENTREGA_FALLIDA sin justificación falla y no modifica la donación")
        void entregaFallidaSinJustificacionNoDejaRastro() {
            donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            donacion.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar", null);
            donacion.cambiarEstado("EN_TRASLADO", "iniciar ruta", null);

            assertThatThrownBy(() -> donacion.cambiarEstado("ENTREGA_FALLIDA", "fallar", null))
                    .isInstanceOf(FaltaJustificacionException.class);

            assertThat(donacion.estaEnEstado("EN_TRASLADO")).isTrue();
            assertThat(donacion.getHistorialEstados()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("asignarA() - asignación a una entidad beneficiaria")
    class AsignarA {

        private EntidadBeneficiaria entidad;

        @BeforeEach
        void crearEntidad() {
            entidad = EntidadBeneficiaria.builder()
                    .id(UUID.randomUUID())
                    .razonSocial("Comedor Los Pibes")
                    .build();
        }

        @Test
        @DisplayName("Asignar guarda el id de la entidad, la fecha y pasa a ASIGNACION_REALIZADA en un solo paso")
        void asignarHaceLasTresCosas() {
            // asignarA() encapsula las tres operaciones para que ningún service pueda
            // olvidarse de una y dejar la donación asignada pero con el estado viejo.
            donacion.asignarA(entidad);

            assertThat(donacion.getIdEntidadBeneficiaria()).isEqualTo(entidad.getId());
            assertThat(donacion.getFechaAsignacion()).isEqualTo(FechaHoraArgentina.hoy());
            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("La justificación registrada menciona a la entidad asignada")
        void justificacionMencionaLaEntidad() {
            donacion.asignarA(entidad);

            assertThat(donacion.getHistorialEstados().getFirst().getJustificacion())
                    .contains("Comedor Los Pibes");
        }

        @Test
        @DisplayName("No se puede asignar una donación que ya fue asignada")
        void noSePuedeAsignarDosVeces() {
            // La segunda llamada intenta ASIGNACION_REALIZADA -> ASIGNACION_REALIZADA,
            // que no es una transición contemplada por la máquina de estados.
            donacion.asignarA(entidad);

            assertThatThrownBy(() -> donacion.asignarA(entidad))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
        }
    }

    @Nested
    @DisplayName("Consultas sobre el tipo de bienes que contiene")
    class ConsultasSobreBienes {

        @Test
        @DisplayName("esPerecible() es true cuando la donación contiene bienes perecederos")
        void esPerecible() {
            donacion.setBienes(List.of(BienPerecible.builder()
                    .subcategoria(new Subcategoria("arroz"))
                    .descripcion("Arroz")
                    .cantidad(10)
                    .unidad("kg")
                    .fechaVencimiento(LocalDate.of(2027, 1, 1))
                    .build()));

            assertThat(donacion.esPerecible()).isTrue();
            assertThat(donacion.requiereEstado()).isFalse();
        }

        @Test
        @DisplayName("requiereEstado() es true cuando la donación contiene bienes con estado nuevo/usado")
        void requiereEstado() {
            donacion.setBienes(List.of(BienConEstado.builder()
                    .subcategoria(new Subcategoria("sillas"))
                    .descripcion("Sillas")
                    .cantidad(6)
                    .unidad("unidades")
                    .esNuevo(false)
                    .build()));

            assertThat(donacion.requiereEstado()).isTrue();
            assertThat(donacion.esPerecible()).isFalse();
        }

        @Test
        @DisplayName("Una donación de bienes genéricos no es perecedera ni requiere estado")
        void bienGenerico() {
            donacion.setBienes(List.of(BienGenerico.builder()
                    .subcategoria(new Subcategoria("ropa"))
                    .descripcion("Camperas")
                    .cantidad(3)
                    .unidad("unidades")
                    .build()));

            assertThat(donacion.esPerecible()).isFalse();
            assertThat(donacion.requiereEstado()).isFalse();
        }

        @Test
        @DisplayName("Una donación sin bienes responde false a ambas preguntas (sin romper)")
        void sinBienesNoRompe() {
            // Caso borde: las implementaciones miran el primer bien de la lista,
            // por lo que deben contemplar la lista vacía.
            assertThat(donacion.esPerecible()).isFalse();
            assertThat(donacion.requiereEstado()).isFalse();
        }
    }

    @Nested
    @DisplayName("Consultas de estado y subcategoría")
    class ConsultasVarias {

        @Test
        @DisplayName("esDeSubcategoria() ignora mayúsculas y minúsculas")
        void esDeSubcategoriaIgnoraCase() {
            // El algoritmo de asignación compara la subcategoría de la donación
            // contra el texto de las necesidades, y ahí el case no debe importar.
            assertThat(donacion.esDeSubcategoria("arroz")).isTrue();
            assertThat(donacion.esDeSubcategoria("ARROZ")).isTrue();
            assertThat(donacion.esDeSubcategoria("Arroz")).isTrue();
        }

        @Test
        @DisplayName("esDeSubcategoria() es false para otra subcategoría")
        void esDeSubcategoriaOtra() {
            assertThat(donacion.esDeSubcategoria("ropa")).isFalse();
        }

        @Test
        @DisplayName("esDeSubcategoria() no rompe cuando la donación aún no tiene subcategoría")
        void esDeSubcategoriaSinSubcategoria() {
            Donacion sinSubcategoria = new Donacion();

            assertThat(sinSubcategoria.esDeSubcategoria("arroz")).isFalse();
        }

        @Test
        @DisplayName("fueEntregada() solo es true al final del circuito")
        void fueEntregada() {
            assertThat(donacion.fueEntregada()).isFalse();

            donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            donacion.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar", null);
            donacion.cambiarEstado("EN_TRASLADO", "iniciar ruta", null);
            assertThat(donacion.fueEntregada()).isFalse();

            donacion.cambiarEstado("ENTREGADA", "entregar", null);
            assertThat(donacion.fueEntregada()).isTrue();
        }
    }
}
