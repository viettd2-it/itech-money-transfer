package ibank.tech.money.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ibank.tech.money.transfer.dto.FliptGenericUpdateEvent;
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
     * Broadcast Flipt update to all connected WebSocket clients
     */
    public void broadcastUpdate(FliptGenericUpdateEvent event) {
        try {
            log.info("=== WEBSOCKET BROADCAST SERVICE CALLED ===");
            log.info("Event type: {}, namespace: {}, action: {}", event.getType(), event.getNamespace(), event.getAction());

            String namespace = event.getNamespace();
            String entityType = getEntityTypeFromEvent(event);

            log.info("Determined entity type: {}", entityType);
            log.info("Broadcasting to topics: /topic/{} and /topic/{}/{}", entityType, entityType, namespace);

            // Broadcast to all clients subscribed to /topic/{entityType}
            messagingTemplate.convertAndSend("/topic/" + entityType, event);
            log.info("Sent to /topic/{}", entityType);

            // Broadcast to namespace-specific topic
            messagingTemplate.convertAndSend("/topic/" + entityType + "/" + namespace, event);
            log.info("Sent to /topic/{}/{}", entityType, namespace);

            log.info("=== WEBSOCKET BROADCAST COMPLETED ===");
            log.info("Broadcasted {} update to WebSocket clients: namespace={}, action={}",
                    entityType, namespace, event.getAction());

        } catch (Exception e) {
            log.error("Error broadcasting update via WebSocket", e);
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

    /**
     * Extract entity type from event type field
     */
    private String getEntityTypeFromEvent(FliptGenericUpdateEvent event) {
        String type = event.getType();
        if (type == null) return "unknown";
        
        if (type.startsWith("flag.")) return "flags";
        if (type.startsWith("segment.")) return "segments";
        if (type.startsWith("constraint.")) return "constraints";
        
        return "unknown";
    }
}
