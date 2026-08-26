package br.com.claus.mcpregressionplatform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class McpRegressionPlatformApplication

fun main(args: Array<String>) {
    runApplication<McpRegressionPlatformApplication>(*args)
}
