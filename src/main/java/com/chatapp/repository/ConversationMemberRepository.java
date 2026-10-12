package com.chatapp.repository;

import com.chatapp.entity.ConversationMember;
import com.chatapp.entity.ConversationMemberId;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Quản lý các thành viên tham gia từng cuộc trò chuyện.
@Repository
public interface ConversationMemberRepository
        extends JpaRepository<ConversationMember, ConversationMemberId> {

    // Lấy danh sách thành viên của một cuộc trò chuyện.
    List<ConversationMember> findByConversation_Id(Long conversationId);

    // Kiểm tra một user có tham gia cuộc trò chuyện hay không.
    boolean existsByConversation_IdAndUser_Id(
            Long conversationId,
            Long userId
    );

    // Lấy danh sách cuộc trò chuyện theo ID người dùng.
    List<ConversationMember> findByUser_Id(Long userId);

    // Lấy danh sách cuộc trò chuyện theo username.
    // Đồng thời tải thông tin conversation để tránh lỗi lazy loading.
    @EntityGraph(attributePaths = {"conversation"})
    List<ConversationMember> findByUser_Username(String username);
}