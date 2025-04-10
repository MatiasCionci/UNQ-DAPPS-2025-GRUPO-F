# Etapa de construcción con Maven y Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Creamos una carpeta de trabajo
WORKDIR /app

# Copiamos todo el proyecto al contenedor
COPY . .

# Ejecutamos el build con Maven (sin tests)
RUN mvn clean package -DskipTests

# Etapa final con solo Java para ejecutar la app
FROM eclipse-temurin:21-jdk

# Creamos carpeta de ejecución
WORKDIR /app

# Copiamos el .jar desde el contenedor anterior
COPY --from=build /app/target/dappstp-0.0.1-SNAPSHOT.jar app.jar

# Comando para iniciar la aplicación
CMD ["java", "-jar", "app.jar"]
