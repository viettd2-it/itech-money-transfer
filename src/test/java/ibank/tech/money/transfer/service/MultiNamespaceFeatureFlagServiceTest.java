package ibank.tech.money.transfer.service;

import ibank.tech.feature.flag.service.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiNamespaceFeatureFlagServiceTest {

    @Mock
    private FeatureFlagService featureFlagService;

    private MultiNamespaceFeatureFlagService multiNamespaceService;

    @BeforeEach
    void setUp() {
        multiNamespaceService = new MultiNamespaceFeatureFlagService(featureFlagService);
        
        // Set up namespace tokens
        Map<String, String> namespaceTokens = new HashMap<>();
        namespaceTokens.put("bep", "bep-token-123");
        namespaceTokens.put("rdb", "rdb-token-456");
        namespaceTokens.put("default", "default-token-789");
        namespaceTokens.put("unconfigured", "your-unconfigured-token-here"); // This should be ignored
        
        ReflectionTestUtils.setField(multiNamespaceService, "namespaceTokens", namespaceTokens);
        ReflectionTestUtils.setField(multiNamespaceService, "defaultToken", "bep-token-123");
    }

    @Test
    void testRefreshFlagCache_ConfiguredNamespace() {
        // Given
        String namespace = "bep";

        // When
        multiNamespaceService.refreshFlagCache(namespace);

        // Then
        verify(featureFlagService, times(1)).getFlagsByNamespace(namespace);
    }

    @Test
    void testRefreshFlagCache_UnconfiguredNamespace() {
        // Given
        String namespace = "unknown";

        // When
        multiNamespaceService.refreshFlagCache(namespace);

        // Then
        verify(featureFlagService, never()).getFlagsByNamespace(any());
    }

    @Test
    void testRefreshFlagCache_PlaceholderToken() {
        // Given
        String namespace = "unconfigured"; // Has placeholder token

        // When
        multiNamespaceService.refreshFlagCache(namespace);

        // Then
        verify(featureFlagService, never()).getFlagsByNamespace(any());
    }

    @Test
    void testIsNamespaceSupported() {
        // Test configured namespaces
        assertTrue(multiNamespaceService.isNamespaceSupported("bep"));
        assertTrue(multiNamespaceService.isNamespaceSupported("rdb"));
        assertTrue(multiNamespaceService.isNamespaceSupported("default"));
        
        // Test unconfigured namespace
        assertFalse(multiNamespaceService.isNamespaceSupported("unknown"));
        
        // Test placeholder token
        assertFalse(multiNamespaceService.isNamespaceSupported("unconfigured"));
    }

    @Test
    void testGetSupportedNamespaces() {
        // When
        var supportedNamespaces = multiNamespaceService.getSupportedNamespaces();

        // Then
        assertEquals(4, supportedNamespaces.size());
        assertTrue(supportedNamespaces.contains("bep"));
        assertTrue(supportedNamespaces.contains("rdb"));
        assertTrue(supportedNamespaces.contains("default"));
        assertTrue(supportedNamespaces.contains("unconfigured"));
    }

    @Test
    void testRefreshFlagCache_WithException() {
        // Given
        String namespace = "bep";
        doThrow(new RuntimeException("Connection failed")).when(featureFlagService).getFlagsByNamespace(namespace);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> multiNamespaceService.refreshFlagCache(namespace));
        
        verify(featureFlagService, times(1)).getFlagsByNamespace(namespace);
    }

    @Test
    void testBepNamespaceUsesDefaultToken() {
        // Given - clear namespace tokens and only use default token
        ReflectionTestUtils.setField(multiNamespaceService, "namespaceTokens", new HashMap<>());
        ReflectionTestUtils.setField(multiNamespaceService, "defaultToken", "default-bep-token");

        // When
        multiNamespaceService.refreshFlagCache("bep");

        // Then
        verify(featureFlagService, times(1)).getFlagsByNamespace("bep");
    }
}
