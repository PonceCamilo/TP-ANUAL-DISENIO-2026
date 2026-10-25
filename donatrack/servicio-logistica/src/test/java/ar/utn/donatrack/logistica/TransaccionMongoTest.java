package ar.utn.donatrack.logistica;

import ar.utn.donatrack.logistica.models.flota.Camion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
