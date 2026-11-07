package ar.utn.donatrack.incentivos.models.insignias;

public class InsigniaBuilder {
    private String nombre;
    private String imagen;

    public InsigniaBuilder conNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public InsigniaBuilder conImagen(String imagen) {
        this.imagen = imagen;
        return this;
    }

    public Insignia build() {
        return Insignia.builder()
                .nombre(nombre)
                .imagen(imagen)
                .build();
    }
}
