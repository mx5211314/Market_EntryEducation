package com.investedu.smartassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.investedu.smartassistant.entity.Favorite;
import com.investedu.smartassistant.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    public FavoriteService(FavoriteMapper favoriteMapper) {
        this.favoriteMapper = favoriteMapper;
    }

    public void favorite(Long userId, Long articleId) {
        if (isFavorited(userId, articleId)) return;
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setArticleId(articleId);
        f.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(f);
    }

    public void unfavorite(Long userId, Long articleId) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("article_id", articleId);
        favoriteMapper.delete(wrapper);
    }

    public boolean isFavorited(Long userId, Long articleId) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("article_id", articleId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    public IPage<Favorite> pageByUser(Long userId, int pageNum, int pageSize) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at");
        return favoriteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}