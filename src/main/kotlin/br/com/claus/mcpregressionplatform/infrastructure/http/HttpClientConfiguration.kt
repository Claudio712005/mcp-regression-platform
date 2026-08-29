package br.com.claus.mcpregressionplatform.infrastructure.http

import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class HttpClientConfiguration {

    @Bean
    fun accountServiceRestClient(properties: PlatformProperties): RestClient {
        val settings = HttpClientSettings.defaults()
            .withTimeouts(
                properties.integrations.accountService.connectTimeout,
                properties.integrations.accountService.readTimeout
            )
        return RestClient.builder()
            .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings))
            .build()
    }
}
