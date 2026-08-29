package br.com.claus.mcpregressionplatform.infrastructure.configuration

import br.com.claus.mcpregressionplatform.domain.dependency.HealthProbeOutcome
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cache", name = ["type"], havingValue = "redis")
    fun probeCacheCustomizer(jsonMapper: JsonMapper): RedisCacheManagerBuilderCustomizer =
        RedisCacheManagerBuilderCustomizer { builder ->
            builder.withCacheConfiguration(
                DEPENDENCY_HEALTH_CACHE,
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(PROBE_CACHE_SECONDS))
                    .disableCachingNullValues()
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            JacksonJsonRedisSerializer(jsonMapper, HealthProbeOutcome::class.java)
                        )
                    )
            )
        }

    companion object {
        const val DEPENDENCY_HEALTH_CACHE = "dependencyHealth"
        private const val PROBE_CACHE_SECONDS = 5L
    }
}
