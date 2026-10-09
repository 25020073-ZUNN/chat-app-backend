
package com.chatapp.security;

import com.chatapp.entity.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    // Spring tự tiêm JwtService và UserRepository vào Filter
    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    // Hàm được Spring gọi khi HTTP request đi qua Filter
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Lấy JWT từ header Authorization
        String authHeader = request.getHeader("Authorization");

        // Không có Bearer token thì để Spring Security xử lý tiếp
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bỏ tiền tố "Bearer " để lấy JWT
        String token = authHeader.substring(7);

        try {
            // Kiểm tra chữ ký, thời hạn và tính hợp lệ của JWT
            if (jwtService.isTokenValid(token)) {

                // Lấy username từ trường subject trong JWT
                Claims claims = jwtService.extractClaims(token);
                String username = claims.getSubject();

                // Chỉ xác thực nếu request chưa có Authentication
                if (username != null
                        && SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

                    // Kiểm tra user có còn tồn tại trong MySQL không
                    Optional<User> optionalUser =
                            userRepository.findByUsername(username);

                    if (optionalUser.isPresent()) {

                        // Tạo thông tin xác thực cho user tồn tại
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        Collections.emptyList()
                                );

                        // Gắn thông tin request hiện tại vào Authentication
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        // Lưu Authentication để Spring Security kiểm tra
                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                    // Nếu không tìm thấy user, không xác thực request.
                    // API cần đăng nhập sẽ bị từ chối bởi Spring Security.
                }
            }
        } catch (io.jsonwebtoken.JwtException
                 | IllegalArgumentException e) {

            // JWT sai định dạng, sai chữ ký hoặc hết hạn
            SecurityContextHolder.clearContext();
        }

        // Tiếp tục chuỗi Filter
        filterChain.doFilter(request, response);
    }
}