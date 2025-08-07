# Server-Sent Events (SSE) for Flipt Real-Time Updates

This implementation provides real-time flag, segment, and constraint updates from Flipt using Server-Sent Events (SSE). SSE is perfect for unidirectional communication where the server pushes updates to connected clients.

## 🚀 Overview

The SSE implementation consists of:

1. **SseController** - Manages SSE connections and broadcasting
2. **SseBroadcastService** - Handles message formatting and distribution
3. **FliptUnifiedUpdateHandler** - Routes Redis messages to SSE broadcasting
4. **SSE Test Page** - Browser-based testing interface

## 📡 How It Works

```
Flipt UI Update → Redis Publish → Java Redis Listener → SSE Broadcast → Browser Clients
```

### Flow Diagram
```
┌─────────────┐    ┌─────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Flipt UI  │───▶│    Redis    │───▶│ Java Application │───▶│ Browser Clients │
│             │    │   Channel   │    │                  │    │                 │
│ Flag Update │    │ Pub/Sub     │    │ SSE Broadcasting │    │ Real-time UI    │
└─────────────┘    └─────────────┘    └──────────────────┘    └─────────────────┘
```

## 🔧 Configuration

### application.yml
```yaml
sse:
  enabled: true

redis:
  pubsub:
    enabled: true
    channels:
      flags: flipt:flags:update
      segments: flipt:segments:update
      constraints: flipt:constraints:update
```

## 📋 API Endpoints

### SSE Endpoints

| Endpoint | Description | Response Type |
|----------|-------------|---------------|
| `GET /api/sse/subscribe` | Subscribe to all Flipt updates | `text/event-stream` |
| `GET /api/sse/subscribe/{namespace}` | Subscribe to namespace-specific updates | `text/event-stream` |
| `GET /api/sse/stats` | Get connection statistics | `application/json` |

### Test Endpoints

| Endpoint | Description | Method |
|----------|-------------|---------|
| `/api/test/simulate-flag-update` | Simulate flag update | `POST` |
| `/api/test/simulate-segment-update` | Simulate segment update | `POST` |
| `/api/test/simulate-constraint-update` | Simulate constraint update | `POST` |

## 🌐 Client Implementation

### JavaScript Example

```javascript
// Connect to SSE endpoint
const eventSource = new EventSource('/api/sse/subscribe');

// Handle connection events
eventSource.onopen = function(event) {
    console.log('Connected to SSE');
};

eventSource.onerror = function(event) {
    console.log('SSE connection error:', event);
};

// Listen for flag updates
eventSource.addEventListener('flag.update', function(event) {
    const message = JSON.parse(event.data);
    console.log('Flag update:', message);
    
    // Extract data
    const { action, flag_key, namespace, enabled } = message.data;
    displayFlagUpdate(action, flag_key, namespace, enabled);
});

// Listen for segment updates
eventSource.addEventListener('segment.update', function(event) {
    const message = JSON.parse(event.data);
    const { action, segment_key, namespace } = message.data;
    displaySegmentUpdate(action, segment_key, namespace);
});

// Listen for constraint updates
eventSource.addEventListener('constraint.update', function(event) {
    const message = JSON.parse(event.data);
    const { action, constraint_id, segment_key, namespace } = message.data;
    displayConstraintUpdate(action, constraint_id, segment_key, namespace);
});

// Close connection when done
function disconnect() {
    eventSource.close();
}
```

### Namespace-Specific Subscription

```javascript
// Subscribe to specific namespace only
const bepEventSource = new EventSource('/api/sse/subscribe/bep');

bepEventSource.addEventListener('flag.update', function(event) {
    // Only receives updates for 'bep' namespace
    const message = JSON.parse(event.data);
    console.log('BEP flag update:', message);
});
```

## 📨 Message Formats

### Flag Update Message
```json
{
  "type": "flag.update",
  "data": {
    "action": "updated",
    "flag_key": "feature-toggle",
    "namespace": "bep",
    "enabled": true
  },
  "source": "flipt-server",
  "timestamp": "2025-08-07T10:23:27Z"
}
```

### Segment Update Message
```json
{
  "type": "segment.update",
  "data": {
    "action": "created",
    "segment_key": "premium-users",
    "namespace": "default"
  },
  "source": "flipt-server",
  "timestamp": "2025-08-07T10:23:27Z"
}
```

