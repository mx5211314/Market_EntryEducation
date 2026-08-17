package com.investedu.smartassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.investedu.smartassistant.entity.*;
import com.investedu.smartassistant.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DashboardService {

    // 看板每块都单独 try 住，某张表出问题不至于整个页面白屏；但失败必须留日志，
    // 否则前台只看到 0，没人知道是真的没数据还是 SQL 挂了
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RiskAssessmentMapper riskAssessmentMapper;
    private final DiaryMapper diaryMapper;

    public DashboardService(UserMapper userMapper, ArticleMapper articleMapper,
                            ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper,
                            RiskAssessmentMapper riskAssessmentMapper, DiaryMapper diaryMapper) {
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.riskAssessmentMapper = riskAssessmentMapper;
        this.diaryMapper = diaryMapper;
    }

    public Map<String, Object> getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();

        Map<String, Object> stats = new HashMap<>();

        try {
            // 用户
            long totalUsers = userMapper.selectCount(null);
            long todayUsers = userMapper.selectCount(new QueryWrapper<User>().ge("created_at", todayStart));
            long adminUsers = userMapper.selectCount(new QueryWrapper<User>().eq("role", "ADMIN"));
            long normalUsers = totalUsers - adminUsers;
            // 活跃 = 近 7 天真的来问过问题的人，之前直接等于总用户数，等于这个指标没意义
            long activeUsers = countDistinctUsers(chatMessageMapper.selectMaps(
                    new QueryWrapper<ChatMessage>()
                            .select("count(distinct user_id) as cnt")
                            .ge("created_at", weekStart)));

            stats.put("totalUsers", totalUsers);
            stats.put("todayUsers", todayUsers);
            stats.put("adminUsers", adminUsers);
            stats.put("normalUsers", normalUsers);
            stats.put("activeUsers", activeUsers);
        } catch (Exception e) {
            log.warn("看板用户统计失败", e);
            stats.put("totalUsers", 0L);
            stats.put("todayUsers", 0L);
            stats.put("adminUsers", 0L);
            stats.put("normalUsers", 0L);
            stats.put("activeUsers", 0L);
        }

        try {
            // 文章
            long totalArticles = articleMapper.selectCount(null);
            long publishedArticles = articleMapper.selectCount(new QueryWrapper<Article>().eq("status", 1));
            stats.put("totalArticles", totalArticles);
            stats.put("publishedArticles", publishedArticles);
        } catch (Exception e) {
            log.warn("看板文章统计失败", e);
            stats.put("totalArticles", 0L);
            stats.put("publishedArticles", 0L);
        }

        try {
            // 会话与消息
            long totalSessions = chatSessionMapper.selectCount(null);
            long totalMessages = chatMessageMapper.selectCount(null);
            long todayMessages = chatMessageMapper.selectCount(new QueryWrapper<ChatMessage>().ge("created_at", todayStart));
            long weekSessions = chatSessionMapper.selectCount(new QueryWrapper<ChatSession>().ge("created_at", weekStart));
            stats.put("totalSessions", totalSessions);
            stats.put("totalMessages", totalMessages);
            stats.put("todayMessages", todayMessages);
            stats.put("weekSessions", weekSessions);
        } catch (Exception e) {
            log.warn("看板会话消息统计失败", e);
            stats.put("totalSessions", 0L);
            stats.put("totalMessages", 0L);
            stats.put("todayMessages", 0L);
            stats.put("weekSessions", 0L);
        }

        try {
            // 测评
            long totalAssessments = riskAssessmentMapper.selectCount(null);
            Map<String, Long> levelDist = groupCount(riskAssessmentMapper.selectMaps(
                    new QueryWrapper<RiskAssessment>().select("level", "count(*) as cnt").groupBy("level")));
            stats.put("totalAssessments", totalAssessments);
            stats.put("levelDist", levelDist);
        } catch (Exception e) {
            log.warn("看板测评统计失败", e);
            stats.put("totalAssessments", 0L);
            stats.put("levelDist", new HashMap<>());
        }

        try {
            // 日记
            long totalDiaries = diaryMapper.selectCount(null);
            long todayNewDiaries = diaryMapper.selectCount(new QueryWrapper<Diary>().ge("created_at", todayStart));
            Map<String, Long> sentimentDist = groupCount(diaryMapper.selectMaps(
                    new QueryWrapper<Diary>().select("sentiment", "count(*) as cnt").groupBy("sentiment")));
            stats.put("totalDiaries", totalDiaries);
            stats.put("todayNewDiaries", todayNewDiaries);
            stats.put("sentimentDist", sentimentDist);
        } catch (Exception e) {
            log.warn("看板日记统计失败", e);
            stats.put("totalDiaries", 0L);
            stats.put("todayNewDiaries", 0L);
            stats.put("sentimentDist", new HashMap<>());
        }

        try {
            // 近 7 天消息趋势
            List<Map<String, Object>> trendRows = chatMessageMapper.selectMaps(
                    new QueryWrapper<ChatMessage>()
                            .select("date_format(created_at, '%Y-%m-%d') as day", "count(*) as cnt")
                            .ge("created_at", weekStart)
                            .groupBy("day")
                            .orderByAsc("day"));
            Map<String, Long> rawTrend = groupCount(trendRows);
            Map<String, Long> fullTrend = new LinkedHashMap<>();
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                String day = today.minusDays(i).toString();
                fullTrend.put(day, rawTrend.getOrDefault(day, 0L));
            }
            stats.put("messageTrend", fullTrend);
        } catch (Exception e) {
            log.warn("看板消息趋势统计失败", e);
            stats.put("messageTrend", new HashMap<>());
        }

        try {
            // 近 7 天四条真实曲线：活跃用户、新增用户、写日记用户、新建会话
            // 原来只有活跃用户是真的，新增和日记两条是前端 Math.random() 编的
            Map<String, Long> rawActive = groupCount(chatMessageMapper.selectMaps(
                    new QueryWrapper<ChatMessage>()
                            .select("date_format(created_at, '%Y-%m-%d') as day", "count(distinct user_id) as cnt")
                            .ge("created_at", weekStart).groupBy("day")));
            Map<String, Long> rawNewUsers = groupCount(userMapper.selectMaps(
                    new QueryWrapper<User>()
                            .select("date_format(created_at, '%Y-%m-%d') as day", "count(*) as cnt")
                            .ge("created_at", weekStart).groupBy("day")));
            Map<String, Long> rawDiaryUsers = groupCount(diaryMapper.selectMaps(
                    new QueryWrapper<Diary>()
                            .select("date_format(created_at, '%Y-%m-%d') as day", "count(distinct user_id) as cnt")
                            .ge("created_at", weekStart).groupBy("day")));
            Map<String, Long> rawSessions = groupCount(chatSessionMapper.selectMaps(
                    new QueryWrapper<ChatSession>()
                            .select("date_format(created_at, '%Y-%m-%d') as day", "count(*) as cnt")
                            .ge("created_at", weekStart).groupBy("day")));

            Map<String, Map<String, Long>> activityData = new LinkedHashMap<>();
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                String day = today.minusDays(i).toString();
                Map<String, Long> dayData = new LinkedHashMap<>();
                dayData.put("activeUsers", rawActive.getOrDefault(day, 0L));
                dayData.put("newUsers", rawNewUsers.getOrDefault(day, 0L));
                dayData.put("diaryUsers", rawDiaryUsers.getOrDefault(day, 0L));
                dayData.put("sessionCount", rawSessions.getOrDefault(day, 0L));
                activityData.put(day, dayData);
            }
            stats.put("userActivity", activityData);
        } catch (Exception e) {
            log.warn("看板活跃度趋势统计失败", e);
            stats.put("userActivity", new HashMap<>());
        }

        return stats;
    }

    // count(distinct user_id) 走 selectMaps 出来是单行单列，取第一个数字即可
    private long countDistinctUsers(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return 0L;
        Object v = rows.get(0).get("cnt");
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private Map<String, Long> groupCount(List<Map<String, Object>> rows) {        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = null;
            Long value = null;
            for (Map.Entry<String, Object> e : row.entrySet()) {
                Object v = e.getValue();
                if (v == null) continue;
                if (v instanceof Number) {
                    if (value == null) value = ((Number) v).longValue();
                } else if (key == null) {
                    key = String.valueOf(v);
                }
            }
            if (key != null && value != null) result.put(key, value);
        }
        return result;
    }
}