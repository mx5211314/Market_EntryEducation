package com.investedu.smartassistant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth/github")
public class GithubOAuthController {

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GithubOAuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // 获取授权链接
    @GetMapping("/login-url")
    public String loginUrl() {
        return "https://github.com/login/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=read:user";
    }

    // 回调处理
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code) {
        try {
            // 1. 用 code 换 access_token
            String tokenUrl = "https://github.com/login/oauth/access_token";
            Map<String, String> tokenRequest = new HashMap<>();
            tokenRequest.put("client_id", clientId);
            tokenRequest.put("client_secret", clientSecret);
            tokenRequest.put("code", code);
            tokenRequest.put("redirect_uri", redirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(tokenRequest, headers);
            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, entity, String.class);

            // 解析 access_token（返回可能是URL编码格式）
            String accessToken = extractAccessToken(tokenResponse.getBody());

            // 2. 调用 GitHub API 获取用户信息
            String userApiUrl = "https://api.github.com/user";
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);
            ResponseEntity<String> userResponse = restTemplate.exchange(userApiUrl, HttpMethod.GET, userEntity, String.class);
            JsonNode userJson = objectMapper.readTree(userResponse.getBody());
            Long githubId = userJson.get("id").asLong();
            String username = userJson.get("login").asText();

            // 3. 查找或创建用户
            User user = userService.findOrCreateByGithubId(githubId, username);

            // 4. 生成 JWT
            String jwt = jwtUtil.generateToken(user.getUsername());

            // 5. 重定向到前端
            String frontendUrl = "http://localhost:5173/auth/login?token=" + jwt
                    + "&username=" + user.getUsername()
                    + "&nickname=" + (user.getNickname() == null ? "" : user.getNickname())
                    + "&role=" + user.getRole();
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(frontendUrl)).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:5173/auth/login?error=github_login_failed"))
                    .build();
        }
    }

    private String extractAccessToken(String body) {
        // GitHub 返回格式可能是 access_token=xxx&scope=xxx&token_type=bearer
        String[] parts = body.split("&");
        for (String part : parts) {
            if (part.startsWith("access_token=")) {
                return part.substring("access_token=".length());
            }
        }
        return "";
    }
}