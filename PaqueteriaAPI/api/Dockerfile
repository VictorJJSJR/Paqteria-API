FROM maven:3.9.11-eclipse-temurin-17 AS compilacion
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilacion /app/target/api-paqteria-1.0.0.jar /app/api-paqteria.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/api-paqteria.jar"]
