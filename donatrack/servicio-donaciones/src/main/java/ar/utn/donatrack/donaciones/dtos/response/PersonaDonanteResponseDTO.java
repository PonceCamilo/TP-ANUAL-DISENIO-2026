package ar.utn.donatrack.donaciones.dtos.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PersonaHumanaResponseDTO.class,  name = "HUMANA"),
        @JsonSubTypes.Type(value = PersonaJuridicaResponseDTO.class, name = "JURIDICA")
})
@Getter
@SuperBuilder
public abstract class PersonaDonanteResponseDTO {

    private final UUID id;
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final String email;
    private final String estado;
    private final MedioDeContactoResponseDTO medioContactoPredeterminado;
    private final List<MedioDeContactoResponseDTO> contactos;
    private final DireccionResponseDTO direccion;
}
