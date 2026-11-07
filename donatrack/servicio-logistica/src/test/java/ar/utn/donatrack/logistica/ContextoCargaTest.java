package ar.utn.donatrack.logistica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el contexto de Spring cargue.
 *
 * POR QUÉ EXISTE: dos servicios del sistema (notificaciones e incentivos)
 * estuvieron semanas sin poder arrancar y nadie lo notó, porque todos los tests
 * eran unitarios con mocks y ninguno levantaba el contexto. Un fallo de
 * configuración —una property sin definir, un datasource sin driver— no se
 * detecta de ninguna otra forma que levantando Spring.
 *
 * Cuesta diez líneas y corre en la build de todos los días.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Carga del contexto de Spring")
class ContextoCargaTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("El contexto levanta sin errores")
    void elContextoCarga() {
        assertThat(contexto).isNotNull();
    }
}
