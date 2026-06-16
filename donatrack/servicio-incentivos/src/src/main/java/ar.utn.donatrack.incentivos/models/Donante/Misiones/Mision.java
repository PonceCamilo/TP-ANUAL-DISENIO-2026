abstract class Mision{
    int objetivo; // es la cantidad necesaria para completar la mision, por ejemplo, cantidad de donaciones exitosas, cantidad de categorias distintas, cantidad de bienes donados, etc. Este atributo puede ser utilizado por las clases hijas para definir el requerimiento específico de cada tipo de mision.
    Insignia insignia;
    
    // el donante que tengo aca en realidad es el que me tienen que pasar del servicio de donaciones creo.
    public abstract boolean completada(Donante donante);

    // el donante que tengo aca en realidad es el que me tienen que pasar del servicio de donaciones creo.
    public abstract int progresoActual(Donante donante); // este metodo puede ser utilizado para obtener el progreso actual del donante hacia el objetivo de la mision, por ejemplo, cantidad de donaciones exitosas realizadas hasta el momento, cantidad de categorias distintas completadas, cantidad de bienes donados, etc. Este metodo puede ser implementado por las clases hijas para calcular el progreso actual en base a los datos disponibles.
}