package com.chatapp.controller;

import com.chatapp.dto.ConversationResponse;
import com.chatapp.dto.CreateDirectConversationRequest;
import com.chatapp.service.ConversationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    // Spring tự inject Service vào Controller.
    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    // Lấy danh sách cuộc trò chuyện của tài khoản đang đăng nhập.
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getMyConversations(
            Authentication authentication
    ) {
        // Username lấy từ danh tính đã được Spring Security xác thực.
        String currentUsername = authentication.getName();

        List<ConversationResponse> conversations =
                conversationService.getMyConversations(currentUsername);

        return ResponseEntity.ok(conversations);
    }

    // Mở hoặc lấy lại cuộc trò chuyện trực tiếp với một người.
    @PostMapping("/direct")
    public ResponseEntity<ConversationResponse> createDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();

        ConversationResponse response =
                conversationService.createDirectConversation(
                        currentUsername,
                        request.getParticipantUsername()
                );

        return ResponseEntity.ok(response);
    }
}