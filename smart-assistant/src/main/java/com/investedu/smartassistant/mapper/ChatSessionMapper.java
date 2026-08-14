package com.investedu.smartassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investedu.smartassistant.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<ChatSession> listByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} AND session_id = #{sessionId}")
    ChatSession findByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}