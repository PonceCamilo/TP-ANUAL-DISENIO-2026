# Build stage: se construye desde la raíz del reactor (context: .) para poder
# resolver el POM padre y las dependencias entre módulos. -am compila también
# los módulos de los que depende este servicio.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -pl servicio-notificaciones -am package -DskipTests

# Runtime stage: imagen liviana solo con el JRE y el jar ejecutable.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/servicio-notificaciones/target/servicio-notificaciones-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
