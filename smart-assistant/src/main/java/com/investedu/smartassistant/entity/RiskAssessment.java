package com.investedu.smartassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("risk_assessment")
public class RiskAssessment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 适当性报告编号，对外展示与留痕用 */
    private String reportNo;
    private String level;
    private Integer score;
    // 题库可增删，满分随之变化；存下来历史记录才能算出当时的得分率
    private Integer maxScore;
    private String detail;
    private LocalDateTime createdAt;
    /** 风险揭示书签署时间，留痕用 */
    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
}