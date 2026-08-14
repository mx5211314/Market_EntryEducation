package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
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
        response.put("role", user.getRole());
        return response;
    }

    @GetMapping("/me")
    public Map<String, String> currentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        return Map.of("username", user.getUsername(), "nickname", user.getNickname());
    }

}