FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S vetline && adduser -S vetline -G vetline
COPY --from=build /app/target/vetline-crm-1.0.0.jar app.jar
RUN mkdir -p /app/logs && chown -R vetline:vetline /app
USER vetline
EXPOSE 8080
ENTRYPOINT ["java","-Xms256m","-Xmx512m","-XX:+UseG1GC","-Dfile.encoding=UTF-8","-jar","app.jar"]
