package ar.utn.donatrack.logistica.models.entrega;

/**
 * posibles motivos por los que una entrega no pudo concretarse.
 *
 * La condición de "replanificable" es una regla de negocio derivada del motivo,
 * no un dato que decide el chofer: si la mercadería se perdió/rompió/robó, no
 * tiene sentido reintentar; si la entidad estaba ausente o la dirección era
 * incorrecta, sí. El chofer solo informa el motivo y Logística deriva el booleano.
 */
public enum MotivoFalloEntrega {
    ENTIDAD_AUSENTE(true),
    DIRECCION_INCORRECTA(true),
    RECHAZADA_POR_ENTIDAD(true),
    MERCADERIA_ROTA(false),
    MERCADERIA_PERDIDA(false),
    ROBO(false);

    private final boolean replanificable;

    MotivoFalloEntrega(boolean replanificable) {
        this.replanificable = replanificable;
    }

    public boolean esReplanificable() {
        return replanificable;
    }
}
