FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy only pom and download dependencies first for cache
COPY pom.xml mvnw .
COPY .mvn .mvn
RUN mvn -B -f pom.xml -DskipTests dependency:go-offline

# copy source and build
COPY src ./src
RUN mvn -B -f pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
