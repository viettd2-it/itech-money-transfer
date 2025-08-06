package ibank.tech.money.transfer.service;

import ibank.tech.money.transfer.dto.FliptGenericUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processor for constraint update events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FliptConstraintProcessor {

    private final MultiNamespaceFeatureFlagService multiNamespaceFeatureFlagService;

    public void processEvent(FliptGenericUpdateEvent event) {
        String action = event.getAction();
        String namespace = event.getNamespace();
        String constraintId = event.getConstraintId();
        String segmentKey = event.getSegmentKey();
        
        log.info("Processing constraint update - action: {}, namespace: {}, segment: {}, constraint: {}", 
                action, namespace, segmentKey, constraintId);
        
        if (action == null) {
            log.warn("Received constraint event with null action");
            refreshConstraintCache(namespace);
            return;
        }
        
        switch (action.toLowerCase()) {
            case "created":
                handleConstraintCreated(namespace, segmentKey, constraintId);
                break;
            case "updated":
                handleConstraintUpdated(namespace, segmentKey, constraintId);
                break;
            case "deleted":
                handleConstraintDeleted(namespace, segmentKey, constraintId);
                break;
            default:
                log.warn("Unknown constraint action: {}", action);
                refreshConstraintCache(namespace);
        }
    }

    private void handleConstraintCreated(String namespace, String segmentKey, String constraintId) {
        log.info("Processing constraint created: namespace={}, segment={}, constraint={}",
                namespace, segmentKey, constraintId);
        refreshConstraintCache(namespace);
    }

    private void handleConstraintUpdated(String namespace, String segmentKey, String constraintId) {
        log.info("Processing constraint updated: namespace={}, segment={}, constraint={}",
                namespace, segmentKey, constraintId);
        refreshConstraintCache(namespace);
    }

    private void handleConstraintDeleted(String namespace, String segmentKey, String constraintId) {
        log.info("Processing constraint deleted: namespace={}, segment={}, constraint={}",
                namespace, segmentKey, constraintId);
        refreshConstraintCache(namespace);
    }

    private void refreshConstraintCache(String namespace) {
        // Constraints affect segment evaluations, which affect flag evaluations, so refresh flag cache
        log.info("Refreshing flag cache due to constraint change in namespace: {}", namespace);
        multiNamespaceFeatureFlagService.refreshFlagCache(namespace);
    }
}
