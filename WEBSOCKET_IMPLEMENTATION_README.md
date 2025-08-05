# WebSocket Real-Time Flag Updates Implementation

## Overview

This implementation extends your existing Redis pub/sub system with WebSocket functionality to provide real-time flag updates to connected clients. When Redis receives flag update events, they are now broadcast to all connected WebSocket clients in real-time.

## Architecture

```
Redis Pub/Sub → FliptFlagsUpdateHandler → WebSocketService → Connected Clients
```

### Components

1. **WebSocketConfig**: Configures WebSocket endpoints and message broker
2. **WebSocketService**: Handles broadcasting messages to connected clients
3. **WebSocketController**: Handles client subscriptions and messages
4. **WebSocketInfoController**: REST endpoints for WebSocket information
5. **FliptFlagsUpdateHandler**: Enhanced to broadcast updates via WebSocket

## Features

### Real-Time Updates
- Flag updates are broadcast to all connected clients immediately
- Support for namespace-specific subscriptions
- System messages for connection status

### Subscription Topics
- `/topic/flags` - All flag updates
- `/topic/flags/{namespace}` - Updates for specific namespace
- `/topic/system` - System messages
- `/user/queue/messages` - User-specific messages
- `/user/queue/subscription` - Subscription responses

### Message Types
- `flag_update` - Flag creation, update, deletion, enable/disable
- `connection_status` - Connection and subscription status
- `error` - Error messages

## API Endpoints

### WebSocket
- **Endpoint**: `/ws`
- **Protocol**: STOMP over SockJS
- **Fallback**: SockJS for browsers without WebSocket support

### REST API
- `GET /api/v1/websocket/info` - WebSocket connection information
- `GET /api/v1/websocket/namespaces` - Available namespaces
- `GET /api/v1/websocket/status` - Connection status

## Client Integration

### JavaScript/Web Client
```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    // Subscribe to all flag updates
    stompClient.subscribe('/topic/flags', function (message) {
        const update = JSON.parse(message.body);
        console.log('Flag update:', update);
    });
    
    // Subscribe to specific namespace
    stompClient.subscribe('/topic/flags/bep', function (message) {
        const update = JSON.parse(message.body);
        console.log('BEP flag update:', update);
    });
});
```

### Message Format
```json
{
    "type": "flag_update",
    "namespace": "bep",
    "flagKey": "new-feature",
    "action": "enabled",
    "enabled": true,
    "timestamp": 1640995200000
}
```

## Testing

### Web Interface
Access the test page at: `http://localhost:8282/websocket-test.html`

Features:
- Real-time connection status
- Subscribe to specific namespaces
- Message log with timestamps
- Clear message history

### Manual Testing
1. Start the application
2. Open the test page in multiple browser tabs
3. Trigger flag updates via Redis pub/sub
4. Observe real-time updates in all connected clients

## Configuration

### Application Properties
```yaml
spring:
  websocket:
    allowed-origins: "*"  # Configure CORS as needed

redis:
  pubsub:
    enabled: true
    channel: flipt:flags:update
```

### Dependencies
The following dependencies were added to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## Security Considerations

1. **CORS Configuration**: Currently allows all origins (`*`). Configure appropriately for production.
2. **Authentication**: Consider adding authentication for WebSocket connections.
3. **Rate Limiting**: Implement rate limiting for WebSocket connections.
4. **Message Validation**: Validate incoming WebSocket messages.

## Production Deployment

### Load Balancing
- Use sticky sessions for WebSocket connections
- Consider using Redis for session storage

### Monitoring
- Monitor WebSocket connection count
- Track message delivery rates
- Set up alerts for connection failures

### Scaling
- WebSocket connections are memory-intensive
- Consider horizontal scaling with Redis pub/sub for cross-instance communication

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Check if WebSocket endpoint is accessible
   - Verify CORS configuration

2. **Messages Not Received**
   - Check Redis pub/sub configuration
   - Verify topic subscriptions
   - Check browser console for errors

3. **Memory Issues**
   - Monitor WebSocket connection count
   - Implement connection cleanup

### Debug Logging
Enable debug logging for WebSocket:
```yaml
logging:
  level:
    org.springframework.web.socket: DEBUG
    ibank.tech.money.transfer.service.WebSocketService: DEBUG
```

## Future Enhancements

1. **Authentication**: Add JWT-based authentication for WebSocket connections
2. **Message Persistence**: Store recent messages for new connections
3. **Connection Management**: Implement connection pooling and cleanup
4. **Metrics**: Add Prometheus metrics for WebSocket usage
5. **Compression**: Enable message compression for large payloads 