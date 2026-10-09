
package com.chatapp.config;

import com.chatapp.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Spring tự tiêm JWT Filter vào cấu hình bảo mật
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // API sử dụng JWT nên tắt CSRF cho cấu hình REST này
                .csrf(csrf -> csrf.disable())

                // Không lưu trạng thái đăng nhập bằng HTTP Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Cấu hình phản hồi khi người dùng chưa xác thực
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Vui lòng đăng nhập hoặc cung cấp JWT hợp lệ\"}"
                            );
                        })
                )

                // Quy định API công khai và API cần đăng nhập
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Tắt các cơ chế đăng nhập mặc định không dùng trong API JWT
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Kiểm tra JWT trước khi Spring Security quyết định quyền truy cập
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}