package br.com.claus.mcpregressionplatform.infrastructure.mcp.security

import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.annotation.McpResource
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

class UnprotectedMcpCapabilityException(details: List<String>) :
    IllegalStateException("MCP capabilities without an authorization requirement: ${details.joinToString()}")

@Component
class McpCapabilityCoverageValidator(
    private val applicationContext: ApplicationContext,
    private val registry: McpCapabilityRegistry
) : InitializingBean {

    private val log = LoggerFactory.getLogger(McpCapabilityCoverageValidator::class.java)

    override fun afterPropertiesSet() {
        val violations = mutableListOf<String>()
        val tools = mutableListOf<String>()
        val resources = mutableListOf<String>()
        applicationContext.beanDefinitionNames.forEach { beanName ->
            val type = runCatching { applicationContext.getType(beanName) }.getOrNull() ?: return@forEach
            ClassUtils.getUserClass(type).declaredMethods.forEach { method ->
                AnnotationUtils.findAnnotation(method, McpTool::class.java)?.let { tool ->
                    tools.add(tool.name)
                    if (registry.tool(tool.name) == null) {
                        violations.add("tool ${tool.name}")
                    }
                    if (AnnotationUtils.findAnnotation(method, GuardedTool::class.java)?.name != tool.name) {
                        violations.add("tool ${tool.name} is missing the GuardedTool declaration")
                    }
                }
                AnnotationUtils.findAnnotation(method, McpResource::class.java)?.let { resource ->
                    resources.add(resource.name)
                    if (registry.resource(resource.name) == null) {
                        violations.add("resource ${resource.name}")
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw UnprotectedMcpCapabilityException(violations)
        }
        log.info(
            "MCP authorization coverage validated for {} tools and {} resources",
            tools.size,
            resources.size
        )
    }
}
