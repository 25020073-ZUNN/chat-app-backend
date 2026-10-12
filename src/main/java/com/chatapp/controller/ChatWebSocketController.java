package com.chatapp.controller;

import com.chatapp.dto.SendMessageRequest;
import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    // Spring tự động cung cấp service và công cụ phát message.
    public ChatWebSocketController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    // Nhận message từ client qua /app/chat.send.
    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload ChatMessage request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // Danh tính phải đến từ phiên WebSocket đã xác thực JWT.
        Principal principal = headerAccessor.getUser();

        if (principal == null) {
            throw new IllegalStateException(
                    "WebSocket chưa được xác thực"
            );
        }

        // Kiểm tra dữ liệu đầu vào.
        if (request.getConversationId() == null
                || request.getContent() == null
                || request.getContent().isBlank()) {
            throw new IllegalArgumentException(
                    "Conversation ID và nội dung tin nhắn là bắt buộc"
            );
        }

        // Tạo request dùng chung với REST API hiện có.
        SendMessageRequest sendRequest = new SendMessageRequest();
        sendRequest.setConversationId(request.getConversationId());
        sendRequest.setContent(request.getContent());

        // Service kiểm tra quyền thành viên và lưu vào database.
        Message savedMessage = messageService.sendMessage(
                principal.getName(),
                sendRequest
        );

        // Chỉ phát vào phòng chat tương ứng.
        String destination = "/topic/conversations/"
                + savedMessage.getConversation().getId();

        // Phát dữ liệu lấy từ bản ghi đã lưu thành công.
        ChatMessage response = new ChatMessage(
                savedMessage.getId(),
                savedMessage.getConversation().getId(),
                savedMessage.getSender().getUsername(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt()
        );

        messagingTemplate.convertAndSend(destination, response);
    }

    // DTO dùng để nhận dữ liệu gửi lên và phát dữ liệu xuống.
    public static class ChatMessage {

        private Long id;
        private Long conversationId;
        private String sender;
        private String content;
        private LocalDateTime createdAt;

        public ChatMessage() {
        }

        public ChatMessage(
                Long id,
                Long conversationId,
                String sender,
                String content,
                LocalDateTime createdAt
        ) {
            this.id = id;
            this.conversationId = conversationId;
            this.sender = sender;
            this.content = content;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getConversationId() {
            return conversationId;
        }

        public void setConversationId(Long conversationId) {
            this.conversationId = conversationId;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
