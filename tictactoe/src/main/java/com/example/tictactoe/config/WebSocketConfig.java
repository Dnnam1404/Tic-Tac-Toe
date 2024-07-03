package com.example.tictactoe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Lớp cấu hình để thiết lập tính năng nhắn tin WebSocket trong ứng dụng.
 * Cho phép sử dụng STOMP (Giao thức nhắn tin hướng văn bản đơn giản) để gửi tin nhắn giữa máy khách và máy chủ
 */
@Configuration
@EnableWebSocketMessageBroker //Kích hoạt tính năng nhắn tin WebSocket với một message broker.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Đăng ký điểm cuối (endpoint) /ws để cho phép kết nối WebSocket.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    /**
     * Cấu hình message broker để xử lý các tin nhắn
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/queue", "/topic", "/user");
        registry.setUserDestinationPrefix("/user");
    }
}
