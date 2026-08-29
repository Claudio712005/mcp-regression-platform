package br.com.claus.mcpregressionplatform.infrastructure.api

import br.com.claus.mcpregressionplatform.infrastructure.security.PlatformTokenIssuer
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class TokenRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: Instant,
    val roles: List<String>
)

@RestController
@RequestMapping("/auth")
@Validated
class AuthController(private val issuer: PlatformTokenIssuer) {

    @PostMapping("/token")
    fun token(@RequestBody request: TokenRequest): ResponseEntity<TokenResponse> {
        val issued = issuer.issue(request.username, request.password)
        return ResponseEntity.ok(TokenResponse(issued.token, "Bearer", issued.expiresAt, issued.roles))
    }
}
