
package com.chatapp.config;

import com.chatapp.entity.User;
import com.chatapp.repository.ConversationMemberRepository;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.JwtService;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ConversationMemberRepository memberRepository;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            ConversationMemberRepository memberRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // Bước 1: Xác thực JWT khi client kết nối.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null
                    || !authorization.startsWith("Bearer ")) {
                throw new MessagingException("Thiếu JWT trong STOMP CONNECT");
            }

            String token = authorization.substring(7);

            if (!jwtService.isTokenValid(token)) {
                throw new MessagingException("JWT không hợp lệ hoặc đã hết hạn");
            }

            String username = jwtService.extractUsername(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username, null, Collections.emptyList());

            accessor.setUser(authentication);
        }

        // Bước 2: Kiểm tra quyền khi client đăng ký nhận tin.
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            Principal principal = accessor.getUser();

            if (principal == null) {
                throw new MessagingException("WebSocket chưa được xác thực");
            }

            String destination = accessor.getDestination();

            // Chỉ cho phép subscribe vào topic conversation hợp lệ.
            String prefix = "/topic/conversations/";

            if (destination == null || !destination.startsWith(prefix)) {
                throw new MessagingException("Destination không được phép");
            }

            String conversationIdText =
                    destination.substring(prefix.length());

            // Không cho phép ID rỗng hoặc đường dẫn có thêm dấu /.
            if (conversationIdText.isBlank()
                    || conversationIdText.contains("/")) {
                throw new MessagingException("Conversation ID không hợp lệ");
            }

            final Long conversationId;

            try {
                conversationId = Long.parseLong(conversationIdText);
            } catch (NumberFormatException exception) {
                throw new MessagingException("Conversation ID không hợp lệ");
            }

            // Lấy danh tính từ JWT, không tin username do client tự gửi.
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() ->
                            new MessagingException("Không tìm thấy người dùng"));

            // Kiểm tra người dùng có thuộc conversation này không.
            boolean isMember =
                    memberRepository.existsByConversation_IdAndUser_Id(
                            conversationId, user.getId());

            if (!isMember) {
                throw new MessagingException(
                        "Bạn không phải thành viên của phòng chat này");
            }
        }

        return message;
    }
}
