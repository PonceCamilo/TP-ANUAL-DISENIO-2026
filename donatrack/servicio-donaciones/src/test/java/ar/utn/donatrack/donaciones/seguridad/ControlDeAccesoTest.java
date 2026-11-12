package ar.utn.donatrack.donaciones.seguridad;

import ar.utn.donatrack.donaciones.exceptions.seguridadExceptions.CuentaConPerfilException;
import ar.utn.donatrack.donaciones.exceptions.seguridadExceptions.CuentaSinPerfilException;
import ar.utn.donatrack.donaciones.exceptions.seguridadExceptions.RecursoAjenoException;
import ar.utn.donatrack.donaciones.util.FiltroDonaciones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ControlDeAcceso: cada persona opera solo sobre lo suyo")
class ControlDeAccesoTest {

    private final ControlDeAcceso acceso = new ControlDeAcceso();

    private final UUID yo = UUID.randomUUID();
    private final UUID otro = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void comoCuenta(String rol, String email, UUID perfilId, String tipoDonante) {
        Jwt.Builder token = Jwt.withTokenValue("token").header("alg", "RS256")
                .subject("cuenta").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", rol).claim("email", email);
        if (perfilId != null) {
            token.claim("perfilId", perfilId.toString());
        }
        if (tipoDonante != null) {
            token.claim("tipoDonante", tipoDonante);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token.build()));
    }

    @Test
    @DisplayName("La identidad sale de los claims del token, con el email normalizado")
    void identidad() {
        comoCuenta(Roles.DONANTE, " Juan@Example.COM ", yo, "HUMANA");

        assertThat(acceso.actual()).isEqualTo(new Identidad("juan@example.com", Roles.DONANTE, yo, "HUMANA"));
    }

    @Test
    @DisplayName("Sin token no hay identidad: no se permite nada")
    void sinToken() {
        assertThat(acceso.actual().rol()).isNull();
        assertThatThrownBy(() -> acceso.exigirDonante(yo)).isInstanceOf(RecursoAjenoException.class);
        assertThatThrownBy(() -> acceso.limitarAlUsuario(new FiltroDonaciones(null, null, null, null, false)))
                .isInstanceOf(RecursoAjenoException.class);
    }

    @Nested
    @DisplayName("Administración")
    class Administracion {

        @Test
        @DisplayName("Puede todo y el listado no se limita")
        void puedeTodo() {
            comoCuenta(Roles.ADMINISTRADOR, "admin@donatrack.org", null, null);
            FiltroDonaciones filtro = new FiltroDonaciones("EN_DEPOSITO", otro, null, null, false);

            assertThatCode(() -> {
                acceso.exigirDonante(otro);
                acceso.exigirEntidad(otro);
                acceso.exigirVerDonacion(otro, otro);
                acceso.exigirAltaDeDonante("cualquiera@example.com", "JURIDICA");
                acceso.exigirAltaDeEntidad(List.of());
            }).doesNotThrowAnyException();
            assertThat(acceso.limitarAlUsuario(filtro)).isEqualTo(filtro);
        }
    }

    @Nested
    @DisplayName("Persona donante")
    class Donante {

        @Test
        @DisplayName("Opera sobre su perfil y no sobre el de otra persona")
        void suPerfil() {
            comoCuenta(Roles.DONANTE, "juan@example.com", yo, "HUMANA");

            assertThatCode(() -> acceso.exigirDonante(yo)).doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirDonante(otro)).isInstanceOf(RecursoAjenoException.class);
            assertThatThrownBy(() -> acceso.exigirEntidad(yo)).isInstanceOf(RecursoAjenoException.class);
        }

        @Test
        @DisplayName("El listado se limita a sus donaciones y conserva los demás filtros")
        void listado() {
            comoCuenta(Roles.DONANTE, "juan@example.com", yo, "HUMANA");

            assertThat(acceso.limitarAlUsuario(new FiltroDonaciones("ENTREGADA", null, "arroz", null, false)))
                    .isEqualTo(new FiltroDonaciones("ENTREGADA", yo, "arroz", null, false));
            assertThatThrownBy(() -> acceso.limitarAlUsuario(new FiltroDonaciones(null, otro, null, null, false)))
                    .isInstanceOf(RecursoAjenoException.class);
        }

        @Test
        @DisplayName("Ve el detalle de sus donaciones, aunque estén asignadas a una entidad")
        void detalle() {
            comoCuenta(Roles.DONANTE, "juan@example.com", yo, "HUMANA");

            assertThatCode(() -> acceso.exigirVerDonacion(yo, otro)).doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirVerDonacion(otro, null)).isInstanceOf(RecursoAjenoException.class);
        }

        @Test
        @DisplayName("Sin perfil, se le pide completar el registro")
        void sinPerfil() {
            comoCuenta(Roles.DONANTE, "juan@example.com", null, "HUMANA");

            assertThatThrownBy(() -> acceso.exigirDonante(yo)).isInstanceOf(CuentaSinPerfilException.class);
            assertThatThrownBy(() -> acceso.limitarAlUsuario(new FiltroDonaciones(null, null, null, null, false)))
                    .isInstanceOf(CuentaSinPerfilException.class);
        }

        @Test
        @DisplayName("Registra su perfil con su email y su tipo, una sola vez")
        void altaDePerfil() {
            comoCuenta(Roles.DONANTE, "juan@example.com", null, "HUMANA");
            assertThatCode(() -> acceso.exigirAltaDeDonante("JUAN@example.com", "HUMANA")).doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirAltaDeDonante("otra@example.com", "HUMANA"))
                    .isInstanceOf(RecursoAjenoException.class);
            assertThatThrownBy(() -> acceso.exigirAltaDeDonante("juan@example.com", "JURIDICA"))
                    .isInstanceOf(RecursoAjenoException.class);

            comoCuenta(Roles.DONANTE, "juan@example.com", yo, "HUMANA");
            assertThatThrownBy(() -> acceso.exigirAltaDeDonante("juan@example.com", "HUMANA"))
                    .isInstanceOf(CuentaConPerfilException.class);
        }
    }

    @Nested
    @DisplayName("Entidad beneficiaria")
    class Entidad {

        @Test
        @DisplayName("Opera sobre su entidad y ve las donaciones asignadas a ella")
        void suEntidad() {
            comoCuenta(Roles.ENTIDAD_BENEFICIARIA, "comedor@example.org", yo, null);

            assertThatCode(() -> acceso.exigirEntidad(yo)).doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirEntidad(otro)).isInstanceOf(RecursoAjenoException.class);
            assertThatCode(() -> acceso.exigirVerDonacion(otro, yo)).doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirVerDonacion(otro, null)).isInstanceOf(RecursoAjenoException.class);
            assertThat(acceso.limitarAlUsuario(new FiltroDonaciones(null, null, null, null, false)))
                    .isEqualTo(new FiltroDonaciones(null, null, null, yo, false));
        }

        @Test
        @DisplayName("Registra su entidad si tiene el email de la cuenta como contacto o representante")
        void altaDeEntidad() {
            comoCuenta(Roles.ENTIDAD_BENEFICIARIA, "comedor@example.org", null, null);

            assertThatCode(() -> acceso.exigirAltaDeEntidad(List.of("otra@example.org", "Comedor@Example.org")))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirAltaDeEntidad(List.of("otra@example.org")))
                    .isInstanceOf(RecursoAjenoException.class);
        }

        @Test
        @DisplayName("Una persona donante no puede registrar una entidad")
        void donanteNoRegistraEntidades() {
            comoCuenta(Roles.DONANTE, "comedor@example.org", null, "HUMANA");

            assertThatThrownBy(() -> acceso.exigirAltaDeEntidad(List.of("comedor@example.org")))
                    .isInstanceOf(RecursoAjenoException.class);
        }
    }
}
