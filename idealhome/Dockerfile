FROM maven:3.9.9-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package

FROM eclipse-temurin:21-jdk
COPY --from=build /target/*.jar idealhome-1.0.0.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "idealhome-1.0.0.jar"]