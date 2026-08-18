package com.investedu.smartassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investedu.smartassistant.entity.KbVectorRef;
import com.investedu.smartassistant.mapper.KbVectorRefMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    public static final String REF_ARTICLE = "article";
    public static final String REF_DOC = "doc";

    private static final String MILVUS = "milvus";
    private static final String ES = "es";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    @Qualifier("milvusEmbeddingStore")
    private EmbeddingStore<TextSegment> milvusStore;

    @Autowired
    private ElasticsearchEmbeddingStore esStore;

    @Autowired
    private KbVectorRefMapper refMapper;

    /** 灌过就跳过：DataInitializer 每次启动都重跑，没有这道判断向量库里会攒下 N 份重复片段 */
    public boolean hasIngested(String refType, String refId) {
        return refMapper.selectCount(new QueryWrapper<KbVectorRef>()
                .eq("ref_type", refType).eq("ref_id", refId)) > 0;
    }

    /**
     * 入库并记账。先删旧的再灌新的，重复调用等价于重建这个来源的索引。
     * 嵌入或向量库不可用时抛异常，由调用方决定是否要因此中断业务。
     */
    public void ingestFor(String refType, String refId, List<TextSegment> segments) {
        removeFor(refType, refId);
        if (segments == null || segments.isEmpty()) return;

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        record(refType, refId, MILVUS, milvusStore.addAll(embeddings, segments), segments.size());
        record(refType, refId, ES, esStore.addAll(embeddings, segments), segments.size());
    }

    /** 删除该来源在两个库里的全部向量。文章下架或删除后，AI 不该再引用它 */
    public void removeFor(String refType, String refId) {
        List<KbVectorRef> refs = refMapper.selectList(new QueryWrapper<KbVectorRef>()
                .eq("ref_type", refType).eq("ref_id", refId));
        for (KbVectorRef ref : refs) {
            List<String> ids = parseIds(ref.getVectorIds());
            if (!ids.isEmpty()) {
                try {
                    if (MILVUS.equals(ref.getStore())) milvusStore.removeAll(ids);
                    else esStore.removeAll(ids);
                } catch (Exception e) {
                    // 台账照删：留着只会让下次删除重复尝试同一批已失效的 id
                    log.warn("删除向量失败 {}:{} store={} -> {}", refType, refId, ref.getStore(), e.getMessage());
                }
            }
            refMapper.deleteById(ref.getId());
        }
    }

    private void record(String refType, String refId, String store, List<String> ids, int count) {
        KbVectorRef ref = new KbVectorRef();
        ref.setRefType(refType);
        ref.setRefId(refId);
        ref.setStore(store);
        ref.setSegmentCount(count);
        ref.setCreatedAt(LocalDateTime.now());
        try {
            ref.setVectorIds(objectMapper.writeValueAsString(ids == null ? List.of() : ids));
        } catch (Exception e) {
            ref.setVectorIds("[]");
        }
        refMapper.insert(ref);
    }

    private List<String> parseIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
