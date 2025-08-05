package ibank.tech.money.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FliptFlagsUpdateHandlerTest {

    @Mock
    private MultiNamespaceFeatureFlagService multiNamespaceFeatureFlagService;

    @Mock
    private WebSocketBroadcastService webSocketBroadcastService;

    private FliptFlagsUpdateHandler fliptFlagsUpdateHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fliptFlagsUpdateHandler = new FliptFlagsUpdateHandler(objectMapper, multiNamespaceFeatureFlagService, webSocketBroadcastService);
    }

    @Test
    void testHandleMessage_ValidFlagCreatedEvent() throws Exception {
        // Given
        String message = """
            {
                "data": {
                    "action": "created",
                    "namespace": "bep",
                    "flag_key": "test-flag",
                    "enabled": true
                },
                "source": "flipt-server",
                "timestamp": "2023-12-01T10:00:00Z",
                "type": "flag.update"
            }
            """;

        // When
        fliptFlagsUpdateHandler.handleMessage(message);

        // Then
        verify(multiNamespaceFeatureFlagService, times(1)).refreshFlagCache("bep");
    }

    @Test
    void testHandleMessage_ValidFlagUpdatedEvent() throws Exception {
        // Given
        String message = """
            {
                "data": {
                    "action": "updated",
                    "namespace": "bep",
                    "flag_key": "test-flag",
                    "enabled": false
                },
                "source": "flipt-server",
                "timestamp": "2023-12-01T10:05:00Z",
                "type": "flag.update"
            }
            """;

        // When
        fliptFlagsUpdateHandler.handleMessage(message);

        // Then
        verify(multiNamespaceFeatureFlagService, times(1)).refreshFlagCache("bep");
    }

    @Test
    void testHandleMessage_InvalidJson() {
        // Given
        String invalidMessage = "invalid json";

        // When
        fliptFlagsUpdateHandler.handleMessage(invalidMessage);

        // Then
        verify(multiNamespaceFeatureFlagService, never()).refreshFlagCache(any());
    }

    @Test
    void testHandleMessage_EmptyMessage() {
        // Given
        String emptyMessage = "";

        // When
        fliptFlagsUpdateHandler.handleMessage(emptyMessage);

        // Then
        verify(multiNamespaceFeatureFlagService, never()).refreshFlagCache(any());
    }

    @Test
    void testHandleMessage_FlagDeletedEvent() throws Exception {
        // Given
        String message = """
            {
                "data": {
                    "action": "deleted",
                    "namespace": "bep",
                    "flag_key": "test-flag"
                },
                "source": "flipt-server",
                "timestamp": "2023-12-01T10:10:00Z",
                "type": "flag.update"
            }
            """;

        // When
        fliptFlagsUpdateHandler.handleMessage(message);

        // Then
        verify(multiNamespaceFeatureFlagService, times(1)).refreshFlagCache("bep");
    }
}
