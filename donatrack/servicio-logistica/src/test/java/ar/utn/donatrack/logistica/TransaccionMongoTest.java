package ar.utn.donatrack.logistica;

import ar.utn.donatrack.logistica.exceptions.GlobalExceptionHandler;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Transacción de MongoDB")
class TransaccionMongoTest {

    @Autowired private MongoTemplate mongo;
    @Autowired private PlatformTransactionManager transacciones;

    @Test
    @DisplayName("si la transacción se deshace, el camión no queda guardado")
    void rollbackNoDejaElDocumento() {
        UUID id = UUID.randomUUID();
        TransactionTemplate template = new TransactionTemplate(transacciones);
        template.executeWithoutResult(status -> {
            mongo.save(Camion.builder()
                    .id(id)
                    .patente("TX" + id.toString().substring(0, 6))
                    .capacidadVolumenM3(1)
                    .alturaM(1)
                    .capacidadCargaKg(1)
                    .build());
            status.setRollbackOnly();
        });

        assertThat(mongo.findById(id, Camion.class)).isNull();
    }

    @Test
    @DisplayName("si otra transacción cambió la misma ruta, la escritura se rechaza con 409 y no pisa el cambio")
    void escrituraConcurrenteResponde409() {
        Ruta ruta = Ruta.builder()
                .id(UUID.randomUUID())
                .estado(EstadoRuta.PLANIFICADA)
                .paradas(new ArrayList<>())
                .build();
        mongo.save(ruta);

        TransactionTemplate primera = new TransactionTemplate(transacciones);
        TransactionTemplate segunda = new TransactionTemplate(transacciones);
        segunda.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // La primera lee la ruta; mientras sigue abierta, la segunda la cambia y
        // confirma. Cuando la primera intenta escribir, Mongo la rechaza.
        Throwable error = catchThrowable(() -> primera.executeWithoutResult(status -> {
            Ruta leida = mongo.findById(ruta.getId(), Ruta.class);
            segunda.executeWithoutResult(s -> {
                Ruta otra = mongo.findById(ruta.getId(), Ruta.class);
                otra.setEstado(EstadoRuta.INICIADA);
                mongo.save(otra);
            });
            leida.setEstado(EstadoRuta.FINALIZADA);
            mongo.save(leida);
        }));

        assertThat(error).isInstanceOf(DataIntegrityViolationException.class);
        ResponseEntity<Map<String, Object>> respuesta = new GlobalExceptionHandler()
                .manejarEscrituraConcurrente((DataIntegrityViolationException) error);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mongo.findById(ruta.getId(), Ruta.class).getEstado()).isEqualTo(EstadoRuta.INICIADA);
    }
}
