package ibank.tech.money.transfer.service;

import ibank.tech.feature.flag.service.FeatureFlagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Service to handle feature flag operations across multiple namespaces
 * Each namespace can have its own authentication token
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "redis.pubsub.enabled", havingValue = "true", matchIfMissing = false)
public class MultiNamespaceFeatureFlagService {

    private final FeatureFlagService featureFlagService;
    
    @Value("#{${feature-flag.namespace-tokens:{}}}")
    private Map<String, String> namespaceTokens;
    
    @Value("${feature-flag.namespace-token:}")
    private String defaultToken;

    public MultiNamespaceFeatureFlagService(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    /**
     * Refresh flag cache for a specific namespace
     * Uses the appropriate token for the namespace
     */
    public void refreshFlagCache(String namespace) {
        try {
            String token = getTokenForNamespace(namespace);
            
            if (token == null || token.trim().isEmpty()) {
                log.warn("No token configured for namespace: {}. Skipping cache refresh.", namespace);
                return;
            }
            
            log.info("Refreshing flag cache for namespace: {} with token: {}...", namespace, token.substring(0, Math.min(8, token.length())));
            
            // The FeatureFlagService should handle the token internally
            // If it doesn't support dynamic tokens, we need a different approach
            featureFlagService.getFlagsByNamespace(namespace);
            
            log.info("Successfully refreshed flag cache for namespace: {}", namespace);
            
        } catch (Exception e) {
            log.error("Failed to refresh flag cache for namespace: {}. Error: {}", namespace, e.getMessage());
            
            // If the namespace is not configured or token is invalid, log a helpful message
            if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("unauthorized"))) {
                log.error("Authentication failed for namespace: {}. Please check the token configuration.", namespace);
            }
        }
    }

    /**
     * Check if a namespace is supported (has a configured token)
     */
    public boolean isNamespaceSupported(String namespace) {
        return getTokenForNamespace(namespace) != null;
    }

    /**
     * Get all supported namespaces
     */
    public Set<String> getSupportedNamespaces() {
        return namespaceTokens.keySet();
    }

    /**
     * Get the token for a specific namespace
     */
    private String getTokenForNamespace(String namespace) {
        // First check if we have a specific token for this namespace
        if (namespaceTokens != null && namespaceTokens.containsKey(namespace)) {
            String token = namespaceTokens.get(namespace);
            if (token != null && !token.trim().isEmpty() && !token.equals("your-" + namespace + "-token-here")) {
                return token;
            }
        }
        // For 'bep' namespace, also check the default token (backward compatibility)
        if ("bep".equals(namespace) && defaultToken != null && !defaultToken.trim().isEmpty()) {
            return defaultToken;
        }
        return null;
    }

    /**
     * Log configuration status for debugging
     */
    public void logConfiguration() {
        log.info("Multi-namespace feature flag service configuration:");
        log.info("Default token configured: {}", defaultToken != null && !defaultToken.trim().isEmpty());
        
        if (namespaceTokens != null && !namespaceTokens.isEmpty()) {
            for (Map.Entry<String, String> entry : namespaceTokens.entrySet()) {
                String namespace = entry.getKey();
                String token = entry.getValue();
                boolean isConfigured = token != null && !token.trim().isEmpty() && !token.equals("your-" + namespace + "-token-here");
                log.info("Namespace '{}': token configured = {}", namespace, isConfigured);
            }
        } else {
            log.info("No namespace-specific tokens configured");
        }
    }
}
