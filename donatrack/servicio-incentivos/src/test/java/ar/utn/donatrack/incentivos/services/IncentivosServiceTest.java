package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.client.N8nWebhookClient;
import ar.utn.donatrack.incentivos.client.NotificacionClient;
import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Sostenedor;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Transformador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Completitud;
import ar.utn.donatrack.incentivos.models.misiones.DonacionesExitosas;
import ar.utn.donatrack.incentivos.models.misiones.HabilDonador;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.misiones.Racha;
import ar.utn.donatrack.incentivos.repositories.IncentivosRepositorioEnMemoria;
import ar.utn.donatrack.incentivos.validations.IncentivosValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncentivosService - casos de uso del servicio")
class IncentivosServiceTest {

    private static final String DESTINATARIO = "donante@mail.com";
    private static final String MEDIO = "EMAIL";

    @Mock
    private NotificacionClient notificacionClient;

    @Mock
    private N8nWebhookClient n8nWebhookClient;

    private IncentivosRepositorioEnMemoria repositorio;
    private IncentivosService service;

    @BeforeEach
    void crearService() {
        repositorio = new IncentivosRepositorioEnMemoria();
        service = new IncentivosService(repositorio, notificacionClient, n8nWebhookClient, new IncentivosValidator());
    }

    @Nested
    @DisplayName("Perfil")
    class Perfil {

        @Test
        @DisplayName("Obtener perfil crea un donante colaborador con su primera mision")
        void obtenerPerfilInicializaDonante() {
            DonacionesExitosas primeraMision = misionPrimeraDonacion();
            repositorio.guardarMision(primeraMision);

            Donante perfil = service.obtenerPerfil(UUID.randomUUID());

            assertThat(perfil.getCategoria()).isInstanceOf(Colaborador.class);
            assertThat(perfil.getProgresoMision().getMisionActual()).isSameAs(primeraMision);
            assertThat(repositorio.buscarPerfil(perfil.getId())).contains(perfil);
        }

        @Test
        @DisplayName("Obtener misiones devuelve las misiones de la categoria actual")
        void obtieneMisionesDeLaCategoria() {
            UUID donanteId = UUID.randomUUID();
            DonacionesExitosas colaborador = misionPrimeraDonacion();
            HabilDonador sostenedor = misionGranDonacion();
            repositorio.guardarMision(colaborador);
            repositorio.guardarMision(sostenedor);

            assertThat(service.obtenerMisiones(donanteId)).containsExactly(colaborador);
        }
    }

    @Nested
    @DisplayName("Procesar donacion registrada")
    class ProcesarDonacionRegistrada {

        @Test
        @DisplayName("Guarda la donacion y actualiza el progreso sin completar una mision exitosa")
        void guardaDonacionRegistrada() {
            UUID donanteId = UUID.randomUUID();
            repositorio.guardarMision(misionPrimeraDonacion());

            service.procesarDonacion(donanteId, donacionRegistrada(3, "ALIMENTOS"), DESTINATARIO, MEDIO);

            Donante perfil = service.obtenerPerfil(donanteId);
            assertThat(perfil.getDonaciones()).hasSize(1);
            assertThat(perfil.getProgresoMision().progresoActual(perfil)).isZero();
            verifyNoInteractions(notificacionClient, n8nWebhookClient);
        }

        @Test
        @DisplayName("Rechaza una donacion sin categorias")
        void rechazaCategoriasVacias() {
            UUID donanteId = UUID.randomUUID();
            repositorio.guardarMision(misionPrimeraDonacion());

            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    service.procesarDonacion(donanteId, donacionRegistradaSinCategorias(), DESTINATARIO, MEDIO)
            ).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Procesar donacion exitosa")
    class ProcesarDonacionExitosa {

        @Test
        @DisplayName("Completa la mision, entrega insignia y dispara n8n")
        void completaMisionYNotifica() {
            UUID donanteId = UUID.randomUUID();
            repositorio.guardarMision(misionPrimeraDonacion());
            repositorio.guardarMision(misionRacha());

            service.procesarDonacionExitosa(donanteId, donacionExitosa("Comedor Norte"), DESTINATARIO, MEDIO);

            Donante perfil = service.obtenerPerfil(donanteId);
            assertThat(perfil.getInsigniasObtenidas()).hasSize(1);
            assertThat(perfil.getProgresoMision().getMisionActual()).isInstanceOf(Racha.class);
            verify(notificacionClient).enviarNotificacion(eq(DESTINATARIO), eq("Mision cumplida: Primera donacion exitosa"), eq(MEDIO), eq("MISION_CUMPLIDA"));
            verify(n8nWebhookClient).notificarInsigniaObtenida(eq(donanteId), any(InsigniaObtenida.class), eq(DESTINATARIO));
        }

        @Test
        @DisplayName("Cuando termina las misiones de una categoria sube a la siguiente")
        void subeDeCategoria() {
            UUID donanteId = UUID.randomUUID();
            HabilDonador misionSostenedor = misionGranDonacion();
            repositorio.guardarMision(misionPrimeraDonacion());
            repositorio.guardarMision(misionSostenedor);

            service.procesarDonacionExitosa(donanteId, donacionExitosa("Comedor Norte"), DESTINATARIO, MEDIO);

            Donante perfil = service.obtenerPerfil(donanteId);
            assertThat(perfil.getCategoria()).isInstanceOf(Sostenedor.class);
            assertThat(perfil.getProgresoMision().getMisionActual()).isSameAs(misionSostenedor);
            verify(notificacionClient).enviarNotificacion(eq(DESTINATARIO), eq("Subiste a Sostenedor."), eq(MEDIO), eq("CAMBIO_CATEGORIA"));
        }

