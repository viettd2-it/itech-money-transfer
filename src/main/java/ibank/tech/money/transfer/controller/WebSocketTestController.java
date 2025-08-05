package ibank.tech.money.transfer.controller;

import ibank.tech.money.transfer.dto.FliptFlagUpdateEvent;
import ibank.tech.money.transfer.service.WebSocketBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Controller for testing WebSocket functionality
 */
@RestController
@RequestMapping("/api/v1/websocket")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = false)
public class WebSocketTestController {

    private final WebSocketBroadcastService webSocketBroadcastService;

    @PostMapping("/test/flag-update")
    public ResponseEntity<String> testFlagUpdate(
            @RequestParam String namespace,
            @RequestParam String flagKey,
            @RequestParam String action,
            @RequestParam(required = false) Boolean enabled) {
        
        try {
            // Create test event
            FliptFlagUpdateEvent event = createTestEvent(namespace, flagKey, action, enabled);
            
            // Broadcast via WebSocket
            webSocketBroadcastService.broadcastFlagUpdate(event);
            
            return ResponseEntity.ok("Flag update broadcasted via WebSocket");
            
        } catch (Exception e) {
            log.error("Error testing WebSocket broadcast", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/test/message")
    public ResponseEntity<String> testMessage(
            @RequestParam String topic,
            @RequestParam String message) {
        
        try {
            webSocketBroadcastService.broadcastMessage(topic, message);
            return ResponseEntity.ok("Message broadcasted to topic: " + topic);
        } catch (Exception e) {
            log.error("Error testing WebSocket message", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    private FliptFlagUpdateEvent createTestEvent(String namespace, String flagKey, String action, Boolean enabled) {
        FliptFlagUpdateEvent event = new FliptFlagUpdateEvent();
        
        FliptFlagUpdateEvent.FlagUpdateData data = new FliptFlagUpdateEvent.FlagUpdateData();
        data.setAction(action);
        data.setNamespace(namespace);
        data.setFlagKey(flagKey);
        data.setEnabled(enabled);
        
        event.setData(data);
        event.setSource("test-controller");
        event.setTimestamp(Instant.now().toString());
        event.setType("flag.update");
        
        return event;
    }
}
