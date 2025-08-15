FROM eclipse-temurin:17-jdk-jammy
COPY target/shipping-0.0.1-SNAPSHOT.jar /app/monapp.jar
WORKDIR /app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "monapp.jar"]