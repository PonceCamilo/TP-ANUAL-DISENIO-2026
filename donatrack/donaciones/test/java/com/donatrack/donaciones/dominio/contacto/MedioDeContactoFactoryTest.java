package com.donatrack.donaciones.dominio.contacto;

import com.donatrack.donaciones.dominio.contacto.factory.MedioDeContactoFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MedioDeContactoFactory - Patrón FACTORY METHOD")
class MedioDeContactoFactoryTest {

    @Test
    @DisplayName("Crea un Email con tipo EMAIL")
    void creaEmail() {
        MedioDeContacto medio = MedioDeContactoFactory.crear(TipoMedioContacto.EMAIL, "a@b.com");
        assertInstanceOf(Email.class, medio);
        assertEquals(TipoMedioContacto.EMAIL, medio.getTipo());
        assertEquals("a@b.com", medio.getValor());
    }

    @Test
    @DisplayName("Crea un Telefono con tipo TELEFONO")
    void creaTelefono() {
        MedioDeContacto medio = MedioDeContactoFactory.crear(TipoMedioContacto.TELEFONO, "+5411555");
        assertInstanceOf(Telefono.class, medio);
        assertEquals(TipoMedioContacto.TELEFONO, medio.getTipo());
        assertEquals("+5411555", medio.getValor());
    }

    @Test
    @DisplayName("Crea un WhatsApp con tipo WHATSAPP")
    void creaWhatsApp() {
        MedioDeContacto medio = MedioDeContactoFactory.crear(TipoMedioContacto.WHATSAPP, "+5411999");
        assertInstanceOf(WhatsApp.class, medio);
        assertEquals(TipoMedioContacto.WHATSAPP, medio.getTipo());
        assertEquals("+5411999", medio.getValor());
    }

    @ParameterizedTest(name = "tipo={0}, valor={1}")
    @DisplayName("El valor del medio nunca es nulo o vacío tras la creación")
    @CsvSource({
            "EMAIL,    ana@mail.com",
            "TELEFONO, +541155555555",
            "WHATSAPP, +541166666666"
    })
    void valorNuncaEsNuloOVacio(TipoMedioContacto tipo, String valor) {
        MedioDeContacto medio = MedioDeContactoFactory.crear(tipo, valor.trim());
        assertNotNull(medio.getValor());
        assertFalse(medio.getValor().isBlank());
    }

    @Test
    @DisplayName("Lanza excepción si el valor está vacío")
    void lanzaExcepcionConValorVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                MedioDeContactoFactory.crear(TipoMedioContacto.EMAIL, "")
        );
    }

    @Test
    @DisplayName("Lanza excepción si el valor es nulo")
    void lanzaExcepcionConValorNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                MedioDeContactoFactory.crear(TipoMedioContacto.EMAIL, null)
        );
    }
}
