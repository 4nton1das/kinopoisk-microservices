FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 9090 9091
ENTRYPOINT ["java", "-jar", "app.jar"]