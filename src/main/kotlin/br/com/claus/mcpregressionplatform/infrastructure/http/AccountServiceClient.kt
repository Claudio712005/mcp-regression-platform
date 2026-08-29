package br.com.claus.mcpregressionplatform.infrastructure.http

import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import br.com.claus.mcpregressionplatform.infrastructure.demo.DemoScenarioHolder
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant

data class HttpExchange(
    val status: Int?,
    val body: String?,
    val latency: Duration,
    val timedOut: Boolean,
    val transportError: String?
)

@Component
class AccountServiceClient(
    private val restClient: RestClient,
    private val properties: PlatformProperties,
    private val scenarioHolder: DemoScenarioHolder
) {

    fun get(path: String): HttpExchange {
        val started = Instant.now()
        return try {
            val response = restClient.get()
                .uri(properties.integrations.accountService.baseUrl + path)
                .header(
                    properties.integrations.accountService.apiKeyHeader,
                    properties.integrations.accountService.apiKey
                )
                .header(properties.demo.scenarioHeader, scenarioHolder.current().id)
                .exchange({ _, response ->
                    ExchangeSnapshot(response.statusCode, response.body.readAllBytes().decodeToString())
                }, false)
            HttpExchange(
                status = response?.status?.value(),
                body = response?.body,
                latency = Duration.between(started, Instant.now()),
                timedOut = false,
                transportError = null
            )
        } catch (error: Exception) {
            HttpExchange(
                status = null,
                body = null,
                latency = Duration.between(started, Instant.now()),
                timedOut = isTimeout(error),
                transportError = error.message ?: error::class.simpleName
            )
        }
    }

    private fun isTimeout(error: Throwable?): Boolean {
        var cause = error
        while (cause != null) {
            if (cause is java.net.SocketTimeoutException || cause is java.net.http.HttpTimeoutException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private data class ExchangeSnapshot(val status: HttpStatusCode, val body: String)
}
