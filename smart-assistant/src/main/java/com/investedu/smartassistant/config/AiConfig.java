package com.investedu.smartassistant.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.timeout}")
    private Duration timeout;

    @Value("${langchain4j.open-ai.chat-model.max-tokens}")
    private Integer maxTokens;

    @Value("${langchain4j.open-ai.chat-model.temperature}")
    private Double temperature;

    @Value("${langchain4j.dashscope.embedding-model.api-key}")
    private String dashscopeApiKey;

    @Value("${langchain4j.dashscope.embedding-model.model-name}")
    private String embeddingModelName;

    @Value("${milvus.host}")
    private String milvusHost;

    @Value("${milvus.port}")
    private int milvusPort;

    @Value("${milvus.collection-name}")
    private String milvusCollection;

    @Value("${elasticsearch.host}")
    private String esHost;

    @Value("${elasticsearch.port}")
    private int esPort;

    @Value("${elasticsearch.index-name}")
    private String esIndex;

    @Value("${langchain4j.dashscope.embedding-model.dimensions:1024}")
    private int embeddingDimension;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(embeddingModelName)
                .build();
    }

    @Bean
    @Primary
    public EmbeddingStore<TextSegment> milvusEmbeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(milvusHost)
                .port(milvusPort)
                .collectionName(milvusCollection)
                .dimension(embeddingDimension)
                .build();
    }

    @Bean
    public ElasticsearchEmbeddingStore esEmbeddingStore() {
        return ElasticsearchEmbeddingStore.builder()
                .serverUrl("http://" + esHost + ":" + esPort)
                .indexName(esIndex)
                .dimension(embeddingDimension)
                .build();
    }

}