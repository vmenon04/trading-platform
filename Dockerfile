FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
# skip running tests during the build process
RUN mvn -B clean package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /src/target/fintech-five.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]