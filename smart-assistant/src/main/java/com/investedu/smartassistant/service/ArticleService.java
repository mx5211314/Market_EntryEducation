package com.investedu.smartassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.investedu.smartassistant.entity.Article;
import com.investedu.smartassistant.mapper.ArticleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    // 排序字段白名单：请求参数直接拼进 ORDER BY 会造成 SQL 注入，只允许映射表内的字段
    private static final Map<String, String> SORTABLE = Map.of(
            "publishedAt", "published_at",
            "createdAt", "created_at",
            "updatedAt", "updated_at",
            "readCount", "read_count",
            "title", "title"
    );

    private final ArticleMapper articleMapper;

    public ArticleService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public Article createArticle(Article body) {
        Article article = new Article();
        article.setTitle(body.getTitle());
        article.setCategory(body.getCategory());
        article.setSummary(body.getSummary());
        article.setContent(body.getContent());
        article.setCoverImage(body.getCoverImage());
        article.setTags(body.getTags());
        article.setAuthor(body.getAuthor() == null || body.getAuthor().isBlank() ? "管理员" : body.getAuthor());
        article.setReadCount(0);
        article.setStatus(0); // 默认草稿
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.insert(article);
        return article;
    }

    public Article updateArticle(Long id, Article body) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new RuntimeException("文章不存在");
        if (body.getTitle() != null && !body.getTitle().isBlank()) article.setTitle(body.getTitle());
        if (body.getCategory() != null) article.setCategory(body.getCategory());
        if (body.getSummary() != null) article.setSummary(body.getSummary());
        if (body.getContent() != null) article.setContent(body.getContent());
        if (body.getCoverImage() != null) article.setCoverImage(body.getCoverImage());
        if (body.getTags() != null) article.setTags(body.getTags());
        if (body.getAuthor() != null) article.setAuthor(body.getAuthor());
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return article;
    }

    public void updateStatus(Long id, Integer status) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new RuntimeException("文章不存在");
        article.setStatus(status);
        // 首次发布时记录发布时间，列表页默认按它排序
        if (status != null && status == 1 && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }

    // 用户端查询：只显示已发布的文章，支持分类、关键词、排序
    public Page<Article> listPublished(int pageNum, int pageSize, String category,
                                       String keyword, String sortField, String sortDirection) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);  // 已发布
        if (category != null && !category.isBlank()) {
            wrapper.eq("category", category);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("title", keyword)
                    .or().like("summary", keyword)
                    .or().like("tags", keyword));
        }
        applySort(wrapper, sortField, sortDirection);
        return articleMapper.selectPage(page, wrapper);
    }

    // 管理端查询：显示所有文章
    public Page<Article> listAll(int pageNum, int pageSize, String keyword, Integer status) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 后台找文章常靠记得某个标签或摘要里的词，只搜标题太窄
            wrapper.and(w -> w.like("title", kw).or().like("summary", kw)
                    .or().like("category", kw).or().like("tags", kw));
        }
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("created_at");
        return articleMapper.selectPage(page, wrapper);
    }

    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    // 收藏列表要一次性补上本页文章的标题分类，逐条 selectById 会打出一页条数的查询
    public Map<Long, Article> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return articleMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Article::getId, a -> a, (a, b) -> a));
    }

    // 前台分类筛选用：只列出已发布文章里真实存在的分类
    public List<String> listCategories() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT category")
                .eq("status", 1)
                .isNotNull("category")
                .ne("category", "")
                .orderByAsc("category");
        return articleMapper.selectObjs(wrapper).stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }

    public void increaseReadCount(Long id) {
        articleMapper.increaseReadCount(id);
    }

    private void applySort(QueryWrapper<Article> wrapper, String sortField, String sortDirection) {
        String column = SORTABLE.get(sortField);
        boolean asc = "asc".equalsIgnoreCase(sortDirection);
        if (column == null) {
            // 未发布时间的老数据排在后面，避免默认排序把它们顶到最前
            wrapper.orderByDesc("published_at", "created_at");
            return;
        }
        wrapper.orderBy(true, asc, column);
    }
}