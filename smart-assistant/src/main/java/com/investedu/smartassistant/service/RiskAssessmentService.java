package com.investedu.smartassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investedu.smartassistant.entity.AssessmentQuestion;
import com.investedu.smartassistant.entity.RiskAssessment;
import com.investedu.smartassistant.mapper.AssessmentQuestionMapper;
import com.investedu.smartassistant.mapper.RiskAssessmentMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    /** 评估结果有效期（月）。到期后前端提示重新测评 */
    public static final int VALID_MONTHS = 12;

    private static final String[] PRODUCT_NAMES = {
            "低风险（国债、货币基金）",
            "中低风险（债券基金、银行理财）",
            "中风险（混合基金、可转债）",
            "中高风险（股票、股票型基金）",
            "高风险（融资融券、期货、期权）"
    };

    /** 五级分类沿用行业惯例 C1-C5，与产品风险等级 R1-R5 一一对应 */
    public static final List<LevelInfo> LEVELS = List.of(
            new LevelInfo("C1", 1, "保守型",
                    "您几乎不能承受本金损失，追求资金安全与流动性。",
                    "仅建议参与国债、货币基金、银行现金管理类产品",
                    List.of("投资基础", "法规解读")),
            new LevelInfo("C2", 2, "谨慎型",
                    "您可以接受很小幅度的波动，但对本金亏损较为敏感。",
                    "建议以债券基金、银行理财为主，股票仓位保持低位",
                    List.of("投资基础", "风险管理")),
            new LevelInfo("C3", 3, "稳健型",
                    "您能承受一定波动，愿意用有限的风险换取高于存款的回报。",
                    "可参与混合型基金、可转债、指数基金等中等风险品种",
                    List.of("风险管理", "产品分析")),
            new LevelInfo("C4", 4, "积极型",
                    "您愿意承受较大波动以追求较高长期回报，具备一定投资经验。",
                    "可参与股票、股票型基金、科创板等中高风险品种",
                    List.of("交易规则", "产品分析")),
            new LevelInfo("C5", 5, "激进型",
                    "您能承受大幅本金损失，追求高收益并熟悉杠杆类工具。",
                    "可参与融资融券、期货期权等高风险业务，请务必控制仓位",
                    List.of("交易规则", "风险管理"))
    );

    /** 雷达图的四个维度，顺序即前端展示顺序 */
    public static final List<String> DIMENSIONS = List.of("财务实力", "知识与经验", "风险态度", "投资规划");

    // 每维度按得分率给一句短评：低 / 中 / 高
    private static final Map<String, String[]> DIMENSION_COMMENTS = Map.of(
            "财务实力", new String[]{
                    "可投资资产与结余有限，建议先建立应急储备再谈投资",
                    "收支与资产结构基本健康，可拿出部分闲钱做中长期配置",
                    "资产厚度与现金流充足，具备承担波动的物质基础"},
            "知识与经验", new String[]{
                    "对金融产品还比较陌生，建议从知识库的投资基础读起",
                    "了解常见品种，但对风险结构的理解还可以更系统",
                    "具备较成熟的投资认知，可关注更复杂的产品与规则"},
            "风险态度", new String[]{
                    "对本金亏损非常敏感，适合以保本类品种为主",
                    "能接受有限波动，需提前设定好止损与仓位纪律",
                    "对波动的心理承受力较强，但仍需避免满仓与追高"},
            "投资规划", new String[]{
                    "资金期限偏短、目标偏保守，不宜配置流动性差的品种",
                    "有一定的期限规划，建议明确各笔资金的用途与退出时点",
                    "长期规划清晰，可通过分批建仓平滑择时风险"}
    );

    public static class LevelInfo {
        public final String code;
        public final int index;
        public final String name;
        public final String summary;
        public final String biz;
        public final List<String> categories;

        LevelInfo(String code, int index, String name, String summary, String biz, List<String> categories) {
            this.code = code;
            this.index = index;
            this.name = name;
            this.summary = summary;
            this.biz = biz;
            this.categories = categories;
        }
    }

    private final AssessmentQuestionMapper questionMapper;
    private final RiskAssessmentMapper assessmentMapper;
    private final ObjectMapper objectMapper;

    public RiskAssessmentService(AssessmentQuestionMapper questionMapper,
                                 RiskAssessmentMapper assessmentMapper,
                                 ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.assessmentMapper = assessmentMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initDefaultQuestions() {
        try {
            Long count = questionMapper.selectCount(null);
            if (count != null && count == 0) {
                List<Map<String, Object>> defaults = buildDefaultQuestions();
                int order = 1;
                for (Map<String, Object> q : defaults) {
                    AssessmentQuestion question = new AssessmentQuestion();
                    question.setText((String) q.get("text"));
                    question.setOptionsJson(objectMapper.writeValueAsString(q.get("options")));
                    question.setDimension((String) q.get("dimension"));
                    question.setSortOrder(order++);
                    question.setStatus(1);
                    questionMapper.insert(question);
                }
                log.info("已初始化 {} 道风险测评题目", defaults.size());
            }
        } catch (Exception e) {
            log.warn("题库初始化失败（请确认已建表）：{}", e.getMessage());
        }
    }

    public List<Map<String, Object>> getQuestions() {
        QueryWrapper<AssessmentQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort_order");
        List<AssessmentQuestion> questions = questionMapper.selectList(wrapper);
        return questions.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("text", q.getText());
            map.put("dimension", q.getDimension() == null ? "" : q.getDimension());
            try {
                List<Map<String, Object>> options = objectMapper.readValue(
                        q.getOptionsJson(), new TypeReference<List<Map<String, Object>>>() {});
                // 不向前端暴露分值，防止篡改
                List<Map<String, Object>> safe = options.stream().map(o -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("value", o.get("value"));
                    item.put("label", o.get("label"));
                    return item;
                }).collect(Collectors.toList());
                map.put("options", safe);
            } catch (Exception e) {
                map.put("options", List.of());
            }
            return map;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> submit(Long userId, List<Integer> answers) {
        List<AssessmentQuestion> questions = listActiveQuestions();
        if (questions.isEmpty()) throw new RuntimeException("题库为空，请联系管理员");
        if (answers == null || answers.size() != questions.size()) {
            throw new RuntimeException("答案数量与题目不匹配");
        }

        int total = 0;
        int maxTotal = 0;
        for (int i = 0; i < questions.size(); i++) {
            total += scoreForQuestion(questions.get(i), answers.get(i));
            maxTotal += maxScoreForQuestion(questions.get(i));
        }

        LevelInfo info = levelFor(total, maxTotal);
        LocalDateTime now = LocalDateTime.now();
        // 适当性办法要求评估结果有有效期，实务上券商普遍按 12 个月
        LocalDateTime expiresAt = now.plusMonths(VALID_MONTHS);
        String reportNo = buildReportNo(userId, now);

        RiskAssessment ra = new RiskAssessment();
        ra.setUserId(userId);
        ra.setReportNo(reportNo);
        ra.setLevel(info.name);
        ra.setScore(total);
        ra.setMaxScore(maxTotal);
        try {
            ra.setDetail(objectMapper.writeValueAsString(answers));
        } catch (Exception e) {
            ra.setDetail("");
        }
        ra.setCreatedAt(now);
        ra.setSignedAt(now);
        ra.setExpiresAt(expiresAt);
        assessmentMapper.insert(ra);

        Map<String, Object> result = new HashMap<>(
                describe(info, total, maxTotal, expiresAt, dimensionBreakdown(questions, answers), reportNo));
        result.put("createdAt", now.toString());
        result.put("signedAt", now.toString());
        result.put("validMonths", VALID_MONTHS);
        return result;
    }

    /** 报告编号：RA + 时间 + 用户号，前端展示、客服问询都靠它定位 */
    private String buildReportNo(Long userId, LocalDateTime now) {
        return "RA" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", userId == null ? 0 : userId % 10000);
    }

    /** 按四个维度分别算得分率，结果页雷达图用 */
    public List<Map<String, Object>> dimensionBreakdown(List<AssessmentQuestion> questions, List<Integer> answers) {
        if (questions == null || answers == null || questions.size() != answers.size()) return List.of();
        Map<String, int[]> sums = new LinkedHashMap<>();
        for (String d : DIMENSIONS) sums.put(d, new int[]{0, 0});
        for (int i = 0; i < questions.size(); i++) {
            AssessmentQuestion q = questions.get(i);
            String dim = q.getDimension();
            if (dim == null || !sums.containsKey(dim)) continue;
            int[] pair = sums.get(dim);
            try {
                pair[0] += scoreForQuestion(q, answers.get(i));
            } catch (Exception e) {
                return List.of();
            }
            pair[1] += maxScoreForQuestion(q);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, int[]> e : sums.entrySet()) {
            int got = e.getValue()[0];
            int max = e.getValue()[1];
            if (max == 0) continue;
            int percent = (int) Math.round(got * 100.0 / max);
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getKey());
            item.put("score", got);
            item.put("maxScore", max);
            item.put("percent", percent);
            item.put("comment", commentFor(e.getKey(), percent));
            list.add(item);
        }
        return list;
    }

    private String commentFor(String dimension, int percent) {
        String[] texts = DIMENSION_COMMENTS.get(dimension);
        if (texts == null) return "";
        if (percent < 40) return texts[0];
        if (percent < 70) return texts[1];
        return texts[2];
    }

    /** 从历史记录的 detail 里还原答案，让老报告也能画出雷达图 */
    public List<Map<String, Object>> dimensionBreakdown(String detailJson) {
        if (detailJson == null || detailJson.isBlank()) return List.of();
        try {
            List<Integer> answers = objectMapper.readValue(detailJson, new TypeReference<List<Integer>>() {});
            return dimensionBreakdown(listActiveQuestions(), answers);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 历史/最新记录转成结果页payload，和 submit 走同一套 describe，避免两边算法漂移 */
    public Map<String, Object> describeRecord(RiskAssessment ra) {
        int score = ra.getScore() == null ? 0 : ra.getScore();
        int maxScore = ra.getMaxScore() == null ? 0 : ra.getMaxScore();
        // 老记录没有 max_score，用等级名反查，避免历史数据把百分比算成 0
        LevelInfo info = maxScore > 0 ? levelFor(score, maxScore) : levelByName(ra.getLevel());

        Map<String, Object> result = new HashMap<>(describe(info, score, maxScore, ra.getExpiresAt(),
                dimensionBreakdown(ra.getDetail()), ra.getReportNo()));
        result.put("exists", true);
        result.put("createdAt", ra.getCreatedAt() == null ? "" : ra.getCreatedAt().toString());
        result.put("signedAt", ra.getSignedAt() == null ? "" : ra.getSignedAt().toString());
        result.put("validMonths", VALID_MONTHS);
        return result;
    }

    // 结果页要的字段都在这里拼，submit 和 latest 共用，避免两边算法漂移
    public Map<String, Object> describe(LevelInfo info, int total, int maxTotal, LocalDateTime expiresAt,
                                        List<Map<String, Object>> dimensions, String reportNo) {
        Map<String, Object> result = new HashMap<>();
        result.put("level", info.name);
        result.put("levelCode", info.code);
        result.put("levelIndex", info.index);
        result.put("score", total);
        result.put("maxScore", maxTotal);
        result.put("percent", maxTotal == 0 ? 0 : Math.round(total * 100.0 / maxTotal));
        result.put("summary", info.summary);
        result.put("suitableBiz", info.biz);
        result.put("maxProductLevel", "R" + info.index);
        result.put("productLevels", productLevels(info.index));
        result.put("recommendCategories", info.categories);
        result.put("dimensions", dimensions == null ? List.of() : dimensions);
        result.put("reportNo", reportNo == null ? "" : reportNo);
        result.put("expiresAt", expiresAt == null ? null : expiresAt.toString());
        result.put("expired", expiresAt != null && expiresAt.isBefore(LocalDateTime.now()));
        return result;
    }

    public IPage<RiskAssessment> getHistory(Long userId, int pageNum, int pageSize) {
        QueryWrapper<RiskAssessment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at");
        return assessmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public RiskAssessment getLatest(Long userId) {
        QueryWrapper<RiskAssessment> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at").last("limit 1");
        return assessmentMapper.selectOne(wrapper);
    }

    private List<AssessmentQuestion> listActiveQuestions() {
        QueryWrapper<AssessmentQuestion> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort_order");
        return questionMapper.selectList(wrapper);
    }

    private int scoreForQuestion(AssessmentQuestion question, int answerValue) {
        List<Map<String, Object>> options = parseOptions(question);
        for (Map<String, Object> opt : options) {
            Object value = opt.get("value");
            if (value != null && ((Number) value).intValue() == answerValue) {
                Object score = opt.get("score");
                return score == null ? 0 : ((Number) score).intValue();
            }
        }
        // 静默给个兜底分会让伪造的答案也能入库，这里直接拒绝
        throw new RuntimeException("第 " + question.getSortOrder() + " 题的答案不在选项范围内");
    }

    private int maxScoreForQuestion(AssessmentQuestion question) {
        int max = 0;
        for (Map<String, Object> opt : parseOptions(question)) {
            Object score = opt.get("score");
            if (score != null) max = Math.max(max, ((Number) score).intValue());
        }
        return max;
    }

    private List<Map<String, Object>> parseOptions(AssessmentQuestion question) {
        try {
            return objectMapper.readValue(question.getOptionsJson(),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("解析题目选项失败 id={}", question.getId());
            return List.of();
        }
    }

    /** 用得分率而不是绝对分判级：题库增删题目后阈值依然成立 */
    public LevelInfo levelFor(int total, int maxTotal) {
        double rate = maxTotal == 0 ? 0 : (double) total / maxTotal;
        if (rate < 0.30) return LEVELS.get(0);
        if (rate < 0.45) return LEVELS.get(1);
        if (rate < 0.62) return LEVELS.get(2);
        if (rate < 0.80) return LEVELS.get(3);
        return LEVELS.get(4);
    }

    public LevelInfo levelByName(String name) {
        return LEVELS.stream().filter(l -> l.name.equals(name)).findFirst().orElse(LEVELS.get(0));
    }

    /** C{n} 投资者可购买风险等级不高于 R{n} 的产品 */
    private List<Map<String, Object>> productLevels(int levelIndex) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", "R" + i);
            item.put("name", PRODUCT_NAMES[i - 1]);
            item.put("allowed", i <= levelIndex);
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildDefaultQuestions() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(question("您的年龄段是？", "风险态度", new Object[][]{
                {"60 岁以上", 1}, {"50-60 岁", 2}, {"40-50 岁", 3}, {"30-40 岁", 4}, {"30 岁以下", 5}
        }));
        list.add(question("您的最高学历是？", "知识与经验", new Object[][]{
                {"高中及以下", 1}, {"大专", 3}, {"本科", 4}, {"硕士及以上", 5}
        }));
        list.add(question("您的家庭主要收入来源是？", "财务实力", new Object[][]{
                {"退休金或社会保障", 1}, {"较不稳定的劳务收入", 2},
                {"工资、劳务报酬", 4}, {"投资收益或经营所得", 5}
        }));
        list.add(question("您的家庭年可支配收入约为？", "财务实力", new Object[][]{
                {"10 万以下", 1}, {"10-30 万", 2}, {"30-50 万", 3}, {"50-100 万", 4}, {"100 万以上", 5}
        }));
        list.add(question("除自住房产外，您可投资的金融资产规模约为？", "财务实力", new Object[][]{
                {"10 万以下", 1}, {"10-50 万", 2}, {"50-300 万", 3}, {"300-1000 万", 4}, {"1000 万以上", 5}
        }));
        list.add(question("您每年可用于投资的资金占家庭年收入的比例？", "财务实力", new Object[][]{
                {"10% 以下", 1}, {"10%-25%", 2}, {"25%-50%", 4}, {"50% 以上", 5}
        }));
        list.add(question("您的负债（含房贷、消费贷）占总资产的比例？", "财务实力", new Object[][]{
                {"70% 以上", 1}, {"50%-70%", 2}, {"30%-50%", 3}, {"10%-30%", 4}, {"10% 以下", 5}
        }));
        list.add(question("您的证券投资经验有多久？", "知识与经验", new Object[][]{
                {"没有经验", 1}, {"少于 1 年", 2}, {"1-3 年", 3}, {"3-5 年", 4}, {"5 年以上", 5}
        }));
        list.add(question("您对股票、基金、债券等金融产品的了解程度？", "知识与经验", new Object[][]{
                {"完全不了解", 1}, {"了解少量品种", 2},
                {"了解常见品种及其风险", 4}, {"具备专业背景或从业经历", 5}
        }));
        list.add(question("您计划的投资期限是？", "投资规划", new Object[][]{
                {"1 年以内", 1}, {"1-3 年", 2}, {"3-5 年", 4}, {"5 年以上", 5}
        }));
        list.add(question("若投资出现浮亏，您能承受的最大亏损幅度是？", "风险态度", new Object[][]{
                {"不能承受任何亏损", 1}, {"5% 以内", 2}, {"5%-15%", 3}, {"15%-30%", 4}, {"30% 以上", 5}
        }));
        list.add(question("您的投资目标更偏向于？", "投资规划", new Object[][]{
                {"保证本金安全，收益略高于存款", 1}, {"获取稳定收益，可接受小幅波动", 2},
                {"资产稳健增值，能承受一定波动", 3}, {"追求较高回报，愿意承担较大波动", 4},
                {"追求高收益，可承受大幅亏损", 5}
        }));
        return list;
    }

    private Map<String, Object> question(String text, String dimension, Object[][] optionsData) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < optionsData.length; i++) {
            Map<String, Object> opt = new HashMap<>();
            opt.put("value", i + 1);
            opt.put("label", optionsData[i][0]);
            opt.put("score", optionsData[i][1]);
            options.add(opt);
        }
        Map<String, Object> q = new HashMap<>();
        q.put("text", text);
        q.put("dimension", dimension);
        q.put("options", options);
        return q;
    }
}