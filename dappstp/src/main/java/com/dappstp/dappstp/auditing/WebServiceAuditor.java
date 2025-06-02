package com.dappstp.dappstp.auditing;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest; // Para Spring Boot 3+ (Jakarta EE 9+)
import jakarta.servlet.http.HttpServletResponse; // Para Spring Boot 3+
// import javax.servlet.http.HttpServletRequest; // Para Spring Boot 2 (Java EE 8)
// import javax.servlet.http.HttpServletResponse; // Para Spring Boot 2

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class WebServiceAuditor {

    // Define un logger específico para la auditoría. Su nombre debe coincidir con la configuración en logback-spring.xml.
    private static final Logger auditLogger = LoggerFactory.getLogger("WebServiceAuditLogger");
    private final ObjectMapper objectMapper = new ObjectMapper(); // Para serializar parámetros a JSON
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private final MeterRegistry meterRegistry;

    // Inyectar MeterRegistry
    public WebServiceAuditor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Pointcut que intercepta todos los métodos públicos dentro de clases anotadas con @RestController.
     * Puedes ajustar este pointcut según tus necesidades, por ejemplo, para apuntar a paquetes específicos
     * o anotaciones de métodos como @GetMapping, @PostMapping, etc.
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * *(..))")
    public Object auditWebServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        LocalDateTime timestamp = LocalDateTime.now();

        // 1. Usuario
        String user = "anonymous";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            user = authentication.getName();
        }

        // 2. Operación/Método
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();

        // 3. Parámetros
        String parametersString = "N/A";
        try {
            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames(); // Requiere compilación con -parameters
            Map<String, Object> paramsMap = new HashMap<>();

            if (args != null && paramNames != null && args.length == paramNames.length) {
                for (int i = 0; i < args.length; i++) {
                    if (isLoggableParameter(args[i])) {
                        paramsMap.put(paramNames[i], args[i]);
                    } else {
                        paramsMap.put(paramNames[i], args[i].getClass().getSimpleName() + " (no registrado)");
                    }
                }
            }
            if (!paramsMap.isEmpty()) {
                parametersString = objectMapper.writeValueAsString(paramsMap);
            }
        } catch (JsonProcessingException e) {
            parametersString = "Error al serializar parámetros: " + e.getMessage();
            // Considera loguear este error internamente si es necesario
            LoggerFactory.getLogger(WebServiceAuditor.class).warn("No se pudieron serializar los parámetros para la auditoría: {}", methodName, e);
        } catch (Exception e) {
            parametersString = "Error obteniendo parámetros: " + e.getMessage();
            LoggerFactory.getLogger(WebServiceAuditor.class).warn("Error inesperado obteniendo parámetros para auditoría: {}", methodName, e);
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success"; // Por defecto

        Object result;
        try {
            result = joinPoint.proceed(); // Ejecuta el método original del servicio web
        } catch (Throwable throwable) {
            outcome = "error"; // Marcar como error si hay excepción
            throw throwable; // Relanzar para que el manejo de errores de Spring funcione
        } finally {
            long executionTime = System.currentTimeMillis() - startTime; // 4. Tiempo de Ejecución

            // Registrar el tiempo de ejecución con tags para método y resultado
            sample.stop(meterRegistry.timer("webservice.execution.time",
                    "method", methodName,
                    "outcome", outcome));

            // Incrementar un contador para las llamadas al servicio
            meterRegistry.counter("webservice.calls.total",
                    "method", methodName,
                    "outcome", outcome).increment();


            // Formato del log: <timestamp,user,operación/metodo, parámetros, tiempoDeEjecicion>
            String logMessage = String.format("<%s,%s,%s,%s,%dms>",
                    timestamp.format(TIMESTAMP_FORMATTER),
                    user,
                    methodName,
                    parametersString,
                    executionTime);
            auditLogger.info(logMessage);
        }
        return result;
    }

    private boolean isLoggableParameter(Object param) {
        if (param == null) return true;
        // Evita serializar objetos que no son datos de entrada simples o que podrían ser problemáticos/sensibles.
        // Ejemplos: HttpServletRequest, HttpServletResponse, MultipartFile, Model, BindingResult, Principal.
        // Considera añadir más clases o usar anotaciones para marcar parámetros como no registrables.
        return !(param instanceof HttpServletRequest ||
                 param instanceof HttpServletResponse ||
                 param instanceof org.springframework.ui.Model ||
                 param instanceof org.springframework.web.multipart.MultipartFile ||
                 param instanceof org.springframework.validation.BindingResult ||
                 param instanceof java.security.Principal);
    }
}
