package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.LocalidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DireccionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.LocalidadResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.ProvinciaResponseDTO;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.Localidad;
import ar.utn.donatrack.donaciones.models.entidad.Provincia;
import org.springframework.stereotype.Component;

/**
 * Dirección ↔ DTO. La usan personas donantes y entidades beneficiarias; antes
 * vivía dentro de PersonaDonanteMapper y el mapper de entidades dependía de él
 * solo por esto.
 */
@Component
public class DireccionMapper {

    public Direccion toDireccion(DireccionRequestDTO dto) {
        if (dto == null) return null;
        return Direccion.builder()
                .calle(dto.getCalle())
                .numero(dto.getNumero())
                .codigoPostal(dto.getCodigoPostal())
                .localidad(toLocalidad(dto.getLocalidad()))
                .build();
    }

    public Localidad toLocalidad(LocalidadRequestDTO dto) {
        if (dto == null) return null;
        return Localidad.builder()
                .nombre(dto.getNombre())
                .provincia(Provincia.builder()
                        .nombre(dto.getProvincia().getNombre())
                        .build())
                .build();
    }

    public DireccionResponseDTO toDireccionDTO(Direccion direccion) {
        if (direccion == null) return null;
        return DireccionResponseDTO.builder()
                .calle(direccion.getCalle())
                .numero(direccion.getNumero())
                .codigoPostal(direccion.getCodigoPostal())
                .localidad(toLocalidadDTO(direccion.getLocalidad()))
                .build();
    }

    public LocalidadResponseDTO toLocalidadDTO(Localidad localidad) {
        if (localidad == null) return null;
        return LocalidadResponseDTO.builder()
                .nombre(localidad.getNombre())
                .provincia(localidad.getProvincia() == null ? null :
                        ProvinciaResponseDTO.builder()
                                .nombre(localidad.getProvincia().getNombre())
                                .build())
                .build();
    }
}
