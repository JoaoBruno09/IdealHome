FROM maven:3.9.9-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package

FROM eclipse-temurin:21-jdk
COPY --from=build /target/*.war idealhome-1.0.0.war
EXPOSE 8080

ENTRYPOINT ["java", "-war", "idealhome-1.0.0.war"]