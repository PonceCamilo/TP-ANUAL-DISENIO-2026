package ar.utn.donatrack.notificaciones.repositories;

import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.Discord;
import ar.utn.donatrack.notificaciones.model.medios.Email;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.medios.Sms;
import ar.utn.donatrack.notificaciones.model.medios.WhatsApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la persistencia de notificaciones (Entrega 4).
 *
 * POR QUÉ EXISTEN: hasta la Entrega 4 el historial vivía en un
 * ConcurrentHashMap y se perdía en cada reinicio, lo que vuelve inútil
 * cualquier auditoría de qué se mandó y qué falló. Ahora hay una tabla, y con
 * ella aparecen los problemas propios del mapeo.
 *
 * EL PUNTO DELICADO ES EL MEDIO. MedioNotificacion es una jerarquía de clases,
 * no un enum, y esos objetos NO se persisten: en la base solo va el nombre
 * ("EMAIL", "SMS", "WHATSAPP", "DISCORD") y el objeto se reconstruye en
 * @PostLoad. Si el nombre y la clase se desalinean, la notificación se guarda
 * bien y falla AL LEERLA, que es el peor momento posible.
 *
 * Cada test guarda, vacía la caché de persistencia y vuelve a leer. Sin ese
 * clear(), find() devuelve la misma instancia que quedó en memoria, @PostLoad
 * nunca corre y el test pasaría sin probar nada.
 */
