package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
import ar.utn.donatrack.donaciones.models.donante.estado.BloqueadoState;
import ar.utn.donatrack.donaciones.models.donante.estado.InactivoState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del comportamiento propio de la Persona Donante (modelo rico).
 *
 * Cubre los métodos que se agregaron al modelo en el refactor de Entrega 3 para
 * sacarle responsabilidad al service:
 *   - cambiarEstado()       : delega en la máquina de estados y guarda el resultado
 *   - registrarInteraccion(): marca de actividad que usa la consulta de inactividad
 *
 * Se usa PersonaHumana como instancia concreta porque PersonaDonante es abstracta.
 */
@DisplayName("PersonaDonante - comportamiento del modelo rico")
class PersonaDonanteTest {

    /** Construye un donante humano ACTIVO listo para usar en cada test. */
    private PersonaHumana donanteActivo() {
        return PersonaHumana.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .apellido("Pérez")
                .documento(Documento.de("DNI", "30111222"))
                .email("juan.perez@example.com")
                .build();
    }

    @Nested
    @DisplayName("cambiarEstado() - delega en la máquina de estados")
    class CambiarEstado {

        @Test
        @DisplayName("Dar de baja a un donante ACTIVO lo deja INACTIVO")
        void darDeBaja() {
            PersonaHumana donante = donanteActivo();

            donante.cambiarEstado("INACTIVO", null);

            assertThat(donante.getEstado()).isInstanceOf(InactivoState.class);
            assertThat(donante.getEstado().nombre()).isEqualTo("INACTIVO");
        }

        @Test
        @DisplayName("Bloquear a un donante ACTIVO con justificación lo deja BLOQUEADO")
        void bloquearConJustificacion() {
            PersonaHumana donante = donanteActivo();

            donante.cambiarEstado("BLOQUEADO", "Reportado por uso indebido de la plataforma");

            assertThat(donante.getEstado()).isInstanceOf(BloqueadoState.class);
        }

        @Test
        @DisplayName("Reactivar a un donante INACTIVO lo devuelve a ACTIVO")
        void reactivar() {
            PersonaHumana donante = donanteActivo();
            donante.cambiarEstado("INACTIVO", null);

            donante.cambiarEstado("ACTIVO", null);

            assertThat(donante.getEstado()).isInstanceOf(ActivoState.class);
        }

        @Test
        @DisplayName("Un cambio ilegal deja al donante en su estado anterior")
        void cambioIlegalNoMutaAlDonante() {
            // Se comprueba que la asignación al campo `estado` solo ocurre si la
            // transición fue aceptada: la excepción se lanza antes del assignment.
            PersonaHumana donante = donanteActivo();
            donante.cambiarEstado("INACTIVO", null);

            assertThatThrownBy(() -> donante.cambiarEstado("BLOQUEADO", "spam"))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);

            assertThat(donante.getEstado()).isInstanceOf(InactivoState.class);
        }

        @Test
        @DisplayName("Bloquear sin justificación falla y deja al donante ACTIVO")
        void bloquearSinJustificacionNoMuta() {
            PersonaHumana donante = donanteActivo();

            assertThatThrownBy(() -> donante.cambiarEstado("BLOQUEADO", null))
                    .isInstanceOf(FaltaJustificacionException.class);

            assertThat(donante.getEstado()).isInstanceOf(ActivoState.class);
        }

        @Test
        @DisplayName("Cambiar al mismo estado en el que ya está se rechaza")
        void mismoEstadoSeRechaza() {
            PersonaHumana donante = donanteActivo();

            assertThatThrownBy(() -> donante.cambiarEstado("ACTIVO", null))
                    .isInstanceOf(PersonaConMismoEstadoException.class);
        }
    }

    @Nested
    @DisplayName("registrarInteraccion() - marca de actividad")
    class SeguimientoDeInactividad {

        @Test
        @DisplayName("registrarInteraccion() sella la última actividad con la hora argentina actual")
        void registrarInteraccionSellaLaHora() {
            // Este sello es el que después consulta el proceso que detecta donantes
            // inactivos para enviarles la notificación de reactivación.
            PersonaHumana donante = donanteActivo();
            assertThat(donante.getUltimaInteraccion()).isNull();

            LocalDateTime antes = LocalDateTime.now().minusMinutes(1);
            donante.registrarInteraccion();

            assertThat(donante.getUltimaInteraccion()).isAfter(antes);
        }
    }
}
