package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.AuthContext;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthContext authContext;

    public AuthController(UserService userService, JwtUtil jwtUtil, AuthContext authContext) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authContext = authContext;
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String nickname = request.getOrDefault("nickname", "");
        String phone = request.getOrDefault("phone", null);   // 新增手机号
        userService.register(username, password, nickname, phone);
        return Map.of("message", "注册成功");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // 直接验证用户名密码
        User user = userService.login(username, password);

        String token = jwtUtil.generateToken(user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("nickname", user.getNickname());
        response.put("avatar", user.getAvatar());
        response.put("role", user.getRole());
        return response;
    }

    @GetMapping("/me")
    public Map<String, String> currentUser() {
        User user = authContext.requireUser();
        Map<String, String> res = new HashMap<>();
        res.put("username", user.getUsername());
        // Map.of 不接受 null value：GitHub 登录进来的账号 nickname 是空的，之前这里直接 NPE
        res.put("nickname", user.getNickname() == null ? user.getUsername() : user.getNickname());
        res.put("avatar", user.getAvatar());
        res.put("role", user.getRole());
        return res;
    }

}