FROM maven:3.9.4-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
COPY --from=build /target/DALI-0.0.1-SNAPSHOT.jar DALI.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "DALI.jar"]
