package com.investedu.smartassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    /** 只进不出：用户列表等接口直接返回 User，序列化出去等于把每个人的密码哈希发给浏览器 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String nickname;
    private String role;
    private String gender;
    private Integer age;
    private String signature;
    private Integer status;
    /** 头像地址，/api/upload 返回的 URL */
    private String avatar;
    private String phone;
    private String openid;
    private Long githubId;
    private LocalDateTime createdAt;
}