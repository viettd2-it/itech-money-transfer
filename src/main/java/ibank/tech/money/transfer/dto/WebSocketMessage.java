package ibank.tech.money.transfer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for WebSocket messages to clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {
    
    private String type; // "flag_update", "connection_status", "error"
    private String namespace;
    private String flagKey;
    private String action; // "created", "updated", "deleted", "enabled", "disabled"
    private Boolean enabled;
    private String message;
    private Long timestamp;
    
    public static WebSocketMessage flagUpdate(String namespace, String flagKey, String action, Boolean enabled) {
        return new WebSocketMessage("flag_update", namespace, flagKey, action, enabled, null, System.currentTimeMillis());
    }
    
    public static WebSocketMessage connectionStatus(String message) {
        return new WebSocketMessage("connection_status", null, null, null, null, message, System.currentTimeMillis());
    }
    
    public static WebSocketMessage error(String message) {
        return new WebSocketMessage("error", null, null, null, null, message, System.currentTimeMillis());
    }
} 