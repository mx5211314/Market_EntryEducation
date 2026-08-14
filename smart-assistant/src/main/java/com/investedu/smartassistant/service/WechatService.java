package com.investedu.smartassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WechatService {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Value("${wechat.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成微信扫码登录授权 URL
     */
    public String getAuthUrl(String state) {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return "https://open.weixin.qq.com/connect/qrconnect?appid=" + appid
                + "&redirect_uri=" + encodedRedirect
                + "&response_type=code&scope=snsapi_login&state=" + state
                + "#wechat_redirect";
    }

    /**
     * 通过 code 获取 openid
     */
    public String getOpenid(String code) throws Exception {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appid
                + "&secret=" + secret + "&code=" + code + "&grant_type=authorization_code";

        String response = restTemplate.getForObject(url, String.class);
        JsonNode json = objectMapper.readTree(response);

        if (json.has("errcode")) {
            throw new RuntimeException("微信登录失败: " + json.get("errmsg").asText());
        }
        return json.get("openid").asText();
    }
}