### Constraint Update Message
```json
{
  "type": "constraint.update",
  "data": {
    "action": "deleted",
    "constraint_id": "constraint-123",
    "segment_key": "premium-users",
    "namespace": "bep"
  },
  "source": "flipt-server",
  "timestamp": "2025-08-07T10:23:27Z"
}
```

## 🎯 Event Types

| Event Type | Description | Data Fields |
|------------|-------------|-------------|
| `flag.update` | Flag created/updated/deleted/enabled/disabled | `action`, `flag_key`, `namespace`, `enabled` |
| `segment.update` | Segment created/updated/deleted | `action`, `segment_key`, `namespace` |
| `constraint.update` | Constraint created/updated/deleted | `action`, `constraint_id`, `segment_key`, `namespace` |
| `connection` | Connection status messages | `message`, `timestamp` |
| `status` | System status updates | `message`, `timestamp` |

## 🧪 Testing

### Using the Test Page

1. **Open**: http://localhost:8282/sse-test.html
2. **Auto-connects** to SSE endpoint
3. **Update flags** in Flipt UI
4. **See real-time updates** in the browser

### Manual Testing with curl

```bash
# Subscribe to all updates
curl -N -H "Accept: text/event-stream" http://localhost:8282/api/sse/subscribe

# Subscribe to specific namespace
curl -N -H "Accept: text/event-stream" http://localhost:8282/api/sse/subscribe/bep

# Simulate flag update
curl -X POST "http://localhost:8282/api/test/simulate-flag-update?namespace=bep&flagKey=test-flag&action=updated&enabled=true"

# Check connection stats
curl http://localhost:8282/api/sse/stats
```

### Browser Developer Tools

1. **Open browser DevTools** (F12)
2. **Go to Network tab**
3. **Filter by "EventStream"**
4. **See SSE connections** and messages in real-time

## 🔍 Monitoring & Debugging

### Connection Statistics

```bash
GET /api/sse/stats
```

Response:
```json
{
  "totalClients": 3,
  "namespaceClients": 2,
  "namespaceStats": {
    "bep": 1,
    "default": 1
  }
}
```

### Application Logs

The application logs detailed information about SSE operations:

```
INFO i.t.m.t.controller.SseController : New SSE client connected. Total clients: 1
INFO i.t.m.t.service.SseBroadcastService : Broadcasting SSE event: flag.update to all clients
INFO i.t.m.t.controller.SseController : Broadcasting SSE message to 1 clients: flag.update
```

## ⚡ Performance Considerations

### Connection Management

- **Automatic cleanup** of dead connections
- **Thread-safe** concurrent access using `CopyOnWriteArrayList`
- **Memory efficient** - removes disconnected clients immediately

### Scalability

- **Lightweight protocol** - HTTP-based, no WebSocket overhead
- **Efficient broadcasting** - Single message sent to multiple clients
- **Namespace filtering** - Reduces unnecessary traffic

### Browser Limits

- **Most browsers** limit ~6 SSE connections per domain
- **Use namespace-specific** subscriptions to optimize connections
- **Consider connection pooling** for high-traffic scenarios

## 🆚 SSE vs WebSocket Comparison

| Feature | SSE | WebSocket |
|---------|-----|-----------|
| **Direction** | Unidirectional (Server → Client) | Bidirectional |
| **Protocol** | HTTP | Custom protocol over TCP |
| **Complexity** | Simple | More complex |
| **Reconnection** | Automatic | Manual implementation |
| **Firewall/Proxy** | Works everywhere | May be blocked |
| **Browser Support** | Native `EventSource` | Native `WebSocket` |
| **Use Case** | Real-time notifications | Interactive applications |

## 🔧 Troubleshooting

### Common Issues

1. **No messages received**
   - Check if Redis is running
   - Verify Redis channel configuration
   - Check browser console for connection errors

2. **Connection drops frequently**
   - Check network stability
   - Verify server logs for errors
   - Consider increasing timeout values

3. **High memory usage**
   - Monitor connection statistics
   - Check for connection leaks
   - Verify cleanup logic is working

### Debug Steps

1. **Check SSE connection**: Open `/api/sse/subscribe` in browser
2. **Verify Redis**: Check Redis logs for published messages
3. **Test manually**: Use curl to simulate messages
4. **Monitor logs**: Check application logs for SSE operations

## 🚀 Production Deployment

### Recommended Settings

