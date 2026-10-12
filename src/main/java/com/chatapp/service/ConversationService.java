package com.chatapp.service;

import com.chatapp.dto.ConversationResponse;
import com.chatapp.entity.Conversation;
import com.chatapp.entity.ConversationMember;
import com.chatapp.entity.ConversationType;
import com.chatapp.entity.MemberRole;
import com.chatapp.entity.User;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.ConversationRepository;
import com.chatapp.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    // Spring tự truyền các Repository vào Service.
    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    // Lấy danh sách cuộc trò chuyện của người đang đăng nhập.
    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(String username) {

        // Tìm các cuộc trò chuyện mà người dùng tham gia.
        List<ConversationMember> members =
                memberRepository.findByUser_Username(username);

        // Chuyển dữ liệu entity thành DTO để trả về client.
        return members.stream()
                .map(member -> toResponse(member.getConversation()))
                .toList();
    }

    // Mở hoặc lấy lại cuộc trò chuyện trực tiếp với một người.
    @Transactional
    public ConversationResponse createDirectConversation(
            String currentUsername,
            String participantUsername
    ) {
        // 1. Tìm người đang đăng nhập.
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        // 2. Tìm người mà client muốn trò chuyện cùng.
        User participant = userRepository.findByUsername(participantUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Participant not found"
                ));

        // 3. Không cho phép trò chuyện với chính mình.
        if (currentUser.getId().equals(participant.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot create a direct conversation with yourself"
            );
        }

        // 4. Nếu đã có cuộc trò chuyện 1-1 thì sử dụng lại.
        var existingConversation =
                conversationRepository.findDirectBetweenUsers(
                        ConversationType.DIRECT,
                        currentUser.getId(),
                        participant.getId()
                );

        if (existingConversation.isPresent()) {
            return toResponse(existingConversation.get());
        }

        // 5. Tạo cuộc trò chuyện mới.
        Conversation conversation = new Conversation();
        conversation.setConversationType(ConversationType.DIRECT);

        // Gán thời gian để tránh giá trị NULL.
        LocalDateTime now = LocalDateTime.now();
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        // Lưu trước để lấy ID cuộc trò chuyện.
        conversation = conversationRepository.save(conversation);

        // 6. Thêm người đang đăng nhập vào cuộc trò chuyện.
        ConversationMember currentMember = new ConversationMember(
                conversation,
                currentUser,
                MemberRole.MEMBER
        );

        // Thêm người còn lại.
        ConversationMember participantMember = new ConversationMember(
                conversation,
                participant,
                MemberRole.MEMBER
        );

        memberRepository.save(currentMember);
        memberRepository.save(participantMember);

        // 7. Trả về DTO.
        return toResponse(conversation);
    }
    // Chuyển entity Conversation thành DTO phản hồi.
    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getConversationType(),
                conversation.getName(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}