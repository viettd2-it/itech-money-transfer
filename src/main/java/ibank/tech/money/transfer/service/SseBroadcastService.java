package ibank.tech.money.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ibank.tech.money.transfer.controller.SseController;
import ibank.tech.money.transfer.dto.FliptGenericUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting Flipt updates via Server-Sent Events
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "sse.enabled", havingValue = "true", matchIfMissing = true)
public class SseBroadcastService {

    private final SseController sseController;
    private final ObjectMapper objectMapper;

    /**
     * Broadcast Flipt update to all SSE clients
     */
    public void broadcastUpdate(FliptGenericUpdateEvent event) {
        try {
            log.info("=== SSE BROADCAST SERVICE CALLED ===");
            log.info("Event type: {}, namespace: {}, action: {}", event.getType(), event.getNamespace(), event.getAction());
            
            String namespace = event.getNamespace();
            String entityType = getEntityTypeFromEvent(event);
            String eventName = entityType + ".update";
            
            // Convert event to JSON string
            String jsonData = objectMapper.writeValueAsString(event);
            
            log.info("Broadcasting SSE event: {} to all clients", eventName);
            log.info("Event data: {}", jsonData);
            
            // Broadcast to all clients
            sseController.broadcastToAll(eventName, jsonData);
            
            // Broadcast to namespace-specific clients
            if (namespace != null && !namespace.trim().isEmpty()) {
                log.info("Broadcasting SSE event: {} to namespace: {}", eventName, namespace);
                sseController.broadcastToNamespace(namespace, eventName, jsonData);
            }
            
            log.info("=== SSE BROADCAST COMPLETED ===");
            
        } catch (Exception e) {
            log.error("Error broadcasting update via SSE", e);
        }
    }

    /**
     * Broadcast custom message to all clients
     */
    public void broadcastMessage(String eventName, Object message) {
        try {
            String jsonData = objectMapper.writeValueAsString(message);
            sseController.broadcastToAll(eventName, jsonData);
            log.info("Broadcasted custom SSE message: {}", eventName);
        } catch (Exception e) {
            log.error("Error broadcasting custom message via SSE: {}", eventName, e);
        }
    }

    /**
     * Broadcast message to specific namespace
     */
    public void broadcastMessageToNamespace(String namespace, String eventName, Object message) {
        try {
            String jsonData = objectMapper.writeValueAsString(message);
            sseController.broadcastToNamespace(namespace, eventName, jsonData);
            log.info("Broadcasted SSE message to namespace {}: {}", namespace, eventName);
        } catch (Exception e) {
            log.error("Error broadcasting message to namespace {} via SSE: {}", namespace, eventName, e);
        }
    }

    /**
     * Send connection status update
     */
    public void broadcastConnectionStatus(String message) {
        try {
            String statusMessage = String.format(
                "{\"type\":\"connection_status\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, java.time.Instant.now().toString()
            );
            sseController.broadcastToAll("status", statusMessage);
            log.info("Broadcasted connection status: {}", message);
        } catch (Exception e) {
            log.error("Error broadcasting connection status", e);
        }
    }

    /**
     * Extract entity type from event type field
     */
    private String getEntityTypeFromEvent(FliptGenericUpdateEvent event) {
        String type = event.getType();
        if (type == null) return "unknown";
        
        if (type.startsWith("flag.")) return "flag";
        if (type.startsWith("segment.")) return "segment";
        if (type.startsWith("constraint.")) return "constraint";
        
        return "unknown";
    }
}
