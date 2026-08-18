package com.investedu.smartassistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.AuthContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final AuthContext authContext;

    public UserController(UserService userService, AuthContext authContext) {
        this.userService = userService;
        this.authContext = authContext;
    }

    // 获取当前用户信息
    @GetMapping("/user/profile")
    public Map<String, Object> getProfile() {
        User user = authContext.requireUser();
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("gender", user.getGender());
        map.put("age", user.getAge());
        map.put("signature", user.getSignature());
        map.put("role", user.getRole());
        map.put("status", user.getStatus());
        map.put("avatar", user.getAvatar());
        map.put("phone", user.getPhone());
        return map;
    }

    // 修改个人信息
    @PutMapping("/user/profile")
    public Map<String, String> updateProfile(@RequestBody Map<String, Object> body) {
        User user = authContext.requireUser();
        userService.updateProfile(user.getId(),
                str(body.get("nickname")),
                str(body.get("gender")),
                toInt(body.get("age")),
                str(body.get("signature")),
                str(body.get("phone")),
                str(body.get("avatar")));
        return Map.of("message", "修改成功");
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 年龄没填时前端会传 age: null。
     * 之前这里是 body.get("age").isEmpty()，null 直接 NPE，整个保存都失败——
     * 头像、昵称一起丢，界面上还看不到原因。
     */
    private static Integer toInt(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            throw new RuntimeException("年龄格式不正确");
        }
    }
    // 修改密码
    @PutMapping("/user/password")
    public Map<String, String> updatePassword(@RequestBody Map<String, String> body) {
        User user = authContext.requireUser();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        userService.updatePassword(user.getId(), oldPassword, newPassword);
        return Map.of("message", "密码修改成功");
    }

    // ========== 管理员接口 ==========
    // 路径挂在 /api/admin 下，SecurityConfig 的 hasRole("ADMIN") 才真正拦得住，
    // 不再只靠方法里手写的 checkAdmin
    @GetMapping("/admin/user/list")
    public IPage<User> pageUsers(@RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String role,
                                 @RequestParam(required = false) Integer status) {
        authContext.requireAdmin();
        return userService.pageUsers(pageNum, Math.min(pageSize, 100), keyword, role, status);
    }

    // 修改用户角色（管理员）
    @PutMapping("/admin/user/{userId}/role")
    public Map<String, String> updateRole(@PathVariable Long userId,
                                          @RequestBody Map<String, String> body) {
        requireOther(userId, "不能修改自己的角色");
        userService.updateUserRole(userId, body.get("role"));
        return Map.of("message", "角色更新成功");
    }

    // 禁用/启用用户（管理员）
    @PutMapping("/admin/user/{userId}/status")
    public Map<String, String> updateStatus(@PathVariable Long userId,
                                            @RequestBody Map<String, Integer> body) {
        requireOther(userId, "不能禁用自己的账号");
        userService.updateUserStatus(userId, body.get("status"));
        return Map.of("message", "状态更新成功");
    }

    // 删除用户（管理员）
    @DeleteMapping("/admin/user/{userId}")
    public Map<String, String> deleteUser(@PathVariable Long userId) {
        requireOther(userId, "不能删除自己的账号");
        userService.deleteUser(userId);
        return Map.of("message", "删除成功");
    }

    /** 管理员操作自己的账号会把自己锁在门外，一律在服务端挡掉，不能只靠前端隐藏按钮 */
    private void requireOther(Long userId, String message) {
        User current = authContext.requireAdmin();
        if (current.getId().equals(userId)) {
            throw new RuntimeException(message);
        }
    }
}