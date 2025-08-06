package ibank.tech.money.transfer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Generic DTO for all Flipt update events (flags, segments, constraints)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FliptGenericUpdateEvent {
    
    @JsonProperty("type")
    private String type; // e.g., "flag.update", "segment.update", "constraint.update"
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    // Helper methods to extract common fields from data map
    public String getAction() {
        return data != null ? (String) data.get("action") : null;
    }
    
    public String getNamespace() {
        return data != null ? (String) data.get("namespace") : "default";
    }
    
    public String getFlagKey() {
        return data != null ? (String) data.get("flag_key") : null;
    }
    
    public String getSegmentKey() {
        return data != null ? (String) data.get("segment_key") : null;
    }
    
    public String getConstraintId() {
        return data != null ? (String) data.get("constraint_id") : null;
    }
    
    public Boolean getEnabled() {
        Object enabled = data != null ? data.get("enabled") : null;
        if (enabled instanceof Boolean) {
            return (Boolean) enabled;
        }
        return null;
    }
    
    // Helper method to determine entity type from type field
    public EntityType getEntityType() {
        if (type == null) return EntityType.UNKNOWN;
        
        if (type.startsWith("flag.")) return EntityType.FLAG;
        if (type.startsWith("segment.")) return EntityType.SEGMENT;
        if (type.startsWith("constraint.")) return EntityType.CONSTRAINT;
        
        return EntityType.UNKNOWN;
    }
    
    public enum EntityType {
        FLAG, SEGMENT, CONSTRAINT, UNKNOWN
    }
}
