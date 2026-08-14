package com.investedu.smartassistant;

import org.junit.jupiter.api.Test;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartAssistantApplicationTests {

    @MockBean
    private ChatLanguageModel chatLanguageModel;

    @MockBean
    private EmbeddingModel embeddingModel;

    @MockBean(name = "milvusEmbeddingStore")
    private EmbeddingStore<TextSegment> milvusEmbeddingStore;

    @MockBean
    private ElasticsearchEmbeddingStore esEmbeddingStore;

    @Test
    void contextLoads() {
    }

}
