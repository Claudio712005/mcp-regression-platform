package br.com.claus.mcpregressionplatform.infrastructure.contract

import br.com.claus.mcpregressionplatform.domain.contract.ApiContract
import br.com.claus.mcpregressionplatform.domain.contract.ContractHttpMethod
import br.com.claus.mcpregressionplatform.domain.contract.ContractOperation
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@Component
class OpenApiContractParser(private val jsonMapper: JsonMapper) {

    fun parse(service: String, document: String): ApiContract {
        val root = jsonMapper.readTree(document)
        val version = root.path("info").path("version").asString("unknown")
        val components = root.path("components").path("schemas")
        val operations = mutableListOf<ContractOperation>()
        val paths = root.path("paths")
        paths.propertyNames().forEach { path ->
            val pathNode = paths.path(path)
            pathNode.propertyNames().forEach { method ->
                val parsedMethod = methodOf(method) ?: return@forEach
                operations.add(parseOperation(path, parsedMethod, pathNode.path(method), components))
            }
        }
        return ApiContract(service, version, operations)
    }

    private fun methodOf(value: String): ContractHttpMethod? =
        ContractHttpMethod.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }

    private fun parseOperation(
        path: String,
        method: ContractHttpMethod,
        node: JsonNode,
        components: JsonNode
    ): ContractOperation {
        val requiredParameters = node.path("parameters")
            .filter { it.path("required").asBoolean(false) }
            .map { it.path("name").asString("") }
            .filter { it.isNotBlank() }
            .toSet()
        val responses = node.path("responses")
        val successStatus = responses.propertyNames()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 200..299 }
            .minOrNull() ?: 0
        val schema = responses.path(successStatus.toString())
            .path("content")
            .path("application/json")
            .path("schema")
        return ContractOperation(
            path = path,
            method = method,
            requiredParameters = requiredParameters,
            successStatus = successStatus,
            responseFields = resolveFields(schema, components)
        )
    }

    private fun resolveFields(schema: JsonNode, components: JsonNode): Set<String> {
        val resolved = dereference(schema, components)
        val properties = resolved.path("properties")
        if (properties.isObject) {
            return properties.propertyNames().toSet()
        }
        if (resolved.path("type").asString("") == "array") {
            return resolveFields(resolved.path("items"), components)
        }
        return emptySet()
    }

    private fun dereference(schema: JsonNode, components: JsonNode): JsonNode {
        val reference = schema.path("\$ref").asString("")
        if (reference.isBlank()) {
            return schema
        }
        val name = reference.substringAfterLast('/')
        return components.path(name)
    }
}
