package ar.utn.donatrack.donaciones;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Las reglas de capas del servicio, verificadas sobre el código compilado: si un
 * cambio las rompe, falla el build en lugar de descubrirse en una revisión.
 *
 * Las reglas se evalúan desde tests comunes de JUnit (regla.check) y no con el
 * motor propio de ArchUnit, que surefire no llegaba a ejecutar.
 */
@DisplayName("Arquitectura: qué capa puede usar a cuál")
class ArquitecturaTest {

    private static final String RAIZ = "ar.utn.donatrack.donaciones";

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);

    @Test
    @DisplayName("Los controllers no usan repositorios: delegan en los services")
    void controllersSinRepositorios() {
        noClasses()
                .that().resideInAPackage(RAIZ + ".controllers..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        RAIZ + ".repositories..", RAIZ + ".interfaces.repositories..")
                .because("los controllers solo traducen HTTP y delegan en los services")
                .check(CLASES);
    }

    @Test
    @DisplayName("El dominio no conoce a las capas de afuera")
    void dominioIndependiente() {
        noClasses()
                .that().resideInAPackage(RAIZ + ".models..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        RAIZ + ".controllers..", RAIZ + ".services..", RAIZ + ".dtos..", RAIZ + ".mappers..",
                        RAIZ + ".repositories..", RAIZ + ".interfaces.repositories..", RAIZ + ".clientes..",
                        RAIZ + ".integracion..", RAIZ + ".salida..", "org.springframework.web..")
                .because("el modelo de dominio no depende de cómo se lo expone ni de cómo se lo guarda")
                .check(CLASES);
    }

    @Test
    @DisplayName("Services, oyentes y validadores usan los puertos, no los adaptadores de persistencia")
    void casosDeUsoConPuertos() {
        noClasses()
                .that().resideInAnyPackage(RAIZ + ".services..", RAIZ + ".eventos..", RAIZ + ".validations..")
                .should().dependOnClassesThat().resideInAPackage(RAIZ + ".repositories..")
                .because("los casos de uso dependen de los puertos (interfaces.repositories)")
                .check(CLASES);
    }

    @Test
    @DisplayName("Spring Data se usa solo detrás de los puertos (y en la bandeja de salida, que es infraestructura)")
    void springDataEsUnDetalle() {
        classes()
                .that().resideInAPackage(RAIZ + ".repositories.jpa..")
                .should().onlyBeAccessed().byAnyPackage(RAIZ + ".repositories..", RAIZ + ".salida..")
                .because("las interfaces de Spring Data son un detalle de los adaptadores")
                .check(CLASES);
    }

    @Test
    @DisplayName("Ni los services ni los repositorios conocen a los controllers")
    void servicesSinControllers() {
        noClasses()
                .that().resideInAnyPackage(RAIZ + ".services..", RAIZ + ".repositories..")
                .should().dependOnClassesThat().resideInAPackage(RAIZ + ".controllers..")
                .because("las capas de adentro no conocen a la de afuera")
                .check(CLASES);
    }
}
