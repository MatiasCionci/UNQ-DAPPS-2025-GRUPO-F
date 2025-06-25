# Etapa de build: usa la imagen oficial de Maven para compilar
FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar POM y código fuente
COPY pom.xml ./
COPY src ./src

# Empaquetar la aplicación (compila, testa, genera el JAR)
RUN mvn clean package -DskipTests

# Etapa final (runtime) con Java + Chrome
FROM eclipse-temurin:21-jdk AS final

WORKDIR /app

# Instalar Google Chrome estable y dependencias mínimas
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       wget unzip gnupg ca-certificates fonts-liberation \
       libappindicator3-1 libatk-bridge2.0-0 libatk1.0-0 libcups2 \
       libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 \
       libx11-xcb1 libxcomposite1 libxdamage1 libxrandr2 \
       xdg-utils libasound2-plugins \
       libgbm1 libvulkan1 \
    && wget -qO /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && dpkg -i /tmp/chrome.deb || apt-get -fy install \
    && rm -f /tmp/chrome.deb \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
    
WORKDIR /app

# Crear directorio de logs con permisos adecuados
RUN mkdir -p logs && chmod 777 logs

# Copiar el JAR generado por el builder
COPY --from=builder /app/target/*.jar app.jar


# Puerto expuesto y comando de inicio
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
