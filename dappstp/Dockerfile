# Etapa de build
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copiar archivos necesarios para Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dar permisos de ejecución a mvnw
RUN chmod +x mvnw

# Descargar dependencias
RUN ./mvnw dependency:go-offline

# Copiar el resto del código fuente
COPY src src

# Compilar el proyecto
RUN ./mvnw clean package -DskipTests

# Etapa final (runtime) con Java + Chrome
FROM eclipse-temurin:21-jdk AS final

WORKDIR /app

# Instalar Google Chrome estable y dependencias necesarias
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libappindicator3-1 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdbus-1-3 \
    libgdk-pixbuf2.0-0 \
    libnspr4 \
    libnss3 \
    libx11-xcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    xdg-utils \
    # Reemplazar libasound2 con una alternativa
    libasound2-plugins \
    && wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && dpkg -i google-chrome-stable_current_amd64.deb || apt-get -fy install \
    && rm -f google-chrome-stable_current_amd64.deb \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copiar el .jar compilado desde el builder
COPY --from=builder /app/target/*.jar app.jar

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
