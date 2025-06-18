package com.dappstp.dappstp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_logs")
@Getter
@Setter
@NoArgsConstructor
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String methodName;

    @Lob // Large Object para almacenar strings potencialmente largos como JSON
    @Column(columnDefinition = "TEXT")
    private String requestArgs; // Argumentos del método serializados a JSON

    @Lob
    @Column(columnDefinition = "TEXT")
    private String returnValue; // Valor de retorno serializado a JSON o mensaje de error

    private String exceptionType; // Tipo de excepción si ocurrió alguna

    @Column(nullable = false)
    private Long durationMs; // Duración de la ejecución del método en milisegundos

    private String requestUri; // URI de la solicitud HTTP

    private String requestHttpMethod; // Método HTTP (GET, POST, etc.)

    private Integer responseStatus; // Código de estado HTTP de la respuesta


    public ApiLog(String className, String methodName, String requestArgs, String requestUri, String requestHttpMethod) {
        this.timestamp = LocalDateTime.now();
        this.className = className;
        this.methodName = methodName;
        this.requestArgs = requestArgs;
        this.requestUri = requestUri;
        this.requestHttpMethod = requestHttpMethod;
    }

    // Getters y Setters generados por Lombok o puedes añadirlos manualmente
    // Constructor

    // Ejemplo de cómo podrías completar el log después de la ejecución
    public void completeLog(String returnValue, Long durationMs, Integer responseStatus) {
        this.returnValue = returnValue;
        this.durationMs = durationMs;
        this.responseStatus = responseStatus;
    }

    public void completeLogWithError(String exceptionType, String errorMessage, Long durationMs, Integer responseStatus) {
        this.exceptionType = exceptionType;
        this.returnValue = errorMessage; // Usamos returnValue para el mensaje de error
        this.durationMs = durationMs;
        this.responseStatus = responseStatus;
    }
}
