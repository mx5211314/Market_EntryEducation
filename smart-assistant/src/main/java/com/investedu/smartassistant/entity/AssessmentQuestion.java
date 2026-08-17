package com.investedu.smartassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("assessment_question")
public class AssessmentQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String text;
    private String optionsJson;
    /** 所属测评维度，用于结果页的四维能力分析 */
    private String dimension;
    private Integer sortOrder;
    private Integer status;
}