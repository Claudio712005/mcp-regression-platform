package br.com.claus.mcpregressionplatform.integration

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import java.net.http.HttpRequest
import java.time.Duration

object McpClientSupport {

    fun client(baseUrl: String, token: String?): McpSyncClient {
        val requestBuilder = HttpRequest.newBuilder()
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        val transport = HttpClientStreamableHttpTransport.builder(baseUrl)
            .endpoint("/mcp")
            .requestBuilder(requestBuilder)
            .build()
        val client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(60))
            .build()
        client.initialize()
        return client
    }
}
