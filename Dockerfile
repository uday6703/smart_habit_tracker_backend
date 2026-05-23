FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /workspace

# copy only pom and download dependencies first for cache (paths from repo root)
COPY backend/pom.xml ./pom.xml
RUN mvn -B -f pom.xml -DskipTests dependency:go-offline

# copy source and build (paths from repo root)
COPY backend/src ./src
COPY backend/pom.xml ./pom.xml
RUN mvn -B -f pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
