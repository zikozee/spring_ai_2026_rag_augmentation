package com.zee.springai.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @dev : Ezekiel Eromosei
 * @date : 09 Oct, 2026
 */

@Slf4j
@Configuration
public class AIProviderConfig {

    @Bean("aiEmbeddingModel")
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai", matchIfMissing = true)
    EmbeddingModel openAIEmbeddingModel(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel){
        log.info("using 'OPENAI' embedding model");
        return embeddingModel;
    }

    @Bean("aiEmbeddingModel")
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
    EmbeddingModel ollamEmbeddingModel(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel){
        log.info("using 'OLLAMA' embedding model");
        return embeddingModel;
    }

    @Bean("aiChatModel")
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai", matchIfMissing = true)
    ChatModel openAIChatModel(@Qualifier("openAiChatModel") ChatModel chatModel){
        log.info("using 'OPENAI' chat model");
        return chatModel;
    }

    @Bean("aiChatModel")
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
    ChatModel ollamaChatModel(@Qualifier("ollamaChatModel") ChatModel chatModel){
        log.info("using 'OLLAMA' chat model");
        return chatModel;
    }
}
