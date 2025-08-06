package ibank.tech.money.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ibank.tech.money.transfer.dto.FliptGenericUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Unified handler for all Flipt entity updates (flags, segments, constraints)
 * Routes events to appropriate processors and handles WebSocket broadcasting
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "redis.pubsub.enabled", havingValue = "true", matchIfMissing = false)
public class FliptUnifiedUpdateHandler {

    private final ObjectMapper objectMapper;
    private final FliptFlagProcessor flagProcessor;
    private final FliptSegmentProcessor segmentProcessor;
    private final FliptConstraintProcessor constraintProcessor;
    private final WebSocketBroadcastService webSocketBroadcastService;

    public void handleMessage(String message) {
        try {
            log.info("=== RECEIVED FLIPT UPDATE MESSAGE FROM REDIS ===");
            log.info("Raw message: {}", message);
            log.info("Message length: {}", message.length());
            log.info("Thread: {}", Thread.currentThread().getName());

            FliptGenericUpdateEvent event = objectMapper.readValue(message, FliptGenericUpdateEvent.class);
            log.info("Parsed event: type={}, source={}, timestamp={}", event.getType(), event.getSource(), event.getTimestamp());

            // Check if data exists
            if (event.getData() == null) {
                log.warn("Received event without data field: {}", message);
                return;
            }

            log.info("Event data: {}", event.getData());
            
            // Route to appropriate processor based on entity type
            FliptGenericUpdateEvent.EntityType entityType = event.getEntityType();
            log.info("Processing {} update - type: {}, action: {}, namespace: {}", 
                    entityType, event.getType(), event.getAction(), event.getNamespace());
            
            switch (entityType) {
                case FLAG:
                    flagProcessor.processEvent(event);
                    break;
                case SEGMENT:
                    segmentProcessor.processEvent(event);
                    break;
                case CONSTRAINT:
                    constraintProcessor.processEvent(event);
                    break;
                default:
                    log.warn("Unknown entity type: {}", event.getType());
            }
            // Broadcast flag update to WebSocket clients
            log.info("=== ATTEMPTING WEBSOCKET BROADCAST ===");
            try {
                webSocketBroadcastService.broadcastUpdate(event);
                log.info("=== WEBSOCKET BROADCAST COMPLETED ===");
            } catch (Exception e) {
                log.error("Failed to broadcast flag update via WebSocket", e);
            }
        } catch (Exception e) {
            log.error("Error processing Flipt update message: {}", message, e);
        }
    }
}