```yaml
# application-prod.yml
sse:
  enabled: true
  
server:
  tomcat:
    max-connections: 10000
    threads:
      max: 200
      min-spare: 10

logging:
  level:
    ibank.tech.money.transfer.service.SseBroadcastService: INFO
    ibank.tech.money.transfer.controller.SseController: INFO
```

### Load Balancing

- **Sticky sessions** recommended for SSE connections
- **Consider Redis pub/sub** for multi-instance deployments
- **Monitor connection distribution** across instances

## 🏗️ Architecture Components

### Core Classes

```
SseController.java
├── Manages SSE connections and lifecycle
├── Handles client subscriptions (all/namespace-specific)
├── Broadcasts messages to connected clients
└── Provides connection statistics

SseBroadcastService.java
├── Formats Flipt events for SSE transmission
├── Routes messages to appropriate client groups
├── Handles JSON serialization
└── Provides broadcasting utilities

FliptUnifiedUpdateHandler.java
├── Receives Redis pub/sub messages
├── Parses generic Flipt events
├── Routes to entity-specific processors
└── Triggers SSE broadcasting
```

### Data Flow

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Redis Message   │───▶│ Unified Handler │───▶│ SSE Broadcast   │
│ (JSON String)   │    │ (Parse & Route) │    │ (Format & Send) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │ Entity Processor│    │ Connected       │
                       │ (Business Logic)│    │ SSE Clients     │
                       └─────────────────┘    └─────────────────┘
```

## 🔐 Security Considerations

### Authentication & Authorization

```java
// Example: Add authentication to SSE endpoints
@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(Authentication authentication) {
    if (!isAuthorized(authentication)) {
        throw new AccessDeniedException("Unauthorized");
    }
    // ... rest of implementation
}
```

### Rate Limiting

```java
// Example: Implement rate limiting
@RateLimited(maxRequests = 10, timeWindow = "1m")
@GetMapping(value = "/subscribe")
public SseEmitter subscribe() {
    // ... implementation
}
```

### CORS Configuration

```java
@CrossOrigin(origins = {"https://your-frontend-domain.com"})
@RestController
@RequestMapping("/api/sse")
public class SseController {
    // ... implementation
}
```

## 🧪 Advanced Testing

### Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SseIntegrationTest {

    @Test
    void shouldReceiveFlagUpdateViaSse() throws Exception {
        // Connect to SSE endpoint
        WebTestClient.ResponseSpec response = webTestClient
            .get().uri("/api/sse/subscribe")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk();

        // Simulate flag update
        fliptUnifiedUpdateHandler.handleMessage(flagUpdateJson);

        // Verify SSE message received
        StepVerifier.create(response.returnResult(String.class).getResponseBody())
            .expectNextMatches(data -> data.contains("flag.update"))
            .thenCancel()
            .verify();
    }
}
```

### Load Testing

```bash
# Use Apache Bench to test SSE connections
ab -n 100 -c 10 -H "Accept: text/event-stream" http://localhost:8282/api/sse/subscribe

# Monitor connection statistics
watch -n 1 'curl -s http://localhost:8282/api/sse/stats | jq'
```

## 📊 Metrics & Monitoring

### Custom Metrics

```java
@Component
public class SseMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter connectionCounter;
    private final Gauge activeConnections;

    public SseMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.connectionCounter = Counter.builder("sse.connections.total")
            .description("Total SSE connections")
            .register(meterRegistry);
        this.activeConnections = Gauge.builder("sse.connections.active")
            .description("Active SSE connections")
            .register(meterRegistry, this, SseMetrics::getActiveConnections);
    }
}
```

### Health Checks

```java
@Component
public class SseHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        int activeConnections = sseController.getActiveConnectionCount();

        if (activeConnections >= 0) {
            return Health.up()
                .withDetail("activeConnections", activeConnections)
                .withDetail("status", "SSE service operational")
                .build();
        } else {
            return Health.down()
                .withDetail("status", "SSE service unavailable")
                .build();
        }
    }
}
```

## 📚 Additional Resources

- **SSE Specification**: [W3C Server-Sent Events](https://www.w3.org/TR/eventsource/)
- **MDN Documentation**: [Using Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events)
- **Spring Boot SSE**: [SseEmitter Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html)
- **Redis Pub/Sub**: [Redis Publish/Subscribe](https://redis.io/docs/manual/pubsub/)
- **Flipt Documentation**: [Flipt Feature Flags](https://www.flipt.io/docs)
