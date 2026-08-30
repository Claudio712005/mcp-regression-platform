package br.com.claus.mcpregressionplatform.infrastructure.llm

import br.com.claus.mcpregressionplatform.application.port.ReasoningModel
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ReasoningModelConfiguration {

    @Bean
    fun reasoningModel(
        chatModels: ObjectProvider<ChatModel>,
        properties: PlatformProperties
    ): ReasoningModel {
        val chatModel = chatModels.getIfAvailable()
        if (!properties.ai.reasoningEnabled || chatModel == null) {
            return DisabledReasoningModel()
        }
        return ChatClientReasoningModel(ChatClient.builder(chatModel).build())
    }
}

class DisabledReasoningModel : ReasoningModel {

    override fun available(): Boolean = false

    override fun reason(systemInstructions: String, userMessage: String): String =
        throw UnsupportedOperationException("No reasoning model is configured")
}

class ChatClientReasoningModel(private val chatClient: ChatClient) : ReasoningModel {

    override fun available(): Boolean = true

    override fun reason(systemInstructions: String, userMessage: String): String =
        chatClient.prompt()
            .system(systemInstructions)
            .user(userMessage)
            .call()
            .content()
            .orEmpty()
}
