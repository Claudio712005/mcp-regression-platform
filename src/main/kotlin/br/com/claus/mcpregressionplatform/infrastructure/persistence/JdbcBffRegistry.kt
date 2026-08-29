package br.com.claus.mcpregressionplatform.infrastructure.persistence

import br.com.claus.mcpregressionplatform.application.port.BffRegistry
import br.com.claus.mcpregressionplatform.domain.dependency.BffDefinition
import br.com.claus.mcpregressionplatform.domain.dependency.Criticality
import br.com.claus.mcpregressionplatform.domain.dependency.DependencyType
import br.com.claus.mcpregressionplatform.domain.dependency.ServiceDependency
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class JdbcBffRegistry(private val jdbcClient: JdbcClient) : BffRegistry {

    override fun findAll(): List<BffDefinition> =
        jdbcClient.sql("select name, description from bff_service order by name")
            .query { rs, _ -> rs.getString("name") to rs.getString("description") }
            .list()
            .map { (name, description) -> BffDefinition(name, description, dependenciesOf(name)) }

    override fun findByName(name: String): BffDefinition? =
        jdbcClient.sql("select name, description from bff_service where name = :name")
            .param("name", name)
            .query { rs, _ -> BffDefinition(rs.getString("name"), rs.getString("description"), emptyList()) }
            .optional()
            .map { it.copy(dependencies = dependenciesOf(it.name)) }
            .orElse(null)

    private fun dependenciesOf(bff: String): List<ServiceDependency> =
        jdbcClient.sql(
            """
            select dependency_name, dependency_type, criticality, description
            from bff_dependency
            where bff_name = :bff
            order by position
            """.trimIndent()
        )
            .param("bff", bff)
            .query { rs, _ ->
                ServiceDependency(
                    name = rs.getString("dependency_name"),
                    type = DependencyType.valueOf(rs.getString("dependency_type")),
                    criticality = Criticality.valueOf(rs.getString("criticality")),
                    description = rs.getString("description")
                )
            }
            .list()
}
