package com.investedu.smartassistant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.investedu.smartassistant.entity.Article;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.ArticleService;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleService articleService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public ArticleController(ArticleService articleService, UserService userService, JwtUtil jwtUtil) {
        this.articleService = articleService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
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
        return articleService.listPublished(pageNum, pageSize, category, keyword, sortField, sortDirection);
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
        return article;
    }

    // ==================== 管理端接口（需要管理员权限） ====================
    @PostMapping("/admin/article")
    public Article create(@RequestHeader("Authorization") String authHeader,
                          @RequestBody Article body) {
        checkAdmin(authHeader);
        return articleService.createArticle(body);
    }

    @PutMapping("/admin/article/{id}")
    public Article update(@RequestHeader("Authorization") String authHeader,
                          @PathVariable Long id,
                          @RequestBody Article body) {
        checkAdmin(authHeader);
        return articleService.updateArticle(id, body);
    }

    @PutMapping("/admin/article/{id}/status")
    public Map<String, String> updateStatus(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, Integer> body) {
        checkAdmin(authHeader);
        articleService.updateStatus(id, body.get("status"));
        return Map.of("message", "状态更新成功");
    }

    @DeleteMapping("/admin/article/{id}")
    public Map<String, String> delete(@RequestHeader("Authorization") String authHeader,
                                      @PathVariable Long id) {
        checkAdmin(authHeader);
        articleService.deleteArticle(id);
        return Map.of("message", "删除成功");
    }

    @GetMapping("/admin/article/list")
    public Page<Article> listAll(@RequestHeader("Authorization") String authHeader,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) Integer status) {
        checkAdmin(authHeader);
        return articleService.listAll(pageNum, Math.min(pageSize, 100), keyword, status);
    }

    private void checkAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new RuntimeException("未登录");
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("无权限");
        }
    }
}