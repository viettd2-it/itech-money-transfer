# Redis Subscriber for Flipt Flag Updates

This implementation provides a Redis subscriber that listens to flag creation/update/deletion events from your Golang Flipt integration.

## Overview

The Redis subscriber consists of these components:

1. **RedisConfig** - Configuration for Redis connection and message listener
2. **FliptFlagsUpdateHandler** - Handles flag creation, update, and deletion events
3. **FliptFlagUpdateEvent** - DTO for flag update event data structure

## Configuration

### application.yml

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms

redis:
  pubsub:
    channel: flipt:flags:update
    enabled: true

feature-flag:
  flipt-url: http://localhost:8080
  # Single token for backward compatibility (will be used for 'bep' namespace)
  namespace-token: wDR3bzX6SQ7iMFE_IpdzsWCVd3ft9-CHvp1Ep7o5NGI=
  # Multiple namespace tokens
  namespace-tokens:
    bep: wDR3bzX6SQ7iMFE_IpdzsWCVd3ft9-CHvp1Ep7o5NGI=
    rdb: your-rdb-token-here
    default: your-default-token-here
    # Add more namespaces as needed
```

## Multi-Namespace Support

The subscriber supports multiple namespaces, each with its own authentication token. This solves the problem where you have multiple namespaces (bep, rdb, default, etc.) but only one configured token.

### How it works:
- **Configured namespaces**: Only refresh cache for namespaces that have valid tokens
- **Unconfigured namespaces**: Skip cache refresh and log a warning
- **Backward compatibility**: The `bep` namespace can use either the specific token or the default `namespace-token`
- **Placeholder detection**: Tokens like `your-namespace-token-here` are treated as unconfigured

### Configuration:
1. **Replace placeholder tokens** with your actual Flipt namespace tokens
2. **Add new namespaces** as needed in the `namespace-tokens` section
3. **Remove unused namespaces** to keep configuration clean

### Environment Variables (Optional)

You can override configuration using environment variables:
- `SPRING_REDIS_HOST` - Redis host (default: localhost)
- `SPRING_REDIS_PORT` - Redis port (default: 6379)
- `REDIS_PUBSUB_CHANNEL` - Redis channel name (default: flipt:flags:update)
- `REDIS_PUBSUB_ENABLED` - Enable/disable subscriber (default: true)

## Expected Message Format

The subscriber listens to the `flipt:flags:update` channel and expects JSON messages in the following format (matching your Golang publisher):

```json
{
  "data": {
    "action": "created|updated|deleted|enabled|disabled",
    "namespace": "default",
    "flag_key": "flag-key",
    "enabled": true
  },
  "source": "flipt-server",
  "timestamp": "2025-07-30T09:52:50Z",
  "type": "flag.update"
}
```

Alternative format with nested flag object:
```json
{
  "data": {
    "action": "created",
    "namespace": "default",
    "flag": {
      "key": "flag-key",
      "name": "Flag Name",
      "description": "Flag description",
      "enabled": true,
      "type": "boolean"
    }
  },
  "source": "flipt-server",
  "timestamp": "2025-07-30T09:52:50Z",
  "type": "flag.update"
}
```

## Supported Actions

- `created` - When a new feature flag is created
- `updated` - When an existing feature flag is modified
- `deleted` - When a feature flag is deleted
- `enabled` - When a feature flag is enabled
- `disabled` - When a feature flag is disabled

## How It Works

1. **Message Reception**: The `FliptFlagsUpdateHandler` receives messages from the `flipt:flags:update` Redis channel
2. **JSON Parsing**: Messages are parsed into `FliptFlagUpdateEvent` objects using Jackson
3. **Event Processing**: The handler processes different flag actions (created/updated/deleted/enabled/disabled)
4. **Cache Refresh**: For each event, the feature flag cache is refreshed by calling the existing `FeatureFlagService`

## Testing

### Unit Tests

Run the unit tests:
```bash
./mvnw test
```

### Manual Testing

1. Start Redis server:
```bash
redis-server
```

2. Start the Spring Boot application:
```bash
./mvnw spring-boot:run
```

3. Test by publishing messages directly to Redis (see Redis CLI testing below)

### Testing with Redis CLI

You can publish test messages directly using Redis CLI (matching the format from your Golang publisher):

```bash
# Flag Created
redis-cli PUBLISH flipt:flags:update '{"data":{"action":"created","namespace":"default","flag_key":"test-flag","enabled":true},"source":"flipt-server","timestamp":"2025-07-30T09:52:50Z","type":"flag.update"}'

# Flag Updated
redis-cli PUBLISH flipt:flags:update '{"data":{"action":"updated","namespace":"default","flag_key":"test-flag","enabled":false},"source":"flipt-server","timestamp":"2025-07-30T09:52:50Z","type":"flag.update"}'

# Flag Deleted
redis-cli PUBLISH flipt:flags:update '{"data":{"action":"deleted","namespace":"default","flag_key":"test-flag"},"source":"flipt-server","timestamp":"2025-07-30T09:52:50Z","type":"flag.update"}'

# Flag Enabled
redis-cli PUBLISH flipt:flags:update '{"data":{"action":"enabled","namespace":"default","flag_key":"test-flag"},"source":"flipt-server","timestamp":"2025-07-30T09:52:50Z","type":"flag.update"}'

# Flag Disabled
redis-cli PUBLISH flipt:flags:update '{"data":{"action":"disabled","namespace":"default","flag_key":"test-flag"},"source":"flipt-server","timestamp":"2025-07-30T09:52:50Z","type":"flag.update"}'
```

## Integration with Golang Publisher

Make sure your Golang publisher publishes messages to the `flipt:flags:update` Redis channel with the expected JSON format.

Example Golang code structure:
```go
type FliptFlagUpdateEvent struct {
    Action    string    `json:"action"`
    Namespace string    `json:"namespace"`
    FlagKey   string    `json:"flag_key"`
    Enabled   *bool     `json:"enabled,omitempty"`
    Flag      *FlagData `json:"flag,omitempty"`
    Timestamp string    `json:"timestamp"`
}

type FlagData struct {
    Key         string `json:"key"`
    Name        string `json:"name"`
    Description string `json:"description"`
    Enabled     bool   `json:"enabled"`
    Type        string `json:"type"`
}
```

## Troubleshooting

1. **Redis Connection Issues**: Check Redis server is running and accessible
2. **Message Not Received**: Verify channel name matches between publisher and subscriber
3. **JSON Parsing Errors**: Check message format matches expected structure
4. **Feature Flag Service Errors**: Ensure Flipt server is accessible and credentials are correct

## Disabling the Subscriber

To disable the Redis subscriber, set:
```yaml
redis:
  pubsub:
    enabled: false
```

Or use environment variable:
```bash
REDIS_PUBSUB_ENABLED=false
```
