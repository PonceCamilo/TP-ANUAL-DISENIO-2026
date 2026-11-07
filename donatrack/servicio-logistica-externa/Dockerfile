FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .
RUN mvn -pl servicio-logistica-externa -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/servicio-logistica-externa/target/servicio-logistica-externa-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
