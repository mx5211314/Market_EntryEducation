package com.investedu.smartassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investedu.smartassistant.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ChatMessage> listBySessionId(@Param("sessionId") String sessionId);
}