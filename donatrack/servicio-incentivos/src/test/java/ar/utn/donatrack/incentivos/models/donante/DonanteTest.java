package ar.utn.donatrack.incentivos.models.donante;

import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Sostenedor;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Transformador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.DonacionesExitosas;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Donante - perfil de incentivos")
class DonanteTest {

    @Nested
    @DisplayName("Donaciones")
    class Donaciones {

        @Test
        @DisplayName("Registrar una donacion la guarda en el historial del donante")
        void registraDonacion() {
            Donante donante = new Donante();
            DonacionRegistrada donacion = DonacionRegistrada.builder()
                    .fecha(LocalDateTime.now())
                    .cantidadBienes(4)
                    .categorias(Set.of("ALIMENTOS"))
                    .build();

            donante.registrarDonacion(donacion);

            assertThat(donante.getDonaciones()).containsExactly(donacion);
        }

        @Test
        @DisplayName("Conserva los datos que llegan desde servicio-donaciones")
        void conservaDatosDeLaDonacion() {
            Donante donante = new Donante();
            LocalDateTime fecha = LocalDateTime.of(2026, 6, 24, 19, 7);
            DonacionRegistrada donacion = DonacionRegistrada.builder()
                    .fecha(fecha)
                    .cantidadBienes(8)
                    .categorias(Set.of("ALIMENTOS", "ABRIGO"))
                    .entidadBeneficiaria("Comedor Norte")
                    .exitosa(true)
                    .build();

            donante.registrarDonacion(donacion);

            DonacionRegistrada guardada = donante.getDonaciones().get(0);
            assertThat(guardada.getFecha()).isEqualTo(fecha);
            assertThat(guardada.getCantidadBienes()).isEqualTo(8);
            assertThat(guardada.getCategorias()).containsExactlyInAnyOrder("ALIMENTOS", "ABRIGO");
            assertThat(guardada.getEntidadBeneficiaria()).isEqualTo("Comedor Norte");
            assertThat(guardada.isExitosa()).isTrue();
        }

        @Test
        @DisplayName("Registra varias donaciones en el orden recibido")
        void registraVariasDonaciones() {
            Donante donante = new Donante();
            DonacionRegistrada primera = donacion("ALIMENTOS");
            DonacionRegistrada segunda = donacion("ABRIGO");

            donante.registrarDonacion(primera);
            donante.registrarDonacion(segunda);

            assertThat(donante.getDonaciones()).containsExactly(primera, segunda);
        }
    }

    @Nested
    @DisplayName("Categorias")
    class Categorias {

        @Test
        @DisplayName("Un colaborador puede subir a sostenedor")
        void colaboradorSubeASostenedor() {
            Donante donante = new Donante();
            donante.setCategoria(new Colaborador());

            boolean subio = donante.subirCategoria();

            assertThat(subio).isTrue();
            assertThat(donante.getCategoria()).isInstanceOf(Sostenedor.class);
        }

        @Test
        @DisplayName("Un sostenedor puede subir a transformador")
        void sostenedorSubeATransformador() {
            Donante donante = new Donante();
            donante.setCategoria(new Sostenedor());

            boolean subio = donante.subirCategoria();

            assertThat(subio).isTrue();
            assertThat(donante.getCategoria()).isInstanceOf(Transformador.class);
        }

        @Test
        @DisplayName("Un transformador no tiene categoria siguiente")
        void transformadorNoSube() {
            Donante donante = new Donante();
            donante.setCategoria(new Transformador());

            boolean subio = donante.subirCategoria();

            assertThat(subio).isFalse();
            assertThat(donante.getCategoria()).isInstanceOf(Transformador.class);
        }
    }

    @Nested
    @DisplayName("Insignias")
    class Insignias {

        @Test
        @DisplayName("Agregar insignia suma una insignia obtenida")
        void agregaInsignia() {
            Donante donante = new Donante();
            InsigniaObtenida insignia = new InsigniaObtenida(insignia("Semilla"), true);

            donante.agregarInsignia(insignia);

            assertThat(donante.getInsigniasObtenidas()).containsExactly(insignia);
        }

        @Test
        @DisplayName("Las insignias agregadas quedan disponibles para consultar")
        void obtieneInsigniasAgregadas() {
            Donante donante = new Donante();
            InsigniaObtenida semilla = new InsigniaObtenida(insignia("Semilla"), true);
            InsigniaObtenida racha = new InsigniaObtenida(insignia("Racha"), true);

            donante.agregarInsignia(semilla);
            donante.agregarInsignia(racha);

            assertThat(donante.getInsigniasObtenidas())
                    .extracting(insigniaObtenida -> insigniaObtenida.getInsignia().getNombre())
                    .containsExactly("Semilla", "Racha");
        }

        @Test
        @DisplayName("Cambiar visibilidad modifica solo la insignia indicada")
        void cambiaVisibilidad() {
            Donante donante = new Donante();
            InsigniaObtenida visible = new InsigniaObtenida(insignia("Semilla"), true);
            InsigniaObtenida otra = new InsigniaObtenida(insignia("Racha"), true);
            donante.setInsigniasObtenidas(List.of(visible, otra));

            donante.cambiarVisibilidadInsignia(visible.getId(), false);

            assertThat(visible.isVisibilidad()).isFalse();
            assertThat(otra.isVisibilidad()).isTrue();
        }

        @Test
        @DisplayName("Completar una mision agrega la insignia correspondiente")
        void completarMisionAgregaInsignia() {
            Donante donante = new Donante();
            Mision mision = new DonacionesExitosas("Entrega finalizada", "Completar entrega", new Colaborador(), 1, insignia("Semilla"));

            donante.completarMision(mision);

            assertThat(donante.getInsigniasObtenidas())
                    .extracting(insigniaObtenida -> insigniaObtenida.getInsignia().getNombre())
                    .containsExactly("Semilla");
        }

        @Test
        @DisplayName("Cuenta solo las misiones completadas en el periodo pedido")
        void cuentaMisionesCompletadasPorPeriodo() {
            Donante donante = new Donante();
            InsigniaObtenida junio = new InsigniaObtenida(insignia("Semilla"), true);
            InsigniaObtenida mayo = new InsigniaObtenida(insignia("Racha"), true);
            junio.setFechaObtencion(java.time.LocalDate.of(2026, 6, 10));
            mayo.setFechaObtencion(java.time.LocalDate.of(2026, 5, 10));
            donante.agregarInsignia(junio);
            donante.agregarInsignia(mayo);

            int completadas = donante.misionesCompletadasEnPeriodo(6, 2026);

            assertThat(completadas).isEqualTo(1);
        }
    }

    private Insignia insignia(String nombre) {
        return Insignia.builder().nombre(nombre).imagen(nombre + ".png").build();
    }

    private DonacionRegistrada donacion(String categoria) {
        return DonacionRegistrada.builder()
                .fecha(LocalDateTime.now())
                .cantidadBienes(1)
                .categorias(Set.of(categoria))
                .exitosa(false)
                .build();
    }
}
