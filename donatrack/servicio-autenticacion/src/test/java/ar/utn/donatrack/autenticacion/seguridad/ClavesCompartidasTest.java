package ar.utn.donatrack.autenticacion.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Claves compartidas entre servidores")
class ClavesCompartidasTest {
    private static final String FRONT = "clave-del-frontend-de-32-caracteres!";
    private static final String SERVICIOS = "clave-de-los-servicios-de-32-caracteres";

    @Test
    @DisplayName("Sin la clave del frontend, o con una corta, el servicio no arranca y explica qué falta")
    void exigeLaClaveDelFrontendAlArrancar() {
        assertThatThrownBy(() -> new ClavesCompartidas("", ""))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("AUTH_FRONTEND_KEY");
        assertThatThrownBy(() -> new ClavesCompartidas("corta", ""))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32");
    }

    @Test
    @DisplayName("La clave entre servicios es opcional, pero si está tiene que ser larga")
    void claveDeServicios() {
        assertThatCode(() -> new ClavesCompartidas(FRONT, "")).doesNotThrowAnyException();
        assertThatThrownBy(() -> new ClavesCompartidas(FRONT, "corta"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("DONATRACK_SERVICIOS_KEY");
    }

    @Test
    @DisplayName("Sin clave de servicios configurada se rechaza todo aviso entre servicios")
    void sinClaveDeServiciosRechaza() {
        ClavesCompartidas claves = new ClavesCompartidas(FRONT, "");
        assertThatThrownBy(() -> claves.exigirServicio("")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Solo la clave exacta pasa")
    void comparaLaClave() {
        ClavesCompartidas claves = new ClavesCompartidas(FRONT, SERVICIOS);
        assertThatCode(() -> claves.exigirFrontend(FRONT, "motivo")).doesNotThrowAnyException();
        assertThatCode(() -> claves.exigirServicio(SERVICIOS)).doesNotThrowAnyException();
        assertThatThrownBy(() -> claves.exigirFrontend(SERVICIOS, "motivo")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> claves.exigirServicio(FRONT)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> claves.exigirServicio(null)).isInstanceOf(ResponseStatusException.class);
    }
}
