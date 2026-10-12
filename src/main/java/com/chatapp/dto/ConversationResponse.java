package com.chatapp.dto;

import com.chatapp.entity.ConversationType;

import java.time.LocalDateTime;

// DTO chứa dữ liệu cuộc trò chuyện trả về cho client.
public record ConversationResponse(
        Long conversationId,
        ConversationType conversationType,
        String name,
        String participantUsername,
        String lastMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
