 package com.chatapp.dto;

import java.time.LocalDateTime;

// Dữ liệu trả về client sau khi gửi tin nhắn.
public class MessageResponse {

    private Long id;
    private Long conversationId;
    private String senderUsername;
    private String content;
    private LocalDateTime createdAt;

    public MessageResponse(
            Long id,
            Long conversationId,
            String senderUsername,
            String content,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
