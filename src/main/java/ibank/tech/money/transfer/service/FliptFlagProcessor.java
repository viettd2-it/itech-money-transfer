package ibank.tech.money.transfer.service;

import ibank.tech.money.transfer.dto.FliptGenericUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processor for flag update events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FliptFlagProcessor {

    private final MultiNamespaceFeatureFlagService multiNamespaceFeatureFlagService;
    private final WebSocketBroadcastService webSocketBroadcastService;

    public void processEvent(FliptGenericUpdateEvent event) {
        String action = event.getAction();
        String namespace = event.getNamespace();
        String flagKey = event.getFlagKey();
        Boolean enabled = event.getEnabled();
        
        log.info("Processing flag update - action: {}, namespace: {}, flag: {}, enabled: {}", 
                action, namespace, flagKey, enabled);
        
        if (action == null) {
            log.warn("Received flag event with null action");
            refreshFlagCache(namespace);
            return;
        }
        
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
                refreshFlagCache(namespace);
        }
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
