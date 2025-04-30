# Etapa 1: Build con Maven
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

# Etapa 2: Runtime con Chrome y ChromeDriver 135
FROM amazoncorretto:21

# Instalar Chrome y ChromeDriver 135
RUN yum update -y && \
    yum install -y wget unzip libX11 && \
    wget https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm && \
    yum install -y ./google-chrome-stable_current_x86_64.rpm && \
    rm google-chrome-stable_current_x86_64.rpm && \
    wget https://storage.googleapis.com/chrome-for-testing-public/135.0.7049.84/linux64/chromedriver-linux64.zip && \
    unzip chromedriver-linux64.zip && \
    mv chromedriver-linux64/chromedriver /usr/bin/chromedriver && \
    chmod +x /usr/bin/chromedriver && \
    rm -rf chromedriver-linux64 chromedriver-linux64.zip

# Variables de entorno necesarias
ENV CHROME_BIN="/usr/bin/google-chrome"
ENV PATH="/usr/bin:${PATH}"

# Copiar el .jar desde el builder
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Comando para iniciar tu aplicación
CMD ["java", "-jar", "app.jar"]
