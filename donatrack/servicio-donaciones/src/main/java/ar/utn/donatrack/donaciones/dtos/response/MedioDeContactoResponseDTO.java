package ar.utn.donatrack.donaciones.dtos.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmailResponseDTO.class,    name = "EMAIL"),
    @JsonSubTypes.Type(value = TelefonoResponseDTO.class, name = "TELEFONO"),
    @JsonSubTypes.Type(value = WhatsappResponseDTO.class, name = "WHATSAPP")
})
@Getter
@SuperBuilder
public abstract class MedioDeContactoResponseDTO {

  protected String valor;
}