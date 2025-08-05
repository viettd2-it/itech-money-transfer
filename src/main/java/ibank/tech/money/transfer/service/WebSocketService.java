package ibank.tech.money.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ibank.tech.money.transfer.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending WebSocket messages to connected clients
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Broadcast flag update to all connected clients
     */
    public void broadcastFlagUpdate(String namespace, String flagKey, String action, Boolean enabled) {
        try {
            WebSocketMessage message = WebSocketMessage.flagUpdate(namespace, flagKey, action, enabled);
            
            // Broadcast to all clients subscribed to general flag updates
            messagingTemplate.convertAndSend("/topic/flags", message);
            
            // Broadcast to clients subscribed to specific namespace
            messagingTemplate.convertAndSend("/topic/flags/" + namespace, message);
            
            log.info("Broadcasted flag update: namespace={}, flag={}, action={}, enabled={}", 
                    namespace, flagKey, action, enabled);
                    
        } catch (Exception e) {
            log.error("Failed to broadcast flag update", e);
        }
    }

    /**
     * Send message to a specific user
     */
    public void sendToUser(String username, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", message);
        } catch (Exception e) {
            log.error("Failed to send message to user: {}", username, e);
        }
    }

    /**
     * Send connection status to a specific user
     */
    public void sendConnectionStatus(String username, String status) {
        WebSocketMessage message = WebSocketMessage.connectionStatus(status);
        sendToUser(username, message);
    }

    /**
     * Send error message to a specific user
     */
    public void sendError(String username, String errorMessage) {
        WebSocketMessage message = WebSocketMessage.error(errorMessage);
        sendToUser(username, message);
    }

    /**
     * Broadcast system message to all connected clients
     */
    public void broadcastSystemMessage(String message) {
        try {
            WebSocketMessage wsMessage = WebSocketMessage.connectionStatus(message);
            messagingTemplate.convertAndSend("/topic/system", wsMessage);
        } catch (Exception e) {
            log.error("Failed to broadcast system message", e);
        }
    }
} 