@DataJpaTest
@Import(NotificacionRepository.class)
@DisplayName("Persistencia de notificaciones")
class NotificacionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificacionRepository repositorio;

    private void vaciarCache() {
        em.flush();
        em.clear();
    }

    private Notificacion notificacion(MedioNotificacion medio) {
        return Notificacion.builder()
                .id(UUID.randomUUID())
                .destinatario("juan@example.com")
                .mensaje("Tu donación fue asignada a Comedor Los Andes.")
                .medio(medio)
                .estado(EstadoNotificacion.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    // ── Ida y vuelta básica ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Guardar y recuperar")
    class IdaYVuelta {

        @Test
        @DisplayName("Una notificación guardada se recupera por su id")
        void seGuardaYSeRecupera() {
            Notificacion original = notificacion(new Email());
            repositorio.guardar(original);
            vaciarCache();

            Notificacion recuperada = repositorio.buscarPorId(original.getId());

            assertThat(recuperada).isNotNull();
            assertThat(recuperada.getDestinatario()).isEqualTo("juan@example.com");
            assertThat(recuperada.getMensaje()).isEqualTo("Tu donación fue asignada a Comedor Los Andes.");
        }

        @Test
        @DisplayName("Buscar un id inexistente devuelve null, no una excepción")
        void idInexistenteDevuelveNull() {
            // El adapter devuelve null y no Optional para no cambiar el contrato
            // que ya usaba el service, que traduce ese null a
            // NotificacionNoEncontradaException.
            assertThat(repositorio.buscarPorId(UUID.randomUUID())).isNull();
        }

        @Test
        @DisplayName("buscarTodas devuelve el historial completo")
        void historialCompleto() {
            // Es la consulta de auditoría: qué se mandó y qué falló. Antes de la
            // Entrega 4 devolvía solo lo de la sesión en curso.
            repositorio.guardar(notificacion(new Email()));
            repositorio.guardar(notificacion(new Sms()));
            vaciarCache();

            assertThat(repositorio.buscarTodas()).hasSize(2);
        }

        @Test
        @DisplayName("Un mensaje largo entra completo, sin truncarse")
        void mensajeLargo() {
            // La columna es length = 2000 justamente porque el default de 255 no
            // alcanza: los mensajes incluyen nombres de entidades y URLs de mapas.
            String largo = "x".repeat(1500);
            Notificacion original = Notificacion.builder()
                    .id(UUID.randomUUID())
                    .destinatario("juan@example.com")
                    .mensaje(largo)
                    .medio(new Email())
                    .estado(EstadoNotificacion.PENDIENTE)
                    .fechaCreacion(LocalDateTime.now())
                    .build();

            repositorio.guardar(original);
            vaciarCache();

            assertThat(repositorio.buscarPorId(original.getId()).getMensaje()).hasSize(1500);
        }
    }

    // ── El medio: lo que de verdad puede romperse ─────────────────────────────

    @Nested
    @DisplayName("Rehidratación del medio")
    class RehidratacionDelMedio {

        /** Los cuatro medios que el servicio puede usar, según los notificadores registrados. */
        static Stream<MedioNotificacion> todosLosMedios() {
            return Stream.of(new Email(), new Sms(), new WhatsApp(), new Discord());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("todosLosMedios")
        @DisplayName("Cada medio sobrevive a la ida y vuelta y vuelve con su clase concreta")
        void cadaMedioSeRehidrata(MedioNotificacion medio) {
            // ESTE TEST ENCONTRÓ UN BUG REAL: MediosNotificacion mapeaba solo
            // EMAIL, SMS y WHATSAPP, pero DiscordNotificadorWebhook está
            // registrado y devuelve new Discord(). Una notificación por Discord
            // se guardaba sin problema y reventaba AL LEERLA, con
            // IllegalArgumentException desde el @PostLoad. Peor: como
            // buscarTodas() las carga todas, una sola fila de Discord rompía el
            // historial entero.
            Notificacion original = notificacion(medio);
            repositorio.guardar(original);
            vaciarCache();

            Notificacion recuperada = repositorio.buscarPorId(original.getId());

            assertThat(recuperada.getMedio())
                    .as("El medio no se pudo reconstruir: falta en el mapa de MediosNotificacion")
                    .isNotNull();
            assertThat(recuperada.getMedio().getNombre()).isEqualTo(medio.getNombre());
            assertThat(recuperada.getMedio()).isInstanceOf(medio.getClass());
        }

        @Test
        @DisplayName("El builder completa el nombre del medio, que es la columna NOT NULL")
        void elBuilderCompletaElNombre() {
            // Notificacion tiene un setter de builder escrito a mano para `medio`
            // que además llena `medioNombre`. Sin él, Lombok generaría el suyo,
            // la columna quedaría en null y TODO insert fallaría. El test mira la
            // columna directamente, no el objeto.
            Notificacion original = notificacion(new WhatsApp());
            repositorio.guardar(original);
            vaciarCache();

            String nombreEnLaColumna = (String) em.getEntityManager()
                    .createQuery("select n.medioNombre from Notificacion n where n.id = :id")
                    .setParameter("id", original.getId())
                    .getSingleResult();

            assertThat(nombreEnLaColumna).isEqualTo("WHATSAPP");
        }

        @Test
        @DisplayName("setMedio mantiene sincronizados el objeto y la columna")
        void setMedioSincroniza() {
            // Si setMedio solo cambiara el @Transient, la fila quedaría con el
            // medio viejo y al releerla aparecería el anterior.
            Notificacion original = notificacion(new Email());
            original.setMedio(new Sms());
            repositorio.guardar(original);
            vaciarCache();

            assertThat(repositorio.buscarPorId(original.getId()).getMedio()).isInstanceOf(Sms.class);
        }
    }

    // ── Estados ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Estado del envío")
    class Estados {

        @Test
        @DisplayName("Una notificación enviada queda ENVIADA y con fecha de envío")
        void enviadaConFecha() {
            Notificacion original = notificacion(new Email());
            original.marcarEnviada();
            repositorio.guardar(original);
            vaciarCache();

            Notificacion recuperada = repositorio.buscarPorId(original.getId());

            assertThat(recuperada.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
            assertThat(recuperada.getFechaEnvio()).isNotNull();
        }

        @Test
        @DisplayName("Una notificación fallida queda FALLIDA y sin fecha de envío")
        void fallidaSinFecha() {
            // Es el caso que se vio en Docker: sin credenciales SMTP reales, el
            // envío falla y la fila queda como FALLIDA. Que no tenga fechaEnvio
            // es lo que permite distinguir "no se mandó" de "se mandó y no llegó".
            Notificacion original = notificacion(new Email());
            original.marcarFallida();
            repositorio.guardar(original);
            vaciarCache();

            Notificacion recuperada = repositorio.buscarPorId(original.getId());

            assertThat(recuperada.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
            assertThat(recuperada.getFechaEnvio()).isNull();
        }

        @Test
        @DisplayName("El estado se guarda por nombre y no por posición en el enum")
        void estadoComoTexto() {
            // @Enumerated(STRING): si fuera ORDINAL, agregar un valor al enum
            // reinterpretaría en silencio todas las filas viejas. Con el nombre,
            // reordenar el enum no cambia nada.
            Notificacion original = notificacion(new Email());
            original.marcarEnviada();
            repositorio.guardar(original);
            vaciarCache();

            Object enLaColumna = em.getEntityManager()
                    .createNativeQuery("select estado from notificacion where id = :id")
                    .setParameter("id", original.getId())
                    .getSingleResult();

            assertThat(enLaColumna).hasToString("ENVIADA");
        }

        @Test
        @DisplayName("Guardar dos veces la misma notificación actualiza la fila, no crea otra")
        void segundoGuardadoActualiza() {
            // El service guarda DOS VECES: una como PENDIENTE y otra después de
            // intentar el envío. Si el id no se respetara, quedarían dos filas
            // por cada notificación y el historial contaría doble.
            Notificacion original = notificacion(new Email());
            repositorio.guardar(original);
            original.marcarEnviada();
            repositorio.guardar(original);
            vaciarCache();

            List<Notificacion> todas = repositorio.buscarTodas();

            assertThat(todas).hasSize(1);
            assertThat(todas.get(0).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        }
    }
}
