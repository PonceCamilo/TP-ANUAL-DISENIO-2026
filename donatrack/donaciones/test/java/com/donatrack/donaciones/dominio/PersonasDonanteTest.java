package com.donatrack.donaciones.dominio;

import com.donatrack.donaciones.dominio.contacto.Email;
import com.donatrack.donaciones.dominio.contacto.Telefono;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PersonaHumana y PersonaJuridica - validaciones de dominio")
class PersonasDonanteTest {

    private static final Direccion DIRECCION =
            new Direccion("Corrientes", "1000", "CABA", "Buenos Aires", "1043");

    // ─── PersonaHumana ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PersonaHumana")
    class PersonaHumanaTests {

        @Test
        @DisplayName("Se construye correctamente con datos válidos")
        void seConstruyeCorrectamente() {
            PersonaHumana p = humanaValida();
            assertEquals("Ana", p.getNombre());
            assertEquals("Pérez", p.getApellido());
            assertEquals("Ana Pérez", p.getNombreCompleto());
            assertEquals(30, p.getEdad());
            assertEquals("12345678", p.getNumeroDocumento());
            assertEquals(Genero.FEMENINO, p.getGenero());
        }

        @Test
        @DisplayName("Lanza excepción si el nombre está vacío")
        void nombreVacioLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaHumanaBuilder()
                            .nombre("").apellido("Pérez").edad(30)
                            .numeroDocumento("12345678").genero(Genero.FEMENINO)
                            .direccion(DIRECCION).agregarMedio(new Email("a@b.com"))
                            .build()
            );
        }

        @Test
        @DisplayName("Lanza excepción si la edad es negativa")
        void edadNegativaLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaHumanaBuilder()
                            .nombre("Ana").apellido("Pérez").edad(-1)
                            .numeroDocumento("12345678").genero(Genero.FEMENINO)
                            .direccion(DIRECCION).agregarMedio(new Email("a@b.com"))
                            .build()
            );
        }

        @Test
        @DisplayName("Lanza excepción si la edad supera 120")
        void edadMayorA120LanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaHumanaBuilder()
                            .nombre("Ana").apellido("Pérez").edad(121)
                            .numeroDocumento("12345678").genero(Genero.FEMENINO)
                            .direccion(DIRECCION).agregarMedio(new Email("a@b.com"))
                            .build()
            );
        }

        @Test
        @DisplayName("Lanza excepción si el documento está vacío")
        void documentoVacioLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaHumanaBuilder()
                            .nombre("Ana").apellido("Pérez").edad(30)
                            .numeroDocumento("").genero(Genero.FEMENINO)
                            .direccion(DIRECCION).agregarMedio(new Email("a@b.com"))
                            .build()
            );
        }

        @Test
        @DisplayName("actualizarDatos() actualiza los campos correctamente")
        void actualizarDatosActualizaCampos() {
            PersonaHumana p = humanaValida();
            p.actualizarDatos("Beatriz", "Lopez", 35, Genero.FEMENINO, DIRECCION);
            assertEquals("Beatriz", p.getNombre());
            assertEquals("Lopez", p.getApellido());
            assertEquals(35, p.getEdad());
        }

        private PersonaHumana humanaValida() {
            return new PersonaHumanaBuilder()
                    .nombre("Ana").apellido("Pérez").edad(30)
                    .numeroDocumento("12345678").genero(Genero.FEMENINO)
                    .direccion(DIRECCION).agregarMedio(new Email("ana@mail.com"))
                    .build();
        }
    }

    // ─── PersonaJuridica ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("PersonaJuridica")
    class PersonaJuridicaTests {

        @Test
        @DisplayName("Se construye correctamente con datos válidos")
        void seConstruyeCorrectamente() {
            PersonaJuridica pj = juridicaValida();
            assertEquals("Arcos Plateados S.A.", pj.getRazonSocial());
            assertEquals(TipoPersonaJuridica.EMPRESA, pj.getTipo());
            assertEquals("Tecnología", pj.getRubro());
            assertEquals(1, pj.getRepresentantes().size());
        }

        @Test
        @DisplayName("Lanza excepción si la razón social está vacía")
        void razonSocialVaciaLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaJuridicaBuilder()
                            .razonSocial("").tipo(TipoPersonaJuridica.EMPRESA).rubro("Tech")
                            .medios(List.of(new Email("co@emp.com")))
                            .agregarRepresentante(repValido())
                            .build()
            );
        }

        @Test
        @DisplayName("Lanza excepción si no tiene representantes")
        void sinRepresentantesLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PersonaJuridicaBuilder()
                            .razonSocial("MiEmpresa").tipo(TipoPersonaJuridica.ONG).rubro("Social")
                            .medios(List.of(new Email("co@emp.com")))
                            .build()
            );
        }

        @Test
        @DisplayName("Puede agregar un representante adicional")
        void puedeAgregarRepresentante() {
            PersonaJuridica pj = juridicaValida();
            pj.agregarRepresentante(new Representante("Luis", "Gomez", "luis@emp.com", null));
            assertEquals(2, pj.getRepresentantes().size());
        }

        @Test
        @DisplayName("No permite dos representantes con el mismo email")
        void noPermiteRepresentanteDuplicado() {
            PersonaJuridica pj = juridicaValida();
            assertThrows(IllegalArgumentException.class, () ->
                    pj.agregarRepresentante(new Representante("Otro", "Nombre", "rep@emp.com", null))
            );
        }

        @Test
        @DisplayName("removerRepresentante() elimina correctamente")
        void removerRepresentanteElimina() {
            PersonaJuridica pj = new PersonaJuridicaBuilder()
                    .razonSocial("Empresa").tipo(TipoPersonaJuridica.EMPRESA).rubro("Tech")
                    .medios(List.of(new Email("co@emp.com")))
                    .agregarRepresentante(new Representante("Ana", "P", "ana@emp.com", null))
                    .agregarRepresentante(new Representante("Luis", "G", "luis@emp.com", null))
                    .build();

            pj.removerRepresentante("ana@emp.com");
            assertEquals(1, pj.getRepresentantes().size());
        }

        @Test
        @DisplayName("removerRepresentante() lanza excepción si quedaría sin representantes")
        void removerUnicoRepresentanteLanzaExcepcion() {
            PersonaJuridica pj = juridicaValida();
            assertThrows(IllegalStateException.class, () ->
                    pj.removerRepresentante("rep@emp.com")
            );
        }

        @Test
        @DisplayName("getRepresentantes() devuelve lista inmutable")
        void representantesEsListaInmutable() {
            PersonaJuridica pj = juridicaValida();
            assertThrows(UnsupportedOperationException.class, () ->
                    pj.getRepresentantes().add(repValido())
            );
        }

        private PersonaJuridica juridicaValida() {
            return new PersonaJuridicaBuilder()
                    .razonSocial("Arcos Plateados S.A.")
                    .tipo(TipoPersonaJuridica.EMPRESA)
                    .rubro("Tecnología")
                    .medios(List.of(new Email("contacto@arcos.com")))
                    .agregarRepresentante(repValido())
                    .build();
        }

        private Representante repValido() {
            return new Representante("Carlos", "Ruiz", "rep@emp.com", "+5411000");
        }
    }
}
