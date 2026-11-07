package ar.utn.donatrack.logistica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Una operación que escribe dos documentos (ruta y camión, lote y rutas)
 * entra en esta transacción. Exige que MongoDB sea un replica set.
 */
@Configuration
public class TransaccionMongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
