package com.investedu.smartassistant.retriever;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class HybridRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridRetriever.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    @Qualifier("milvusEmbeddingStore")
    private EmbeddingStore<TextSegment> milvusStore;

    @Value("${elasticsearch.host}")
    private String esHost;

    @Value("${elasticsearch.port}")
    private int esPort;

    @Value("${elasticsearch.index-name}")
    private String esIndex;

    public List<TextSegment> retrieve(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 1. Milvus 向量检索
        List<TextSegment> milvusResults = milvusStore.search(
                        EmbeddingSearchRequest.builder()
                                .queryEmbedding(queryEmbedding)
                                .maxResults(maxResults * 2)
                                .build())
                .matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());

        // 2. ES BM25 关键词检索
        List<TextSegment> esResults = esKeywordSearch(query, maxResults * 2);

        // 3. 合并去重（Milvus 优先）
        Set<String> seen = new HashSet<>();
        List<TextSegment> combined = new ArrayList<>();
        for (TextSegment seg : milvusResults) {
            if (seen.add(seg.text())) combined.add(seg);
        }
        for (TextSegment seg : esResults) {
            if (seen.add(seg.text())) combined.add(seg);
        }

        // 4. 本地 Embedding 重排序
        return rerankByEmbedding(queryEmbedding, combined, maxResults);
    }

    private List<TextSegment> esKeywordSearch(String query, int size) {
        String url = "http://" + esHost + ":" + esPort + "/" + esIndex + "/_search";
        Map<String, Object> body = new HashMap<>();
        body.put("size", size);
        body.put("query", Map.of("match", Map.of("text", query)));
        try {
            Map<String, Object> resp = restTemplate.postForObject(url, body, Map.class);
            if (resp == null) return List.of();
            Map<String, Object> hits = (Map<String, Object>) resp.get("hits");
            if (hits == null) return List.of();
            List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");
            if (hitList == null) return List.of();
            return hitList.stream()
                    .map(h -> {
                        Map<String, Object> source = (Map<String, Object>) h.get("_source");
                        if (source == null) return null;
                        String text = (String) source.get("text");
                        if (text == null) return null;
                        // metadata 必须一起带出来：丢了它答案就没法标注来源、也没法跳回原文
                        return TextSegment.from(text, toMetadata(source.get("metadata")));
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("ES BM25 检索失败，降级为仅 Milvus 检索: {}", e.getMessage());
            return List.of();
        }
    }

    private Metadata toMetadata(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) return new Metadata();
        Map<String, String> flat = new HashMap<>();
        map.forEach((k, v) -> {
            if (k != null && v != null) flat.put(String.valueOf(k), String.valueOf(v));
        });
        return new Metadata(flat);
    }

    // 查询向量由调用方传入：这里再 embed 一次等于对同一句话多花一次嵌入调用
    private List<TextSegment> rerankByEmbedding(Embedding queryEmbedding, List<TextSegment> candidates, int topN) {
        if (candidates.isEmpty()) return candidates;
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