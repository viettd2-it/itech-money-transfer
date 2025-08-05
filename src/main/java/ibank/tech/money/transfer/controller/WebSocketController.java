package ibank.tech.money.transfer.controller;

import ibank.tech.money.transfer.dto.WebSocketMessage;
import ibank.tech.money.transfer.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket controller for handling real-time flag updates
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * Handle client subscription to flag updates
     */
    @MessageMapping("/subscribe")
    @SendToUser("/queue/subscription")
    public WebSocketMessage handleSubscription(@Payload Map<String, Object> payload, 
                                           SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "anonymous";
            
            String namespace = (String) payload.get("namespace");
            
            log.info("Client {} subscribed to namespace: {}", username, namespace);
            
            return WebSocketMessage.connectionStatus(
                "Successfully subscribed to flag updates" + 
                (namespace != null ? " for namespace: " + namespace : "")
            );
            
        } catch (Exception e) {
            log.error("Error handling subscription", e);
            return WebSocketMessage.error("Failed to subscribe: " + e.getMessage());
        }
    }

    /**
     * Handle client unsubscription
     */
    @MessageMapping("/unsubscribe")
    @SendToUser("/queue/subscription")
    public WebSocketMessage handleUnsubscription(@Payload Map<String, Object> payload,
                                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "anonymous";
            
            String namespace = (String) payload.get("namespace");
            
            log.info("Client {} unsubscribed from namespace: {}", username, namespace);
            
            return WebSocketMessage.connectionStatus("Successfully unsubscribed");
            
        } catch (Exception e) {
            log.error("Error handling unsubscription", e);
            return WebSocketMessage.error("Failed to unsubscribe: " + e.getMessage());
        }
    }

    /**
     * Handle client ping to keep connection alive
     */
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public WebSocketMessage handlePing(SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? 
            headerAccessor.getUser().getName() : "anonymous";
        
        log.debug("Received ping from client: {}", username);
        return WebSocketMessage.connectionStatus("pong");
    }
} 