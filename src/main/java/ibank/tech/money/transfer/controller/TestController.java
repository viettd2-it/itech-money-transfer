package ibank.tech.money.transfer.controller;

import ibank.tech.money.transfer.service.FliptUnifiedUpdateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller to simulate Redis messages
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final FliptUnifiedUpdateHandler fliptUnifiedUpdateHandler;

    @PostMapping("/simulate-flag-update")
    public ResponseEntity<String> simulateFlagUpdate(
            @RequestParam(defaultValue = "default") String namespace,
            @RequestParam(defaultValue = "test-flag") String flagKey,
            @RequestParam(defaultValue = "updated") String action,
            @RequestParam(required = false) Boolean enabled) {
        
        try {
            String testMessage = String.format("""
                {
                    "type": "flag.update",
                    "data": {
                        "action": "%s",
                        "flag_key": "%s",
                        "namespace": "%s"%s
                    },
                    "timestamp": "2025-08-06T16:05:00Z",
                    "source": "test-controller"
                }
                """, action, flagKey, namespace, 
                enabled != null ? ",\"enabled\":" + enabled : "");
            
            log.info("=== TESTING FLAG UPDATE ===");
            log.info("Simulating Redis message: {}", testMessage);
            
            fliptUnifiedUpdateHandler.handleMessage(testMessage);
            
            return ResponseEntity.ok("Test flag message processed successfully. Check logs and WebSocket clients.");
            
        } catch (Exception e) {
            log.error("Error processing test flag message", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/simulate-constraint-update")
    public ResponseEntity<String> simulateConstraintUpdate(
            @RequestParam(defaultValue = "default") String namespace,
            @RequestParam(defaultValue = "test-segment") String segmentKey,
            @RequestParam(defaultValue = "constraint-123") String constraintId,
            @RequestParam(defaultValue = "updated") String action) {

        try {
            String testMessage = String.format("""
                {
                    "type": "constraint.update",
                    "data": {
                        "action": "%s",
                        "constraint_id": "%s",
                        "segment_key": "%s",
                        "namespace": "%s"
                    },
                    "timestamp": "2025-08-06T16:10:00Z",
                    "source": "test-controller"
                }
                """, action, constraintId, segmentKey, namespace);

            log.info("=== TESTING CONSTRAINT UPDATE ===");
            log.info("Simulating constraint Redis message: {}", testMessage);

            fliptUnifiedUpdateHandler.handleMessage(testMessage);

            return ResponseEntity.ok("Test constraint message processed successfully. Check logs and WebSocket clients.");

        } catch (Exception e) {
            log.error("Error processing test constraint message", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
