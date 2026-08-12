package com.investedu.smartassistant.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    @Qualifier("milvusEmbeddingStore")
    private EmbeddingStore<TextSegment> milvusStore;

    @Autowired
    private ElasticsearchEmbeddingStore esStore;

    public void ingest(List<TextSegment> segments) {
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        milvusStore.addAll(embeddings, segments);
        esStore.addAll(embeddings, segments);
    }
}