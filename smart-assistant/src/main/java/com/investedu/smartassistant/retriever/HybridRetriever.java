package com.investedu.smartassistant.retriever;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class HybridRetriever {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    @Qualifier("milvusEmbeddingStore")
    private EmbeddingStore<TextSegment> milvusStore;

    // 暂时不用 ES 检索，保留双写即可
    // @Autowired
    // private ElasticsearchEmbeddingStore esStore;

    public List<TextSegment> retrieve(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 1. Milvus 向量检索（多召回一些用于重排）
        List<TextSegment> milvusResults = milvusStore.search(
                        EmbeddingSearchRequest.builder()
                                .queryEmbedding(queryEmbedding)
                                .maxResults(maxResults * 2)
                                .build())
                .matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());

        // 2. 去重
        Set<String> seen = new HashSet<>();
        List<TextSegment> combined = new ArrayList<>();
        for (TextSegment seg : milvusResults) {
            if (seen.add(seg.text())) {
                combined.add(seg);
            }
        }

        // 3. 本地 Embedding 重排序
        return rerankByEmbedding(query, combined, maxResults);
    }

    private List<TextSegment> rerankByEmbedding(String query, List<TextSegment> candidates, int topN) {
        if (candidates.isEmpty()) return candidates;
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<Embedding> candEmbeddings = embeddingModel.embedAll(candidates).content();

        List<AbstractMap.SimpleEntry<TextSegment, Double>> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            double sim = cosine(queryEmbedding.vector(), candEmbeddings.get(i).vector());
            scored.add(new AbstractMap.SimpleEntry<>(candidates.get(i), sim));
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return scored.stream().limit(topN).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-10);
    }
}