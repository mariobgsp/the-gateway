FROM docker.io/library/maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# ponytail: respect JAVA_OPTS for heap tuning without image rebuild
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
