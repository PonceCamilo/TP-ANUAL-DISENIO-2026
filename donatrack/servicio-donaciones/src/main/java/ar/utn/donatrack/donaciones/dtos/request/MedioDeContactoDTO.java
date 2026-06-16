package ar.utn.donatrack.donaciones.dtos.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmailDTO.class, name = "EMAIL"),
    @JsonSubTypes.Type(value = TelefonoDTO.class, name = "TELEFONO"),
    @JsonSubTypes.Type(value = WhatsappDTO.class, name = "WHATSAPP")
})

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public abstract class MedioDeContactoDTO {

  @NotBlank
  protected String valor;
}