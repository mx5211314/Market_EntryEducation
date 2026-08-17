package com.investedu.smartassistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.investedu.smartassistant.entity.Article;
import com.investedu.smartassistant.entity.Favorite;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.ArticleService;
import com.investedu.smartassistant.service.FavoriteService;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final ArticleService articleService;
    private final JwtUtil jwtUtil;

    public FavoriteController(FavoriteService favoriteService, UserService userService,
                              ArticleService articleService, JwtUtil jwtUtil) {
        this.favoriteService = favoriteService;
        this.userService = userService;
        this.articleService = articleService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/{articleId}")
    public Map<String, String> favorite(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable Long articleId) {
        Long userId = getUserId(authHeader);
        favoriteService.favorite(userId, articleId);
        return Map.of("message", "收藏成功");
    }

    @DeleteMapping("/{articleId}")
    public Map<String, String> unfavorite(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable Long articleId) {
        Long userId = getUserId(authHeader);
        favoriteService.unfavorite(userId, articleId);
        return Map.of("message", "已取消收藏");
    }

    @GetMapping("/check/{articleId}")
    public Map<String, Object> check(@RequestHeader("Authorization") String authHeader,
                                     @PathVariable Long articleId) {
        Long userId = getUserId(authHeader);
        return Map.of("favorited", favoriteService.isFavorited(userId, articleId));
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestHeader("Authorization") String authHeader,
                                    @RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getUserId(authHeader);
        IPage<Favorite> page = favoriteService.pageByUser(userId, pageNum, Math.min(pageSize, 50));
        Map<Long, Article> articles = articleService.mapByIds(
                page.getRecords().stream().map(Favorite::getArticleId).toList());

        List<Map<String, Object>> records = page.getRecords().stream().map(f -> {
            Article article = articles.get(f.getArticleId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("articleId", f.getArticleId());
            map.put("createdAt", f.getCreatedAt());
            map.put("title", article == null ? null : article.getTitle());
            map.put("category", article == null ? null : article.getCategory());
            map.put("summary", article == null ? null : article.getSummary());
            // 收藏之后文章可能被下架或删掉，标出来，前端就不让点进详情页吃报错
            map.put("available", article != null && Integer.valueOf(1).equals(article.getStatus()));
            return map;
        }).toList();

        // 和其他分页接口保持一致的 records/total 结构
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return result;
    }

    private Long getUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        return user.getId();
    }
}