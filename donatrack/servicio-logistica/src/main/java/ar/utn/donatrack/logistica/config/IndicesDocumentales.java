package ar.utn.donatrack.logistica.config;

import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Índices de lotes y rutas. El de la patente del camión lo declara @Indexed
 * en la clase Camion; repetirlo acá chocaría con el nombre que ya crea.
 */
@Configuration
public class IndicesDocumentales {

    @Bean
    ApplicationRunner indicesDeLotesYRutas(MongoTemplate mongo) {
        return args -> asegurar(mongo);
    }

    public static void asegurar(MongoTemplate mongo) {
        mongo.indexOps(LotePlanificacion.class).ensureIndex(
                new Index().on("tokenCorrelacion", Sort.Direction.ASC).unique());
        mongo.indexOps(Ruta.class).ensureIndex(new Index().on("camionId", Sort.Direction.ASC));
        mongo.indexOps(Ruta.class).ensureIndex(new Index().on("paradas.entregas.id", Sort.Direction.ASC));
        mongo.indexOps(Ruta.class).ensureIndex(new Index().on("paradas.entregas.estado", Sort.Direction.ASC));
        mongo.indexOps(Ruta.class).ensureIndex(new Index().on("paradas.entregas.idDonacion", Sort.Direction.ASC));
    }
}
