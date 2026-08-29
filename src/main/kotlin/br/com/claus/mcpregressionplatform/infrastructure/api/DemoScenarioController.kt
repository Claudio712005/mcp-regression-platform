package br.com.claus.mcpregressionplatform.infrastructure.api

import br.com.claus.mcpregressionplatform.infrastructure.demo.DemoScenario
import br.com.claus.mcpregressionplatform.infrastructure.demo.DemoScenarioHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class ScenarioResponse(val scenario: String, val available: List<String>)

@RestController
@RequestMapping("/internal/demo")
@PreAuthorize("hasRole('ARCHITECT')")
class DemoScenarioController(private val holder: DemoScenarioHolder) {

    @GetMapping("/scenario")
    fun current(): ScenarioResponse = ScenarioResponse(holder.current().id, DemoScenario.entries.map { it.id })

    @PutMapping("/scenario/{scenario}")
    fun switch(@PathVariable scenario: String): ScenarioResponse =
        ScenarioResponse(holder.switch(DemoScenario.from(scenario)).id, DemoScenario.entries.map { it.id })
}
