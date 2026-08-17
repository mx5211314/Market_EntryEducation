package com.investedu.smartassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investedu.smartassistant.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    // 用 SQL 原地自增，避免"查出来 +1 再写回"在并发下丢计数
    @Update("UPDATE article SET read_count = read_count + 1 WHERE id = #{id}")
    void increaseReadCount(@Param("id") Long id);
}