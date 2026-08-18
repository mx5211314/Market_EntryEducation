package com.investedu.smartassistant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.investedu.smartassistant.entity.Article;
import com.investedu.smartassistant.service.ArticleIndexService;
import com.investedu.smartassistant.service.ArticleReadService;
import com.investedu.smartassistant.service.ArticleService;
import com.investedu.smartassistant.util.AuthContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleIndexService articleIndexService;
    private final ArticleReadService articleReadService;
    private final AuthContext authContext;

    public ArticleController(ArticleService articleService,
                             ArticleIndexService articleIndexService,
                             ArticleReadService articleReadService,
                             AuthContext authContext) {
        this.articleService = articleService;
        this.articleIndexService = articleIndexService;
        this.articleReadService = articleReadService;
        this.authContext = authContext;
    }

    // ==================== 用户端接口 ====================
    @GetMapping("/user/article/list")
    public Page<Article> listPublished(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        Page<Article> page = articleService.listPublished(pageNum, pageSize, category, keyword, sortField, sortDirection);
        markReadFlags(page.getRecords());
        return page;
    }

    /** 这个接口游客也能访问，登录了才有"已读"标记 */
    private void markReadFlags(List<Article> records) {
        Long userId = authContext.currentUserIdOrNull();
        if (userId == null || records == null || records.isEmpty()) return;
        Set<Long> readIds = articleReadService.readIdsIn(userId,
                records.stream().map(Article::getId).toList());
        records.forEach(a -> a.setReadFlag(readIds.contains(a.getId())));
    }

    @GetMapping("/user/article/categories")
    public List<String> listCategories() {
        return articleService.listCategories();
    }

    @GetMapping("/user/article/{id}")
    public Article getDetail(@PathVariable Long id) {
        Article article = articleService.getById(id);
        if (article == null || article.getStatus() != 1) {
            throw new RuntimeException("文章不存在或未发布");
        }
        articleService.increaseReadCount(id);
        article.setReadCount(article.getReadCount() == null ? 1 : article.getReadCount() + 1);

        Long userId = authContext.currentUserIdOrNull();
        if (userId != null) {
            articleReadService.markRead(userId, id);
            article.setReadFlag(true);
        }
        return article;
    }

    /** 个人中心的学习进度。放在 /api/user/study 下，走 authenticated 规则，游客拿不到 */
    @GetMapping("/user/study/stats")
    public Map<String, Object> studyStats() {
        return articleReadService.studyStats(authContext.requireUserId());
    }

    // ==================== 管理端接口（需要管理员权限） ====================
    @PostMapping("/admin/article")
    public Article create(@RequestBody Article body) {
        authContext.requireAdmin();
        return articleService.createArticle(body);
    }

    @PutMapping("/admin/article/{id}")
    public Article update(@PathVariable Long id,
                          @RequestBody Article body) {
        authContext.requireAdmin();
        return articleService.updateArticle(id, body);
    }

    @PutMapping("/admin/article/{id}/status")
    public Map<String, String> updateStatus(@PathVariable Long id,
                                            @RequestBody Map<String, Integer> body) {
        authContext.requireAdmin();
        articleService.updateStatus(id, body.get("status"));
        return Map.of("message", "状态更新成功");
    }

    @DeleteMapping("/admin/article/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        authContext.requireAdmin();
        articleService.deleteArticle(id);
        return Map.of("message", "删除成功");
    }

    @GetMapping("/admin/article/list")
    public Page<Article> listAll(@RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Integer status) {
        authContext.requireAdmin();
        return articleService.listAll(pageNum, Math.min(pageSize, 100), keyword, status);
    }

    /** 单篇重建向量索引：发布时同步失败（向量库或嵌入服务不可用）后可以手动补 */
    @PostMapping("/admin/article/{id}/reindex")
    public Map<String, Object> reindex(@PathVariable Long id) {
        authContext.requireAdmin();
        return articleIndexService.reindex(id);
    }

    /** 全量重建：立即返回，实际进度由 /reindex-progress 轮询 */
    @PostMapping("/admin/article/reindex-all")
    public Map<String, Object> reindexAll() {
        authContext.requireAdmin();
        return articleIndexService.startReindexAll();
    }

    @GetMapping("/admin/article/reindex-progress")
    public Map<String, Object> reindexProgress() {
        authContext.requireAdmin();
        return articleIndexService.reindexProgress();
    }
}