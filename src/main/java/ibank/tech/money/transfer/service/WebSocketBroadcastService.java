package ibank.tech.money.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ibank.tech.money.transfer.dto.FliptFlagUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Simple WebSocket broadcast service for Flipt updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = false)
public class WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Broadcast flag update to all connected WebSocket clients
     */
    public void broadcastFlagUpdate(FliptFlagUpdateEvent event) {
        try {
            String namespace = getNamespace(event);
            
            // Broadcast to all clients subscribed to /topic/flags
            messagingTemplate.convertAndSend("/topic/flags", event);
            
            // Broadcast to namespace-specific topic
            messagingTemplate.convertAndSend("/topic/flags/" + namespace, event);
            
            log.info("Broadcasted flag update to WebSocket clients: namespace={}, action={}", 
                    namespace, getAction(event));
                    
        } catch (Exception e) {
            log.error("Error broadcasting flag update via WebSocket", e);
        }
    }

    /**
     * Broadcast generic message to all clients
     */
    public void broadcastMessage(String topic, Object message) {
        try {
            messagingTemplate.convertAndSend("/topic/" + topic, message);
            log.debug("Broadcasted message to topic: {}", topic);
        } catch (Exception e) {
            log.error("Error broadcasting message to topic: {}", topic, e);
        }
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(String username, String destination, Object message) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, message);
            log.debug("Sent message to user: {} on destination: {}", username, destination);
        } catch (Exception e) {
            log.error("Error sending message to user: {}", username, e);
        }
    }

    private String getNamespace(FliptFlagUpdateEvent event) {
        if (event != null && event.getData() != null && event.getData().getNamespace() != null) {
            return event.getData().getNamespace();
        }
        return "default";
    }

    private String getAction(FliptFlagUpdateEvent event) {
        if (event != null && event.getData() != null && event.getData().getAction() != null) {
            return event.getData().getAction();
        }
        return "unknown";
    }
}
