package com.chatapp.controller;

import com.chatapp.dto.EditMessageRequest;
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

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Gửi một tin nhắn mới.
    // POST /api/messages
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();

        Message message = messageService.sendMessage(
                currentUsername,
                request
        );

        MessageResponse response = toResponse(message);

        return ResponseEntity.ok(response);
    }

    // Lấy lịch sử tin nhắn của một cuộc trò chuyện theo từng trang.
    // GET /api/messages/conversation/{conversationId}?page=0&size=20
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<MessageResponse>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        // Kiểm tra tham số phân trang hợp lệ.
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến 100"
            );
        }

        String currentUsername = authentication.getName();

        // Lấy tin nhắn mới nhất trước.
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<Message> messages =
                messageService.getConversationMessages(
                        currentUsername,
                        conversationId,
                        pageable
                );

        // Chuyển từng Message entity thành dữ liệu trả về cho client.
        Page<MessageResponse> response = messages.map(this::toResponse);

        return ResponseEntity.ok(response);
    }

    // Sửa nội dung tin nhắn của chính người gửi.
    // PUT /api/messages/{messageId}
    // Body: {"content": "Nội dung mới"}
    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody EditMessageRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();

        Message message = messageService.editMessage(
                currentUsername,
                messageId,
                request.getContent()
        );

        return ResponseEntity.ok(toResponse(message));
    }

    // Xóa tin nhắn của chính người gửi.
    // DELETE /api/messages/{messageId}
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();

        messageService.deleteMessage(
                currentUsername,
                messageId
        );

        // HTTP 204: thao tác thành công, không cần trả nội dung.
        return ResponseEntity.noContent().build();
    }

    // Gom logic chuyển entity thành response để tránh viết lặp.
    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getUsername(),
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt()
        );
    }
}