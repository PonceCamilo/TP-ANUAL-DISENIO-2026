
@Service
public class DonanteService implements DonanteServiceInterface {
    // PERMITE : subir de categoria
    // PERMITE: Completar misiones --> pasando a la siguiente
    // DEBE: obtener insignias
    // DEBE: poder volver las insignias visibles o no visibles para el resto de los usuarios
    // DEBE: ver su progreso en las misiones que tiene en progreso
    // DEBE: ver distancia restante respecto a completar las misiones que tiene en progreso


    private final DonanteRepositoryInterface repositoryInterface;
    private final ServicioDonacionesAPI servicioDonacionesAPI;

    public DonanteService(DonanteRepositoryInterface repositoryInterface, DonacionesClient donacionesClient) {
        this.repositoryInterface = repositoryInterface;
        this.donacionesClient = donacionesClient;
    }

    public void subirDeCategoria(UUID idDonante){
        //1. Buscamos el donante en local
        Donante donante = DonanteRepositoryInterface.findById(idDonante);

        //2. Se le pregunta al servicio de donaciones por su historial de donaciones para obtener la informacion necesaria para verificar si cumple con los requisitos para subir de categoria. Esto se puede hacer a traves de una llamada a una API del servicio de donaciones, pasando el id del donante como parametro, y recibiendo como respuesta el historial de donaciones del donante, que puede incluir informacion como la cantidad de donaciones realizadas, las categorias de las donaciones, la cantidad de bienes donados en cada donacion, etc. Esta informacion es necesaria para verificar si el donante cumple con los requisitos para subir de categoria, como por ejemplo, haber realizado una cierta cantidad de donaciones exitosas, haber completado misiones relacionadas con las categorias de las donaciones, haber alcanzado un record de bienes donados en una unica donacion, etc.
        DonanteInfoDTO donanteInfo = ServicioDonacionesAPI.findById(idDonante); // TODO programar esta clase y metodo para obtener la informacion del donante desde el servicio de donaciones

        //3. Verificamos si el donante cumple con los requisitos para subir de categoria
        CategoriaDonante categoriaActual = donante.getCategoria();
        CategoriaDonante categoriaSiguiente = categoriaActual.siguiente();
        // cuando sube de categoria? Cuando termina todas las misiones de la misma. Eso se deberia programar en requisitosCumplidos
        if(categoriaSiguiente != null && categoriaActual.misionesCompletadas(donanteInfo)){
            //3. Si cumple con los requisitos, actualizamos la categoria del donante
            donante.setCategoria(categoriaSiguiente);
            //4. Guardamos el donante actualizado en local
            repositoryInterface.save(donante);
        }
    }

    public void completarMision(UUID idDonante, Mision mision){
        //1. Buscamos el donante en local
        Donante donante = DonanteRepositoryInterface.findById(idDonante);

        //2. Se le pregunta al servicio de donaciones por su historial de donaciones para obtener la informacion necesaria para verificar si cumple con los requisitos para completar la mision. Esto se puede hacer a traves de una llamada a una API del servicio de donaciones, pasando el id del donante como parametro, y recibiendo como respuesta el historial de donaciones del donante, que puede incluir informacion como la cantidad de donaciones realizadas, las categorias de las donaciones, la cantidad de bienes donados en cada donacion, etc. Esta informacion es necesaria para verificar si el donante cumple con los requisitos para completar la mision, como por ejemplo, haber realizado una cierta cantidad de donaciones exitosas, haber completado misiones relacionadas con las categorias de las donaciones, haber alcanzado un record de bienes donados en una unica donacion, etc.
        DonanteInfoDTO donanteInfo = ServicioDonacionesAPI.findById(idDonante);
        
        //3. Verificamos si el donante cumple con los requisitos para completar la mision
        if(mision.completada(donanteInfo)){
            //4. Si cumple con los requisitos, otorgamos la insignia correspondiente a la mision
            Insignia insignia = mision.getInsignia();
            donante.addInsignia(insignia);

            //5. Le asignamos como misionActual al donante la siguiente mision de la categoria o sino lo subimos de categoria
            Mision siguienteMision = mision.getSiguienteMision();
            donante.setMisionActual(siguienteMision);
            donante.setProgresoMisionActual(0); // reseteamos el progreso de la nueva mision actual a 0
            donante.setDistanciaRestanteMisionActual(siguienteMision.getObjetivo()); // seteamos la distancia restante de la nueva mision actual al objetivo de la nueva mision


            // Subimos de categoria, ver que esto no se debe hacer siempree
            donante.subirDeCategoria(); // verificamos si al completar esta mision, el donante cumple con los requisitos para subir de categoria, y si es asi, lo subimos de categoria. Esto se puede hacer llamando al metodo subirDeCategoria() que ya esta programado, pasando el id del donante como parametro.

            //6. Guardamos el donante actualizado en local
            repositoryInterface.save(donante);
        }


}