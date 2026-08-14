package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.service.WechatService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/wechat")
public class WechatController {

    private final WechatService wechatService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public WechatController(WechatService wechatService, UserService userService, JwtUtil jwtUtil) {
        this.wechatService = wechatService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 前端请求该接口获取授权 URL，然后跳转
     */
    @GetMapping("/auth-url")
    public String getAuthUrl() {
        String state = UUID.randomUUID().toString().replace("-", "");
        return wechatService.getAuthUrl(state);
    }

    /**
     * 微信回调地址，用户扫码后自动跳转到这里
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code) {
        try {
            String openid = wechatService.getOpenid(code);
            User user = userService.findOrCreateByOpenid(openid);
            String token = jwtUtil.generateToken(user.getUsername());

            // 重定向到前端登录页面，携带 token 和用户信息
            String frontendUrl = "http://localhost:5173/login?token=" + token
                    + "&username=" + user.getUsername()
                    + "&nickname=" + (user.getNickname() == null ? "" : user.getNickname())
                    + "&role=" + user.getRole();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl))
                    .build();
        } catch (Exception e) {
            // 重定向到前端登录页并带错误信息
            String errorUrl = "http://localhost:5173/login?error=微信登录失败";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
    }
}