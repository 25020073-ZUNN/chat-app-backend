package com.chatapp.service;

import com.chatapp.dto.SendMessageRequest;
import com.chatapp.entity.Conversation;
import com.chatapp.entity.Message;
import com.chatapp.entity.User;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    // Spring tự động cung cấp các Repository cần thiết.
    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    // Lấy lịch sử tin nhắn theo từng trang.
    @Transactional(readOnly = true)
    public Page<Message> getConversationMessages(
            String currentUsername,
            Long conversationId,
            Pageable pageable
    ) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy người dùng"
                ));

        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy cuộc trò chuyện"
                ));

        // Chỉ thành viên của cuộc trò chuyện mới được xem lịch sử.
        boolean isMember =
                memberRepository.existsByConversation_IdAndUser_Id(
                        conversationId,
                        currentUser.getId()
                );

        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không phải thành viên của cuộc trò chuyện này"
            );
        }

        return messageRepository.findByConversation_IdAndDeletedAtIsNull(
                conversationId,
                pageable
        );
    }

    // Xử lý nghiệp vụ gửi tin nhắn.
    @Transactional
    public Message sendMessage(
            String currentUsername,
            SendMessageRequest request
    ) {
        // Bước 1: Tìm người gửi dựa trên tài khoản đã xác thực.
        User sender = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy người gửi"
                ));

        // Bước 2: Tìm cuộc trò chuyện cần gửi tin nhắn.
        Conversation conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy cuộc trò chuyện"
                ));

        // Bước 3: Kiểm tra người gửi có phải thành viên không.
        boolean isMember =
                memberRepository.existsByConversation_IdAndUser_Id(
                        conversation.getId(),
                        sender.getId()
                );

        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không phải thành viên của cuộc trò chuyện này"
            );
        }

        // Bước 4: Tạo tin nhắn mới.
        Message message = new Message(
                conversation,
                sender,
                request.getContent().trim()
        );

        // Bước 5: Lưu tin nhắn vào database.
        Message savedMessage = messageRepository.save(message);

        // Bước 6: Cập nhật thời gian hoạt động của cuộc trò chuyện.
        conversation.setUpdatedAt(LocalDateTime.now());

        // Bước 7: Trả về tin nhắn đã lưu.
        return savedMessage;
    }
}
