package ibank.tech.money.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ibank.tech.money.transfer.dto.FliptFlagUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Handler for Flipt flag update events from Redis channel: flipt:flags:update
 * Processes flag creation, update, and deletion events
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "redis.pubsub.enabled", havingValue = "true", matchIfMissing = false)
public class FliptFlagsUpdateHandler {

    private final ObjectMapper objectMapper;
    private final MultiNamespaceFeatureFlagService multiNamespaceFeatureFlagService;
    private final WebSocketBroadcastService webSocketBroadcastService;

    public void handleMessage(String message) {
        try {
            log.info("Received flag update message: {}", message);

            FliptFlagUpdateEvent event = objectMapper.readValue(message, FliptFlagUpdateEvent.class);

            // Check if data exists
            if (event.getData() == null) {
                log.warn("Received event without data field: {}", message);
                return;
            }

            FliptFlagUpdateEvent.FlagUpdateData data = event.getData();
            String action = data.getAction();
            String namespace = data.getNamespace() != null ? data.getNamespace() : "default";
            String flagKey = getFlagKey(data);
            Boolean enabled = data.getEnabled();

            log.info("Processing flag update - action: {}, namespace: {}, flag: {}, enabled: {}",
                    action, namespace, flagKey, enabled);

            // Check if action is null
            if (action == null) {
                log.warn("Received event with null action: {}", message);
                // Still refresh cache for unknown actions
                refreshFlagCache(namespace);
                return;
            }

            // Handle different types of flag updates
            switch (action.toLowerCase()) {
                case "created":
                    handleFlagCreated(namespace, flagKey, enabled);
                    break;
                case "updated":
                    handleFlagUpdated(namespace, flagKey, enabled);
                    break;
                case "deleted":
                    handleFlagDeleted(namespace, flagKey);
                    break;
                case "enabled":
                    handleFlagEnabled(namespace, flagKey);
                    break;
                case "disabled":
                    handleFlagDisabled(namespace, flagKey);
                    break;
                default:
                    log.warn("Unknown flag action: {}", action);
                    // Still refresh cache for unknown actions
                    refreshFlagCache(namespace);
            }
            
            // Broadcast flag update to WebSocket clients
            try {
                webSocketBroadcastService.broadcastFlagUpdate(event);
            } catch (Exception e) {
                log.warn("Failed to broadcast flag update via WebSocket: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error processing flag update message: {}", message, e);
        }
    }

    private String getFlagKey(FliptFlagUpdateEvent.FlagUpdateData data) {
        // Try to get flag key from different possible fields
        if (data.getFlagKey() != null) {
            return data.getFlagKey();
        }
        if (data.getFlag() != null && data.getFlag().getKey() != null) {
            return data.getFlag().getKey();
        }
        return "unknown";
    }

    private void handleFlagCreated(String namespace, String flagKey, Boolean enabled) {
        log.info("Processing flag created: namespace={}, flag={}, enabled={}", namespace, flagKey, enabled);
        refreshFlagCache(namespace);
    }

    private void handleFlagUpdated(String namespace, String flagKey, Boolean enabled) {
        log.info("Processing flag updated: namespace={}, flag={}, enabled={}", namespace, flagKey, enabled);
        refreshFlagCache(namespace);
    }

    private void handleFlagDeleted(String namespace, String flagKey) {
        log.info("Processing flag deleted: namespace={}, flag={}", namespace, flagKey);
        refreshFlagCache(namespace);
    }

    private void handleFlagEnabled(String namespace, String flagKey) {
        log.info("Processing flag enabled: namespace={}, flag={}", namespace, flagKey);
        refreshFlagCache(namespace);
    }

    private void handleFlagDisabled(String namespace, String flagKey) {
        log.info("Processing flag disabled: namespace={}, flag={}", namespace, flagKey);
        refreshFlagCache(namespace);
    }

    private void refreshFlagCache(String namespace) {
        multiNamespaceFeatureFlagService.refreshFlagCache(namespace);
    }
}
