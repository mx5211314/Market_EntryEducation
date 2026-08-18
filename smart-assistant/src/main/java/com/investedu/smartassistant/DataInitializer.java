package com.investedu.smartassistant;

import com.investedu.smartassistant.service.DocumentProcessor;
import com.investedu.smartassistant.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.File;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.data-init.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DocumentProcessor documentProcessor;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Override
    public void run(String... args) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:docs/*");
            if (resources.length == 0) {
                System.out.println("未找到知识文档，跳过导入");
                return;
            }
            for (Resource res : resources) {
                try {
                    File file = res.getFile();
                    // 灌过就跳过：这里每次启动都会重跑，无条件重灌会让向量库里攒下 N 份重复片段，
                    // 检索 top4 可能全是同一段的副本
                    if (vectorStoreService.hasIngested(VectorStoreService.REF_DOC, file.getName())) {
                        System.out.println("已存在向量，跳过: " + file.getName());
                        continue;
                    }
                    var segments = documentProcessor.loadAndSplit(file, Map.of("category", "finance_rules"));
                    vectorStoreService.ingestFor(VectorStoreService.REF_DOC, file.getName(), segments);
                    System.out.println("已向量化入库: " + file.getName());
                } catch (Exception ex) {
                    // 单个文档入库失败不阻断应用启动（如 DashScope 免费额度用尽、向量库不可用等）
                    System.err.println("知识文档向量化入库失败，跳过: " + res.getFilename() + " -> " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            // 文档初始化整体失败时仅告警，保证应用正常启动
            System.err.println("知识文档初始化失败，应用仍将继续启动: " + e.getMessage());
        }
    }
}