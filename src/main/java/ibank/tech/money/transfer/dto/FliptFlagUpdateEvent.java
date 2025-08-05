package ibank.tech.money.transfer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Flipt flag update events from Redis channel: flipt:flags:update
 * Matches the actual message structure from Golang publisher
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FliptFlagUpdateEvent {

    @JsonProperty("data")
    private FlagUpdateData data;

    @JsonProperty("source")
    private String source;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("type")
    private String type;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FlagUpdateData {
        @JsonProperty("action")
        private String action; // e.g., "created", "updated", "deleted", "enabled", "disabled"

        @JsonProperty("flag_key")
        private String flagKey;

        @JsonProperty("namespace")
        private String namespace;

        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("flag")
        private FlagDetails flag;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FlagDetails {
        @JsonProperty("key")
        private String key;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("type")
        private String type;
    }
}
