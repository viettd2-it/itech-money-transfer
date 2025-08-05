package ibank.tech.money.transfer.controller;

import ibank.tech.money.transfer.service.MultiNamespaceFeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for WebSocket information and status
 */
@RestController
@RequestMapping("/api/v1/websocket")
@RequiredArgsConstructor
@Slf4j
public class WebSocketInfoController {

    private final MultiNamespaceFeatureFlagService multiNamespaceFeatureFlagService;

    /**
     * Get WebSocket connection information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getWebSocketInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("websocketEndpoint", "/ws");
        info.put("subscriptionTopics", new String[]{
            "/topic/flags",           // All flag updates
            "/topic/flags/{namespace}", // Specific namespace updates
            "/topic/system"           // System messages
        });
        info.put("messageTypes", new String[]{
            "flag_update",
            "connection_status", 
            "error"
        });
        
        return ResponseEntity.ok(info);
    }

    /**
     * Get available namespaces for subscription
     */
    @GetMapping("/namespaces")
    public ResponseEntity<Map<String, Object>> getAvailableNamespaces() {
        Map<String, Object> response = new HashMap<>();
        
        Set<String> supportedNamespaces = multiNamespaceFeatureFlagService.getSupportedNamespaces();
        response.put("namespaces", supportedNamespaces);
        response.put("count", supportedNamespaces.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get WebSocket connection status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        status.put("message", "WebSocket server is running and ready for connections");
        
        return ResponseEntity.ok(status);
    }
} 