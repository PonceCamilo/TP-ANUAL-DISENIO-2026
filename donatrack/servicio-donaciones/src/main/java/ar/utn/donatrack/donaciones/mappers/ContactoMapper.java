package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.EmailRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.TelefonoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.WhatsappRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.EmailResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.MedioDeContactoResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.TelefonoResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.WhatsappResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.comunes.TipoDesconocidoException;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Whatsapp;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Medios de contacto ↔ DTO (de personas donantes, entidades y representantes). */
@Component
public class ContactoMapper {

    public MedioDeContacto toContacto(MedioDeContactoRequestDTO dto) {
        return switch (dto) {
            case EmailRequestDTO ignored -> Email.builder().valor(dto.getValor()).build();
            case WhatsappRequestDTO ignored -> Whatsapp.builder().valor(dto.getValor()).build();
            case TelefonoRequestDTO ignored -> Telefono.builder().valor(dto.getValor()).build();
            case null, default -> throw new TipoDesconocidoException("contacto");
        };
    }

    public List<MedioDeContacto> toContactos(List<MedioDeContactoRequestDTO> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(this::toContacto).collect(Collectors.toCollection(ArrayList::new));
    }

    public MedioDeContactoResponseDTO toContactoDTO(MedioDeContacto contacto) {
        if (contacto == null) return null;
        return switch (contacto) {
            case Email e -> EmailResponseDTO.builder().valor(e.getValor()).build();
            case Telefono t -> TelefonoResponseDTO.builder().valor(t.getValor()).build();
            case Whatsapp w -> WhatsappResponseDTO.builder().valor(w.getValor()).build();
            default -> throw new TipoDesconocidoException("contacto");
        };
    }

    public List<MedioDeContactoResponseDTO> toContactosDTO(List<MedioDeContacto> contactos) {
        if (contactos == null) return new ArrayList<>();
        return contactos.stream().map(this::toContactoDTO).collect(Collectors.toCollection(ArrayList::new));
    }
}
