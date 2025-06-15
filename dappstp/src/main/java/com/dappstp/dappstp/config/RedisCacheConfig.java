package com.dappstp.dappstp.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Configura el ObjectMapper que se usará para la serialización/deserialización
     * de objetos en JSON para Redis.
     * Incluye soporte para tipos de Hibernate y Java Time.
     * Habilita la información de tipo por defecto para manejar correctamente colecciones genéricas.
     */
    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Módulo para manejar correctamente los tipos de Hibernate (lazy loading, proxies)
        // Asegúrate de usar el módulo correcto para tu versión de Hibernate (Hibernate5JakartaModule, Hibernate6Module, etc.)
        Hibernate5JakartaModule hibernateModule = new Hibernate5JakartaModule();
        // Opcional: Configurar características del módulo de Hibernate
        // hibernateModule.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, false);
        // hibernateModule.configure(Hibernate5JakartaModule.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        objectMapper.registerModule(hibernateModule);

        // Módulo para manejar tipos de Java Time API (LocalDate, LocalDateTime, etc.)
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Escribir fechas como ISO Strings

        // Habilitar la información de tipo por defecto.
        // Esto añade una propiedad (ej. "@class") al JSON para que Jackson sepa
        // a qué clase concreta deserializar, crucial para colecciones de objetos polimórficos o genéricos.
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(), // Validador polimórfico por defecto de Jackson
                ObjectMapper.DefaultTyping.NON_FINAL,       // Para tipos no finales
                JsonTypeInfo.As.PROPERTY                    // Almacenar la información de tipo como una propiedad
        );

        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Si quieres ignorar propiedades desconocidas
        // La propiedad spring.jackson.serialization.FAIL_ON_EMPTY_BEANS=false ya está en tu application.properties

        return objectMapper;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration(@Value("${spring.cache.redis.time-to-live:3600s}") Duration ttl) {
        ObjectMapper redisObjectMapper = buildRedisObjectMapper();
        GenericJackson2JsonRedisSerializer jacksonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl) // Usa el TTL de application-dev.properties o un valor por defecto
                .disableCachingNullValues() // Opcional: no cachear valores nulos
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jacksonSerializer));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration cacheConfiguration) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration) // Aplica la configuración por defecto a todos los cachés
                // Opcional: Configurar cachés específicos con diferentes TTLs o configuraciones
                // .withCacheConfiguration("playersCache",
                //    cacheConfiguration.entryTtl(Duration.ofHours(2)))
                // .withCacheConfiguration("anotherSpecificCache",
                //    cacheConfiguration.entryTtl(Duration.ofMinutes(5))
                //                      .serializeValuesWith(...) // si necesitas un serializador diferente para este caché
                // )
                .transactionAware() // Opcional: para que las operaciones de caché sean transaccionales con las de BD
                .build();
    }
}