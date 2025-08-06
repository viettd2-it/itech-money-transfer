package ibank.tech.money.transfer.service;

import ibank.tech.money.transfer.dto.FliptGenericUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processor for segment update events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FliptSegmentProcessor {

    private final MultiNamespaceFeatureFlagService multiNamespaceFeatureFlagService;

    public void processEvent(FliptGenericUpdateEvent event) {
        String action = event.getAction();
        String namespace = event.getNamespace();
        String segmentKey = event.getSegmentKey();
        
        log.info("Processing segment update - action: {}, namespace: {}, segment: {}", 
                action, namespace, segmentKey);
        
        if (action == null) {
            log.warn("Received segment event with null action");
            refreshSegmentCache(namespace);
            return;
        }
        
        switch (action.toLowerCase()) {
            case "created":
                handleSegmentCreated(namespace, segmentKey);
                break;
            case "updated":
                handleSegmentUpdated(namespace, segmentKey);
                break;
            case "deleted":
                handleSegmentDeleted(namespace, segmentKey);
                break;
            default:
                log.warn("Unknown segment action: {}", action);
                refreshSegmentCache(namespace);
        }
    }

    private void handleSegmentCreated(String namespace, String segmentKey) {
        log.info("Processing segment created: namespace={}, segment={}", namespace, segmentKey);
        refreshSegmentCache(namespace);
    }

    private void handleSegmentUpdated(String namespace, String segmentKey) {
        log.info("Processing segment updated: namespace={}, segment={}", namespace, segmentKey);
        refreshSegmentCache(namespace);
    }

    private void handleSegmentDeleted(String namespace, String segmentKey) {
        log.info("Processing segment deleted: namespace={}, segment={}", namespace, segmentKey);
        refreshSegmentCache(namespace);
    }

    private void refreshSegmentCache(String namespace) {
        // Segments affect flag evaluations, so refresh flag cache
        log.info("Refreshing flag cache due to segment change in namespace: {}", namespace);
        multiNamespaceFeatureFlagService.refreshFlagCache(namespace);
    }
}
