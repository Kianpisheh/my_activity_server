FROM maven:3.6.3-jdk-11-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
CMD ["sh","-c", "java -jar /app.jar --server.port=${PORT}"]