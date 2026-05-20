package com.donatrack.donaciones.dominio;

import com.donatrack.donaciones.dominio.contacto.Email;
import com.donatrack.donaciones.dominio.contacto.MedioDeContacto;
import com.donatrack.donaciones.dominio.contacto.Telefono;
import com.donatrack.donaciones.dominio.contacto.WhatsApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PersonaDonante")
class PersonaDonanteTest {

    // Usamos PersonaHumana como clase concreta para testear la lógica de PersonaDonante
    private PersonaHumana donante;

    @BeforeEach
    void setUp() {
        donante = new PersonaHumanaBuilder()
                .nombre("Ana")
                .apellido("Pérez")
                .edad(30)
                .numeroDocumento("12345678")
                .genero(Genero.FEMENINO)
                .direccion(new Direccion("Av. Corrientes", "1234", "CABA", "Buenos Aires", "1043"))
                .agregarMedio(new Email("ana@mail.com"))
                .build();
    }

    // ─── Estado inicial ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Estado inicial al registrarse")
    class EstadoInicial {

        @Test
        @DisplayName("Se crea con estado ACTIVO")
        void seCreaConEstadoActivo() {
            assertEquals(EstadoDonante.ACTIVO, donante.getEstado());
        }

        @Test
        @DisplayName("isActivo() devuelve true cuando el estado es ACTIVO")
        void isActivoEsTrueCuandoEstaActivo() {
            assertTrue(donante.isActivo());
        }

        @Test
        @DisplayName("El email es el medio de contacto predeterminado por defecto")
        void emailEsMedioPredeterminadoPorDefecto() {
            assertEquals("ana@mail.com", donante.getMedioContactoPredeterminado().getValor());
        }

        @Test
        @DisplayName("Se genera un ID no nulo")
        void tieneIdNoNulo() {
            assertNotNull(donante.getId());
        }
    }

    // ─── Patrón STATE: ciclo de vida ──────────────────────────────────────────

    @Nested
    @DisplayName("Patrón STATE - ciclo de vida del donante")
    class CicloDeVida {

        @Test
        @DisplayName("darDeBaja() cambia el estado a INACTIVO")
        void darDeBajaCambiaEstadoAInactivo() {
            donante.darDeBaja();
            assertEquals(EstadoDonante.INACTIVO, donante.getEstado());
            assertFalse(donante.isActivo());
        }

        @Test
        @DisplayName("darDeBaja() lanza excepción si ya está INACTIVO")
        void darDeBajaDobleVezLanzaExcepcion() {
            donante.darDeBaja();
            assertThrows(IllegalStateException.class, () -> donante.darDeBaja());
        }

        @Test
        @DisplayName("reactivar() cambia el estado a ACTIVO")
        void reactivarCambiaEstadoAActivo() {
            donante.darDeBaja();
            donante.reactivar();
            assertEquals(EstadoDonante.ACTIVO, donante.getEstado());
            assertTrue(donante.isActivo());
        }
    }

    // ─── Invariante: debe tener email ─────────────────────────────────────────

    @Nested
    @DisplayName("Invariante: debe tener al menos un EMAIL")
    class InvarianteEmail {

        @Test
        @DisplayName("Lanza excepción si no se pasa ningún medio de contacto")
        void lanzaExcepcionSinMedios() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaHumanaBuilder()
                            .nombre("Juan").apellido("Gomez").edad(25)
                            .numeroDocumento("99999999").genero(Genero.MASCULINO)
                            .direccion(new Direccion("Callao", "1", "CABA", "BA", "1022"))
                            .build()
            );
        }

        @Test
        @DisplayName("Lanza excepción si solo hay teléfono y no email")
        void lanzaExcepcionSiSoloHayTelefono() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaHumanaBuilder()
                            .nombre("Juan").apellido("Gomez").edad(25)
                            .numeroDocumento("99999999").genero(Genero.MASCULINO)
                            .direccion(new Direccion("Callao", "1", "CABA", "BA", "1022"))
                            .agregarMedio(new Telefono("+5411555"))
                            .build()
            );
        }
    }

    // ─── Medios de contacto ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Gestión de medios de contacto")
    class MediosDeContacto {

        @Test
        @DisplayName("Puede agregar un teléfono opcional")
        void puedeAgregarTelefono() {
            donante.agregarMedioDeContacto(new Telefono("+5411555"));
            assertEquals(2, donante.getMediosDeContacto().size());
        }

        @Test
        @DisplayName("No permite duplicar el mismo medio y valor")
        void noPermiteDuplicados() {
            assertThrows(IllegalArgumentException.class, () ->
                    donante.agregarMedioDeContacto(new Email("ana@mail.com"))
            );
        }

        @Test
        @DisplayName("Puede cambiar el medio de contacto predeterminado a WhatsApp")
        void puedeCambiarMedioPredeterminado() {
            WhatsApp wp = new WhatsApp("+5411999");
            donante.agregarMedioDeContacto(wp);
            donante.establecerMedioContactoPredeterminado(wp);
            assertEquals("+5411999", donante.getMedioContactoPredeterminado().getValor());
        }

        @Test
        @DisplayName("No puede establecer como predeterminado un medio no registrado")
        void noPermitePredeterminadoNoRegistrado() {
            WhatsApp noRegistrado = new WhatsApp("+5411000");
            assertThrows(IllegalArgumentException.class, () ->
                    donante.establecerMedioContactoPredeterminado(noRegistrado)
            );
        }

        @Test
        @DisplayName("getMediosDeContacto() devuelve lista inmutable")
        void listaDeMediosEsInmutable() {
            List<MedioDeContacto> medios = donante.getMediosDeContacto();
            assertThrows(UnsupportedOperationException.class, () ->
                    medios.add(new Telefono("+5411111"))
            );
        }
    }
}
