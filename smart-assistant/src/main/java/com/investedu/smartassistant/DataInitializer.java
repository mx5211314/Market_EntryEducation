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
    public void run(String... args) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:docs/*");
        if (resources.length == 0) {
            System.out.println("未找到知识文档，跳过导入");
            return;
        }
        for (Resource res : resources) {
            File file = res.getFile();
            var segments = documentProcessor.loadAndSplit(file, Map.of("category", "finance_rules"));
            vectorStoreService.ingest(segments);
            System.out.println("已向量化入库: " + file.getName());
        }
    }
}