package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmarEntregaRequestDTO {
    // Entrega 6: URLs de las fotos de la recepción, las mismas reglas que acepta
    // Donaciones en el aviso de entrega exitosa (hasta 10, http o https).
    @NotEmpty
    @Size(max = 10, message = "se aceptan hasta 10 fotos")
    private List<@NotBlank @Size(max = 2048) @Pattern(regexp = "https?://\\S+",
            message = "tiene que ser una URL que empiece con http:// o https://") String> fotosComprobante;
}
