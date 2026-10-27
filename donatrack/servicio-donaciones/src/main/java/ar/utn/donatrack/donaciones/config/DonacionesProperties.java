package ar.utn.donatrack.donaciones.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros del servicio que antes estaban escritos como constantes en el código
 * (plazos, horarios de los procesos nocturnos, tamaño de lote, destinatarios).
 *
 * Se leen de las propiedades con prefijo `donaciones.` y se validan al arrancar:
 * un valor inválido impide levantar el servicio en lugar de fallar más tarde.
 * Cada valor tiene un default igual al comportamiento histórico, así que el
 * servicio funciona sin declarar ninguno.
 *
 * Los horarios (cron) se usan en los @Scheduled a través de placeholders con la
 * misma clave; acá se declaran para que la configuración quede documentada y
 * validada en un solo lugar.
 */
@Validated
@ConfigurationProperties(prefix = "donaciones")
public record DonacionesProperties(
        @Valid @DefaultValue Inactividad inactividad,
        @Valid @DefaultValue Procesos procesos,
        @Valid @DefaultValue Importacion importacion,
        @Valid @DefaultValue Notificaciones notificaciones) {

    /** Los mismos valores que se usan cuando no se configura nada. */
    public static DonacionesProperties porDefecto() {
        return new DonacionesProperties(
                new Inactividad(20, "0 0 0 * * *", 100),
                new Procesos("0 0 3 * * *", "0 30 3 * * *", "0 0 4 * * *"),
                new Importacion(500, 100),
                new Notificaciones("admin@donatrack.org"));
    }

    /**
     * Aviso a donantes inactivos. El enunciado fija el plazo en 20 días sin
     * interacción con la plataforma.
     */
    public record Inactividad(
            @DefaultValue("20") @Positive int dias,
            @DefaultValue("0 0 0 * * *") @NotBlank String cron,
            @DefaultValue("100") @Positive int tamanioLote) {
    }

    /** Horarios de los procesos nocturnos, en hora de Argentina. */
    public record Procesos(
            @DefaultValue("0 0 3 * * *") @NotBlank String cronReconciliacion,
            @DefaultValue("0 30 3 * * *") @NotBlank String cronMatchmaking,
            @DefaultValue("0 0 4 * * *") @NotBlank String cronDespacho) {
    }

    /**
     * Importación masiva por CSV: cuántas filas se procesan por transacción y
     * cuántos errores se devuelven en la respuesta (el resto se cuenta).
     */
    public record Importacion(
            @DefaultValue("500") @Positive int tamanioLote,
            @DefaultValue("100") @Positive int maxErroresEnRespuesta) {
    }

    /**
     * Destinatario de las alertas operativas (por ejemplo, una entrega fallida).
     * Cuando exista el servicio de autenticación (Entrega 6) se podrá obtener la
     * lista real de personas administradoras.
     */
    public record Notificaciones(
            @DefaultValue("admin@donatrack.org") @NotBlank @Email String emailAdmin) {
    }
}
