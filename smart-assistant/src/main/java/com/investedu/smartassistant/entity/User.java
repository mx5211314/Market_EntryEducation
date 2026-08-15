package com.investedu.smartassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String role;
    private String gender;
    private Integer age;
    private String signature;
    private Integer status;
    // private String avatar;  // 暂时注释，数据库列待添加
    private String phone;
    private String openid;
    private Long githubId;
    private LocalDateTime createdAt;
}