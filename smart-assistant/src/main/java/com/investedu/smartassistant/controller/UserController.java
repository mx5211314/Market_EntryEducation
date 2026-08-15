package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // 获取当前用户信息
    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestHeader("Authorization") String authHeader) {
        User user = getCurrentUser(authHeader);
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("gender", user.getGender());
        map.put("age", user.getAge());
        map.put("signature", user.getSignature());
        map.put("role", user.getRole());
        map.put("status", user.getStatus());
        // map.put("avatar", user.getAvatar());  // 暂时注释，数据库列待添加
        map.put("phone", user.getPhone());
        return map;
    }

    // 修改个人信息
    @PutMapping("/profile")
    public Map<String, String> updateProfile(@RequestHeader("Authorization") String authHeader,
                                             @RequestBody Map<String, String> body) {
        User user = getCurrentUser(authHeader);
        String nickname = body.get("nickname");
        String gender = body.get("gender");
        Integer age = body.containsKey("age") && !body.get("age").isEmpty() ? Integer.parseInt(body.get("age")) : null;
        String signature = body.get("signature");
        String phone = body.get("phone");   // 新增
        // String avatar = body.get("avatar");  // 暂时注释，数据库列待添加
        userService.updateProfile(user.getId(), nickname, gender, age, signature, phone, null);
        return Map.of("message", "修改成功");
    }
    // 修改密码
    @PutMapping("/password")
    public Map<String, String> updatePassword(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody Map<String, String> body) {
        User user = getCurrentUser(authHeader);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        userService.updatePassword(user.getId(), oldPassword, newPassword);
        return Map.of("message", "密码修改成功");
    }

    // ========== 管理员接口 ==========
    // 获取所有用户（管理员）
    @GetMapping("/list")
    public List<User> listUsers(@RequestHeader("Authorization") String authHeader) {
        checkAdmin(authHeader);
        return userService.listAllUsers();
    }

    // 修改用户角色（管理员）
    @PutMapping("/{userId}/role")
    public Map<String, String> updateRole(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable Long userId,
                                          @RequestBody Map<String, String> body) {
        checkAdmin(authHeader);
        String role = body.get("role");
        userService.updateUserRole(userId, role);
        return Map.of("message", "角色更新成功");
    }

    // 禁用/启用用户（管理员）
    @PutMapping("/{userId}/status")
    public Map<String, String> updateStatus(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable Long userId,
                                            @RequestBody Map<String, Integer> body) {
        checkAdmin(authHeader);
        Integer status = body.get("status");
        userService.updateUserStatus(userId, status);
        return Map.of("message", "状态更新成功");
    }

    // 删除用户（管理员）
    @DeleteMapping("/{userId}")
    public Map<String, String> deleteUser(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable Long userId) {
        checkAdmin(authHeader);
        userService.deleteUser(userId);
        return Map.of("message", "删除成功");
    }

    // ---------- 辅助方法 ----------
    private User getCurrentUser(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        return userService.findByUsername(username);
    }

    private void checkAdmin(String authHeader) {
        User user = getCurrentUser(authHeader);
        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("无权限访问");
        }
    }
}