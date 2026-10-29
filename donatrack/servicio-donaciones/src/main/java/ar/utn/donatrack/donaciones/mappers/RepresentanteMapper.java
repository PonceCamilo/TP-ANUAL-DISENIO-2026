package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.RepresentanteResponseDTO;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Representantes (de organizaciones y entidades) ↔ DTO. */
@Component
@RequiredArgsConstructor
public class RepresentanteMapper {

    private final ContactoMapper contactos;

    public Representante toRepresentante(RepresentanteRequestDTO dto) {
        if (dto == null) return null;
        return Representante.builder()
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .email(dto.getEmail())
                .build();
    }

    public List<Representante> toRepresentantes(List<RepresentanteRequestDTO> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(this::toRepresentante).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<RepresentanteResponseDTO> toRepresentantesDTO(List<Representante> representantes) {
        if (representantes == null) return new ArrayList<>();
        return representantes.stream()
                .map(r -> RepresentanteResponseDTO.builder()
                        .nombre(r.getNombre())
                        .apellido(r.getApellido())
                        .email(r.getEmail())
                        .contactos(contactos.toContactosDTO(r.getContactos()))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
