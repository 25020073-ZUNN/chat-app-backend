package com.chatapp.controller;

import com.chatapp.dto.MessageResponse;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.entity.Message;
import com.chatapp.service.MessageService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    // Spring tự động inject MessageService.
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // API gửi tin nhắn.
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        // Lấy username từ JWT đã được Spring Security xác thực.
        String currentUsername = authentication.getName();

        // Gọi Service xử lý nghiệp vụ gửi tin nhắn.
        Message message = messageService.sendMessage(
                currentUsername,
                request
        );

        // Chuyển Entity thành DTO để trả về client.
        MessageResponse response = new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getUsername(),
                message.getContent(),
                message.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }

    // API lấy lịch sử tin nhắn theo từng trang.
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<MessageResponse>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        // Giới hạn đầu vào để tránh yêu cầu quá nhiều dữ liệu.
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến 100"
            );
        }

        // Lấy username từ JWT.
        String currentUsername = authentication.getName();

        // Lấy tin nhắn mới nhất trước.
        // Nếu cùng thời gian, sắp xếp theo ID giảm dần.
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        // Gọi Service lấy trang tin nhắn sau khi kiểm tra quyền.
        Page<Message> messages = messageService.getConversationMessages(
                currentUsername,
                conversationId,
                pageable
        );

        // Chuyển các Entity trong trang thành DTO.
        Page<MessageResponse> response = messages.map(message ->
                new MessageResponse(
                        message.getId(),
                        message.getConversation().getId(),
                        message.getSender().getUsername(),
                        message.getContent(),
                        message.getCreatedAt()
                )
        );

        return ResponseEntity.ok(response);
    }
}