        @Test
        @DisplayName("Si no hay contacto no frena el otorgamiento de la insignia")
        void completaSinContacto() {
            UUID donanteId = UUID.randomUUID();
            repositorio.guardarMision(misionPrimeraDonacion());
            repositorio.guardarMision(misionGranDonacion());

            service.procesarDonacionExitosa(donanteId, donacionExitosa("Comedor Norte"), null, null);

            Donante perfil = service.obtenerPerfil(donanteId);
            assertThat(perfil.getInsigniasObtenidas()).hasSize(1);
            verify(notificacionClient, never()).enviarNotificacion(any(), any(), any(), any());
            verify(n8nWebhookClient, never()).notificarInsigniaObtenida(any(), any(), any());
        }

        @Test
        @DisplayName("Si completa la ultima categoria finaliza las misiones")
        void finalizaMisionesSinCategoriaSiguiente() {
            UUID donanteId = UUID.randomUUID();
            Completitud completitud = misionCompletitud();
            repositorio.guardarMision(completitud);
            Donante donante = new Donante();
            donante.setId(donanteId);
            donante.setCategoria(new Transformador());
            donante.cambiarMisionActual(completitud);
            repositorio.guardarPerfil(donante);

            service.procesarDonacion(donanteId, donacionRegistrada(1, "ALIMENTOS", "ABRIGO", "HIGIENE"), DESTINATARIO, MEDIO);

            Donante perfil = repositorio.buscarPerfil(donanteId).orElseThrow();
            assertThat(perfil.getInsigniasObtenidas()).hasSize(1);
            assertThat(perfil.getCategoria()).isInstanceOf(Transformador.class);
            assertThat(perfil.getProgresoMision().getMisionActual()).isNull();
        }
    }

    @Nested
    @DisplayName("Perdida de racha")
    class PerdidaDeRacha {

        @Test
        @DisplayName("Reinicia la mision si el donante pasa un mes completo sin donar")
        void reiniciaMisionPorRachaPerdida() {
            DonacionesExitosas primera = misionPrimeraDonacion();
            Racha racha = misionRacha();
            repositorio.guardarMision(primera);
            repositorio.guardarMision(racha);
            UUID donanteId = UUID.randomUUID();
            Donante donante = new Donante();
            donante.setId(donanteId);
            donante.setCategoria(new Colaborador());
            donante.cambiarMisionActual(racha);
            donante.registrarDonacion(donacionRegistradaEn(YearMonth.now().minusMonths(2).atDay(5).atTime(12, 0)));
            repositorio.guardarPerfil(donante);

            Donante perfil = service.obtenerPerfil(donanteId);

            assertThat(perfil.getProgresoMision().getMisionActual()).isSameAs(primera);
        }
    }

    @Nested
    @DisplayName("Ranking")
    class Ranking {

        @Test
        @DisplayName("Calcula la posicion actual del donante")
        void calculaPosicionActual() {
            repositorio.guardarMision(misionPrimeraDonacion());
            Donante primero = donanteConInsignia(UUID.randomUUID());
            Donante segundo = donanteConInsignia(UUID.randomUUID());
            primero.agregarInsignia(new InsigniaObtenida(insignia("Extra"), true));
            repositorio.guardarPerfil(segundo);
            repositorio.guardarPerfil(primero);

            int posicion = service.obtenerPosicionRankingActual(segundo.getId());

            assertThat(posicion).isEqualTo(2);
        }
    }

    private Donante donanteConInsignia(UUID id) {
        Donante donante = new Donante();
        donante.setId(id);
        donante.setCategoria(new Colaborador());
        donante.agregarInsignia(new InsigniaObtenida(insignia("Semilla"), true));
        return donante;
    }

    private DonacionesExitosas misionPrimeraDonacion() {
        return new DonacionesExitosas("Primera donacion exitosa", "Completar una donacion", new Colaborador(), 1, insignia("Semilla"));
    }

    private Racha misionRacha() {
        return new Racha("Racha solidaria", "Donar todos los meses", new Colaborador(), 3, insignia("Racha"));
    }

    private HabilDonador misionGranDonacion() {
        return new HabilDonador("Gran corazon", "Donar muchos bienes", new Sostenedor(), 10, insignia("Plata"));
    }

    private Completitud misionCompletitud() {
        return new Completitud("Ayuda integral", "Donar varias categorias", new Transformador(), 3, insignia("Oro"));
    }

    private Insignia insignia(String nombre) {
        return Insignia.builder().id(UUID.randomUUID()).nombre(nombre).imagen(nombre + ".png").build();
    }

    private DonacionRegistrada donacionRegistrada(int cantidadBienes, String... categorias) {
        return DonacionRegistrada.builder()
                .fecha(LocalDateTime.now())
                .cantidadBienes(cantidadBienes)
                .categorias(Set.of(categorias))
                .exitosa(false)
                .build();
    }

    private DonacionRegistrada donacionRegistradaEn(LocalDateTime fecha) {
        return DonacionRegistrada.builder()
                .fecha(fecha)
                .cantidadBienes(1)
                .categorias(Set.of("ALIMENTOS"))
                .exitosa(false)
                .build();
    }

    private DonacionRegistrada donacionRegistradaSinCategorias() {
        return DonacionRegistrada.builder()
                .fecha(LocalDateTime.now())
                .cantidadBienes(1)
                .categorias(Set.of())
                .exitosa(false)
                .build();
    }

    private DonacionRegistrada donacionExitosa(String entidadBeneficiaria) {
        return DonacionRegistrada.builder()
                .fecha(LocalDateTime.now())
                .cantidadBienes(1)
                .categorias(Set.of("ALIMENTOS"))
                .entidadBeneficiaria(entidadBeneficiaria)
                .exitosa(true)
                .build();
    }
}
