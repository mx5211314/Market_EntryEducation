package com.investedu.smartassistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // 手机号正则（简单校验）
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 注册：用户名 + 密码 + 手机号（可选）
     */
    public User register(String username, String password, String nickname, String phone) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new RuntimeException("密码至少6位");
        }
        if (phone != null && !phone.isEmpty()) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new RuntimeException("手机号格式不正确");
            }
            if (userMapper.countByPhone(phone) > 0) {
                throw new RuntimeException("手机号已被注册");
            }
        }

        if (userMapper.countByUsername(username) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname == null || nickname.isEmpty() ? username : nickname);
        user.setRole("USER");
        user.setStatus(1);
        user.setPhone(phone);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /**
     * 登录：支持用户名或手机号
     */
    public User login(String account, String password) {
        User user = null;
        if (account != null && account.matches("^1[3-9]\\d{9}$")) {
            user = userMapper.findByPhone(account);
        } else {
            user = userMapper.findByUsername(account);
        }

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名/手机号或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }
        return user;
    }

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 修改个人信息（包含手机号）
     */
    public void updateProfile(Long id, String nickname, String gender, Integer age, String signature, String phone, String avatar) {
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");

        if (nickname != null && !nickname.isEmpty()) user.setNickname(nickname);
        if (gender != null) user.setGender(gender);
        if (age != null) user.setAge(age);
        if (signature != null) user.setSignature(signature);
        // if (avatar != null) user.setAvatar(avatar);  // 暂时注释，数据库列待添加
        if (phone != null) {
            if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                throw new RuntimeException("手机号格式不正确");
            }
            if (!phone.equals(user.getPhone()) && userMapper.countByPhone(phone) > 0) {
                throw new RuntimeException("手机号已被使用");
            }
            user.setPhone(phone);
        }

        userMapper.updateById(user);
    }

    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码至少6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    // ========== 管理员方法 ==========
    /** 管理端用户列表：关键词命中用户名/昵称/手机号，角色和状态可选筛选 */
    public IPage<User> pageUsers(int pageNum, int pageSize, String keyword, String role, Integer status) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("username", kw).or().like("nickname", kw).or().like("phone", kw));
        }
        if (StringUtils.hasText(role)) wrapper.eq("role", role);
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("created_at");
        return userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public void updateUserRole(Long userId, String role) {
        if (!"ADMIN".equals(role) && !"USER".equals(role)) throw new RuntimeException("角色不合法");
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        // 平台必须留人能进后台，降掉最后一个管理员就没人能改回来了
        if ("ADMIN".equals(user.getRole()) && "USER".equals(role) && countAdmins() <= 1) {
            throw new RuntimeException("至少要保留一个管理员");
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) throw new RuntimeException("状态不合法");
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if ("ADMIN".equals(user.getRole()) && countAdmins() <= 1) {
            throw new RuntimeException("至少要保留一个管理员");
        }
        userMapper.deleteById(userId);
    }

    private long countAdmins() {
        return userMapper.selectCount(new QueryWrapper<User>().eq("role", "ADMIN"));
    }
//    wechat
    public User findOrCreateByOpenid(String openid) {
        User user = userMapper.findByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setUsername("wx_" + openid.substring(0, Math.min(8, openid.length()))); // 避免重复，可加随机数
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setNickname("微信用户");
            user.setRole("USER");
            user.setStatus(1);
            user.setOpenid(openid);
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }
        return user;
    }
    //github 接入
    public User findOrCreateByGithubId(Long githubId, String username) {
        User user = userMapper.findByGithubId(githubId);
        if (user == null) {
            user = new User();
            user.setUsername("gh_" + username);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setNickname(username);
            user.setRole("USER");
            user.setStatus(1);
            user.setGithubId(githubId);
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }
        return user;
    }
}