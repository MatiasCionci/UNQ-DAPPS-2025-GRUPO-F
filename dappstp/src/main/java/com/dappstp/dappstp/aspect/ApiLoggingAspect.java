package com.dappstp.dappstp.aspect;

import com.dappstp.dappstp.model.ApiLog;
import com.dappstp.dappstp.repository.ApiLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class ApiLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger("AuditLogger");
    private final ApiLogRepository apiLogRepository;
    private final ObjectMapper objectMapper; // Para serializar objetos a JSON

    public ApiLoggingAspect(ApiLogRepository apiLogRepository, ObjectMapper objectMapper) {
        this.apiLogRepository = apiLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Pointcut que intercepta todos los métodos públicos dentro de clases
     * anotadas con @RestController en el paquete de webservices.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) && within(com.dappstp.dappstp.webservices..*)")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String requestArgsJson = "N/A";
        try {
            // Filtrar HttpServletRequest y HttpServletResponse de los argumentos a serializar
            Object[] argsToLog = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> !(arg instanceof HttpServletRequest || arg instanceof HttpServletResponse))
                    .toArray();
            if (argsToLog.length > 0) {
                requestArgsJson = objectMapper.writeValueAsString(argsToLog);
            }
        } catch (Exception e) {
            logger.warn("Error al serializar argumentos del request para {}.{}: {}", className, methodName, e.getMessage());
            requestArgsJson = "Error al serializar argumentos: " + e.getMessage();
        }

        ApiLog apiLog = new ApiLog(className, methodName, requestArgsJson, request.getRequestURI(), request.getMethod());
        Object result;
        Integer responseStatus = null;

        try {
            result = joinPoint.proceed(); // Ejecuta el método del controlador

            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
            if (response != null) {
                responseStatus = response.getStatus();
            }

            apiLog.completeLog(serializeReturnValue(result, className, methodName), System.currentTimeMillis() - startTime, responseStatus);
            return result;
        } catch (Exception e) {
            logger.error("Excepción en {}.{}: {}", className, methodName, e.getMessage(), e);
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
            if (response != null) {
                responseStatus = response.getStatus(); // Puede ser que el status ya esté seteado por un ExceptionHandler
            }
            apiLog.completeLogWithError(e.getClass().getSimpleName(), e.getMessage(), System.currentTimeMillis() - startTime, responseStatus);
            throw e; // Relanza la excepción para que sea manejada por Spring (ej. @ControllerAdvice)
        } finally {
            apiLogRepository.save(apiLog);
            logger.info("API call logged: {}#{} | Duration: {}ms | Status: {}", className, methodName, apiLog.getDurationMs(), apiLog.getResponseStatus());
        }
    }

    /**
     * Serializa el valor de retorno de un método a JSON.
     * @param result El objeto a serializar.
     * @param className El nombre de la clase del método.
     * @param methodName El nombre del método.
     * @return La representación JSON del objeto o un mensaje de error.
     */
    private String serializeReturnValue(Object result, String className, String methodName) {
        if (result == null) {
            return "N/A";
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.warn("Error al serializar valor de retorno para {}.{}: {}", className, methodName, e.getMessage());
            return "Error al serializar valor de retorno: " + e.getMessage();
        }
    }
}