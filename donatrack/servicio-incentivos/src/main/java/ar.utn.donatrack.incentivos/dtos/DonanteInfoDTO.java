package ar.utn.donatrack.incentivos.dtos;

import lombok.Data;

@Data
public class DonanteInfoDTO {
    // esto seria correcto si del lado del microservicio donaciones existe un endpoint de estadisticas tipo GET /api/donantes/id/estadisticas que devuelva un json con estos datos.
    // la otra opcion seria recibir todos los datos (o al menos la lista de donaciones del donante) y tener que desarmarlos aca -- siento que no tiene tanto sentido pasar toda la info
    private int cantidadCategoriasDonacionesRealizadas;
    private int cantidadDonacionesRecibidasExitosamente;
    private int cantidadRecordBienesEnUnicaDonacion;
    private int mesesConsecutivosDonando;
}