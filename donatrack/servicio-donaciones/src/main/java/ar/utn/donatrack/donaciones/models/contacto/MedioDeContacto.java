package ar.utn.donatrack.donaciones.models.contacto;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Canal por el que se puede contactar a una persona o entidad.
 *
 * MAPEO DE LA HERENCIA (Entrega 4): SINGLE_TABLE.
 *
 * Las tres subclases (Email, Telefono, Whatsapp) no agregan ni un campo: lo
 * único que las distingue es el comportamiento de notificación, que resuelve el
 * servicio de notificaciones. Con JOINED tendríamos tres tablas vacías y un
 * JOIN por cada contacto leído; con tabla única alcanza el discriminador.
 */
@Entity
@Table(name = "medio_de_contacto")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_medio")
@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class MedioDeContacto {

  /** Id técnico: en el modelo original el contacto no tenía identidad propia. */
  @Id
  @Column(name = "id")
  @lombok.Builder.Default
  protected UUID id = UUID.randomUUID();

  @Column(name = "valor")
  protected String valor;
}
