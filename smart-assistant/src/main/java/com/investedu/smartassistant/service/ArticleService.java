package com.investedu.smartassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.investedu.smartassistant.entity.Article;
import com.investedu.smartassistant.mapper.ArticleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public Article createArticle(String title, String category, String content, String author) {
        Article article = new Article();
        article.setTitle(title);
        article.setCategory(category);
        article.setContent(content);
        article.setAuthor(author == null || author.isEmpty() ? "管理员" : author);
        article.setStatus(0); // 默认草稿
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.insert(article);
        return article;
    }

    public Article updateArticle(Long id, String title, String category, String content, String author) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new RuntimeException("文章不存在");
        if (title != null && !title.isEmpty()) article.setTitle(title);
        if (category != null) article.setCategory(category);
        if (content != null) article.setContent(content);
        if (author != null) article.setAuthor(author);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return article;
    }

    public void updateStatus(Long id, Integer status) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new RuntimeException("文章不存在");
        article.setStatus(status);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }

    // 用户端查询：只显示已发布的文章，支持分类筛选和分页
    public Page<Article> listPublished(int pageNum, int pageSize, String category) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);  // 已发布
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        wrapper.orderByDesc("created_at");
        return articleMapper.selectPage(page, wrapper);
    }

    // 管理端查询：显示所有文章
    public Page<Article> listAll(int pageNum, int pageSize, String keyword) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("title", keyword);
        }
        wrapper.orderByDesc("created_at");
        return articleMapper.selectPage(page, wrapper);
    }

    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }
}