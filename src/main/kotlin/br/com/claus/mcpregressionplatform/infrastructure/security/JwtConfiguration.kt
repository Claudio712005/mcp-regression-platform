package br.com.claus.mcpregressionplatform.infrastructure.security

import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.spec.SecretKeySpec

class MissingSigningKeyException :
    IllegalStateException("platform.security.signing-key must be configured with at least 32 characters")

@Configuration
class JwtConfiguration {

    @Bean
    fun signingKey(properties: PlatformProperties): SecretKeySpec {
        val key = properties.security.signingKey
        if (key.length < MINIMUM_KEY_LENGTH) {
            throw MissingSigningKeyException()
        }
        return SecretKeySpec(key.toByteArray(), MAC_ALGORITHM)
    }

    @Bean
    fun jwtDecoder(signingKey: SecretKeySpec, properties: PlatformProperties): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(signingKey).build()

    @Bean
    fun jwtEncoder(signingKey: SecretKeySpec): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(signingKey))

    private companion object {
        const val MINIMUM_KEY_LENGTH = 32
        const val MAC_ALGORITHM = "HmacSHA256"
    }
}
