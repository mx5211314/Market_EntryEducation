package com.investedu.smartassistant.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentProcessor {

    private final DocumentSplitter splitter = new DocumentByParagraphSplitter(512, 128);

    // 提取条款编号的正则
    private static final Pattern RULE_PATTERN = Pattern.compile("第[\\d一二三四五六七八九十百]+条");

    // 风险等级关键词
    private static final Set<String> RED_LINE_WORDS = Set.of(
            "强制平仓", "红线", "禁止", "不得", "严重违规", "刑事责任"
    );

    public List<TextSegment> loadAndSplit(File file, Map<String, String> baseMetadata) throws IOException {
        String text;
        if (file.getName().endsWith(".pdf")) {
            PDDocument pdDoc = PDDocument.load(file);
            text = new PDFTextStripper().getText(pdDoc);
            pdDoc.close();
        } else {
            text = new String(Files.readAllBytes(file.toPath()));
        }
        return split(text, file.getName(), baseMetadata);
    }

    /**
     * 切分内存中的文本，供后台文章入库使用。
     * 文章正文可能是 wangEditor 的 HTML，标签必须先剥掉：否则切片里塞满 &lt;p&gt;、style 属性，
     * 嵌入向量被噪声带偏，检索出来的片段也没法直接给用户看。
     */
    public List<TextSegment> splitText(String rawText, String source, Map<String, String> baseMetadata) {
        String text = stripHtml(rawText);
        if (text.isBlank()) return List.of();
        return split(text, source, baseMetadata);
    }

    private List<TextSegment> split(String text, String source, Map<String, String> baseMetadata) {
        Document doc = Document.from(text, new dev.langchain4j.data.document.Metadata(baseMetadata));
        List<TextSegment> segments = splitter.split(doc);

        for (TextSegment seg : segments) {
            String content = seg.text();

            // 提取条款编号
            Matcher matcher = RULE_PATTERN.matcher(content);
            if (matcher.find()) {
                seg.metadata().put("rule_id", matcher.group());
            }

            // 风险等级标签
            for (String word : RED_LINE_WORDS) {
                if (content.contains(word)) {
                    seg.metadata().put("risk_level", "红线");
                    break;
                }
            }
            if (!seg.metadata().containsKey("risk_level")) {
                seg.metadata().put("risk_level", "普通");
            }

            // 来源文件
            seg.metadata().put("source", source);

            // 继承基础元数据
            for (Map.Entry<String, String> entry : baseMetadata.entrySet()) {
                seg.metadata().put(entry.getKey(), entry.getValue());
            }
        }
        return segments;
    }

    private String stripHtml(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                // 块级标签换成换行，段落切分器靠空行判断边界
                .replaceAll("(?i)</(p|div|h[1-6]|li|tr|blockquote|pre)\\s*>", "\n\n")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}