# Etapa de construcción con Maven y Java 21 
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Creamos carpeta de trabajo
WORKDIR /app

# Copiamos solo lo necesario para cachear dependencias primero
COPY pom.xml .  
COPY .mvn .mvn  
COPY mvnw .  
COPY mvnw.cmd .

# Le damos permisos de ejecución al script mvnw y descargamos dependencias sin compilar
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Ahora copiamos el resto del proyecto
COPY src ./src

# Ejecutamos el build con Maven (sin tests)
RUN ./mvnw clean package -DskipTests

# Etapa final con solo Java para ejecutar la app
FROM eclipse-temurin:21-jdk

# Creamos carpeta de ejecución
WORKDIR /app

# Copiamos el .jar desde el contenedor anterior (usamos comodín por si cambia el nombre del JAR)
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto típico (podés ajustarlo si usás otro)
EXPOSE 8080

# Comando de entrada para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]
