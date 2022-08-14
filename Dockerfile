FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
CMD ["sh","-c", "java -jar /app.jar --server.port=${PORT}"]