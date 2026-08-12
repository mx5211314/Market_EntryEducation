package com.investedu.smartassistant.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryRewriter {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    public String rewrite(String userMessage) {
        String prompt = "你是一个金融查询改写助手。\n"
                + "如果用户输入属于问候、自我介绍、闲聊或非金融问题，请直接回复 '__CHAT__'。\n"
                + "否则，将用户口语化描述改写成一个标准金融规则检索短句。只输出改写后的短句或 '__CHAT__'，不要输出其他内容。\n"
                + "用户：" + userMessage + "\n改写：";
        return chatLanguageModel.generate(prompt).trim();
    }

    public static boolean isChat(String rewritten) {
        return "__CHAT__".equals(rewritten);
    }
}