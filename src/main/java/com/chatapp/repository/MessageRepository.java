
package com.chatapp.repository;

import com.chatapp.entity.Message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repository xử lý việc đọc và lưu tin nhắn.
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Lấy tin nhắn chưa bị xóa theo từng trang.
    // Tải kèm thông tin người gửi để tránh lỗi LazyInitializationException.
    // Thứ tự: thời gian tăng dần, nếu trùng thời gian thì theo ID tăng dần.
    @EntityGraph(attributePaths = {"sender"})
    Page<Message> findByConversation_IdAndDeletedAtIsNull(
            Long conversationId,
            Pageable pageable
    );

    // Lấy tin nhắn của một user.
    List<Message> findBySender_Id(Long senderId);
}
