package ar.utn.donatrack.donaciones.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
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
        @Valid @DefaultValue Notificaciones notificaciones,
        @Valid @DefaultValue Matchmaking matchmaking,
        @Valid @DefaultValue Salida salida,
        @Valid @DefaultValue Seguridad seguridad,
        @Valid @DefaultValue Cuentas cuentas) {

    /**
     * Aviso a donantes inactivos. El enunciado fija el plazo en 20 días sin
     * interacción con la plataforma.
     */
    public record Inactividad(
            @DefaultValue("20") @Positive int dias,
            @DefaultValue("0 0 0 * * *") @NotBlank String cron,
            @DefaultValue("100") @Positive int tamanioLote) {
    }

    /**
     * Procesos nocturnos: horarios (en hora de Argentina), cuántas donaciones lee
     * cada lote del matchmaking y del despacho, y si las tareas programadas se
     * bloquean para que con varias instancias corran en una sola
     * (BloqueoDeTareasConfig).
     */
    public record Procesos(
            @DefaultValue("0 0 3 * * *") @NotBlank String cronReconciliacion,
            @DefaultValue("0 30 3 * * *") @NotBlank String cronMatchmaking,
            @DefaultValue("0 0 4 * * *") @NotBlank String cronDespacho,
            @DefaultValue("100") @Positive int tamanioLote,
            @DefaultValue("true") boolean bloqueoDistribuido) {
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
            @DefaultValue("admin@donatrack.org") @NotBlank @Email String emailAdmin,
            // Entrega 6: dirección pública del front, para los enlaces de los avisos.
            @DefaultValue("http://localhost:8080") @NotBlank String urlFrontend) {
    }

    /** Cuántas entidades candidatas devuelve cada algoritmo de asignación. */
    public record Matchmaking(
            @DefaultValue("10") @Positive int tamanioRanking) {
    }

    /**
     * Bandeja de salida (outbox): cada cuánto se envían los avisos pendientes,
     * de a cuántos se leen los de cada destino, cuántas veces se reintenta uno
     * antes de darlo por fallido y cuántos días se conservan los ya enviados.
     */
    public record Salida(
            @DefaultValue("true") boolean habilitada,
            @DefaultValue("5000") @Positive long intervaloMs,
            @DefaultValue("50") @Positive int tamanioLote,
            @DefaultValue("10") @Positive int maxIntentos,
            @DefaultValue("7") @Positive int diasRetencion,
            @DefaultValue("0 30 4 * * *") @NotBlank String cronLimpieza) {
    }

    /**
     * Validación del JWT que emite el Servicio de Autenticación (Entrega 6).
     *
     * Las claves públicas se piden a esa URL recién con el primer token y se
     * guardan en memoria: Donaciones arranca aunque Autenticación todavía no
     * esté arriba. El emisor y la audiencia son los que ese servicio escribe en
     * cada token (iss y aud); uno de otro emisor o para otra audiencia se rechaza.
     */
    public record Seguridad(
            @DefaultValue("http://localhost:8088/api/autenticacion/claves-publicas") @NotBlank String clavesPublicasUrl,
            @DefaultValue("urn:donatrack:autenticacion") @NotBlank String emisor,
            @DefaultValue("donatrack-api") @NotBlank String audiencia) {
    }

    /**
     * Cuentas para las personas importadas por CSV (Entrega 6, requisito 20). Al
     * importar, Donaciones avisa al Servicio de Autenticación qué personas tiene
     * que dar de alta, por la bandeja de salida y de a varias por aviso (el
     * mensaje guardado admite hasta 10.000 caracteres: 60 personas entran).
     *
     * Apagado por defecto (Donaciones corriendo solo); el docker-compose lo
     * enciende junto con Autenticación. Apagado no se encola nada. Al encenderlo,
     * reimportar el CSV avisa por todas (las que ya existen también se avisan, y
     * Autenticación no las duplica).
     *
     * @param avisarImportados si se avisa a Autenticación al importar
     * @param url              dirección del Servicio de Autenticación
     * @param clave            clave que se manda en X-DonaTrack-Servicio; vacía, no se manda
     * @param personasPorAviso cuántas personas van en cada aviso
     */
    public record Cuentas(
            @DefaultValue("false") boolean avisarImportados,
            @DefaultValue("http://localhost:8088") @NotBlank String url,
            @DefaultValue("") String clave,
            @DefaultValue("50") @Positive @Max(60) int personasPorAviso) {
    }
}
