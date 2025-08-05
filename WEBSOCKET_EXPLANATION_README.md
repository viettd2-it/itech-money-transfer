# WebSocket Real-Time Flag Updates - Complete Explanation

## 🎯 What Problem Are We Solving?

We want to notify all connected clients (web browsers, mobile apps) **immediately** when a feature flag changes, without clients having to keep asking the server ("polling"). This is called **real-time updates**.

## 🔄 How Did It Work Before?

**Previous Flow:**
```
Flag Change → Redis Pub/Sub → Java Backend (cache refresh only)
```

- When a flag changed, a message was published to a Redis channel
- Your Java backend (the "subscriber") listened to this channel and refreshed its cache
- **Problem**: Clients didn't know about the change until they refreshed or polled the server

## 🚀 What's New? (WebSocket Layer)

**New Flow:**
```
Flag Change → Redis Pub/Sub → Java Backend → WebSocket → Connected Clients
```

We added a **WebSocket layer** that sits between your Redis subscriber and your clients.

## 📡 How WebSockets Work

**WebSocket** = A persistent connection between a client (browser) and server that stays open and allows real-time, two-way communication.

**Analogy:**
- **HTTP** = Sending letters back and forth (request → response → done)
- **WebSocket** = Making a phone call that stays connected (both sides can talk anytime)

## 🔄 The Complete Flow Step-by-Step

### Step 1: Client Connects
```javascript
// Browser connects to your server
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.connect();
```

### Step 2: Flag Changes in Redis
```json
// Someone publishes this to Redis channel "flipt:flags:update"
{
  "data": {
    "action": "enabled",
    "namespace": "bep", 
    "flag_key": "new-feature",
    "enabled": true
  }
}
```

### Step 3: Your Java Backend Receives It
```java
// FliptFlagsUpdateHandler.handleMessage() gets called
public void handleMessage(String message) {
    // 1. Parse the Redis message
    FliptFlagUpdateEvent event = objectMapper.readValue(message, FliptFlagUpdateEvent.class);
    
    // 2. Refresh cache (existing functionality)
    refreshFlagCache(namespace);
    
    // 3. NEW: Broadcast to WebSocket clients
    webSocketService.broadcastFlagUpdate(namespace, flagKey, action, enabled);
}
```

### Step 4: WebSocket Broadcasts to All Clients
```java
// WebSocketService.broadcastFlagUpdate()
public void broadcastFlagUpdate(String namespace, String flagKey, String action, Boolean enabled) {
    WebSocketMessage message = WebSocketMessage.flagUpdate(namespace, flagKey, action, enabled);
    
    // Send to ALL connected clients
    messagingTemplate.convertAndSend("/topic/flags", message);
    
    // Send to clients subscribed to specific namespace
    messagingTemplate.convertAndSend("/topic/flags/" + namespace, message);
}
```

### Step 5: Client Receives Real-Time Update
```javascript
// Browser receives the message instantly
stompClient.subscribe('/topic/flags', function(message) {
    const update = JSON.parse(message.body);
    console.log('Flag changed:', update);
    // Update UI immediately
});
```

## 🏗️ Key Components Explained

### **WebSocketConfig.java**
```java
@Configuration
@EnableWebSocketMessageBroker  // ← This enables WebSocket support
public class WebSocketConfig {
    // Sets up the "post office" for messages
    // /topic/flags = like a TV channel everyone can watch
    // /user/queue/messages = like a private mailbox
}
```

**What it does:**
- Enables WebSocket support in Spring Boot
- Configures message broker (the "post office")
- Sets up topics and queues for message routing

### **WebSocketService.java**
```java
@Service
public class WebSocketService {
    // This is the "broadcaster" - sends messages to all connected clients
    public void broadcastFlagUpdate(...) {
        // Like a radio station broadcasting to all listeners
    }
}
```

**What it does:**
- Handles sending messages to connected clients
- Manages different types of broadcasts (all clients, specific namespaces, individual users)
- Provides error handling and logging

### **FliptFlagsUpdateHandler.java** (Enhanced)
```java
// Before: Only refreshed cache
// After: Refreshes cache + broadcasts to WebSocket clients
public void handleMessage(String message) {
    // Existing cache refresh logic...
    refreshFlagCache(namespace);
    
    // NEW: Broadcast to WebSocket clients
    webSocketService.broadcastFlagUpdate(namespace, flagKey, action, enabled);
}
```

**What changed:**
- Added WebSocketService as a dependency
- After processing Redis message, it now broadcasts to all connected clients
- Maintains existing cache refresh functionality

### **WebSocketController.java**
```java
@Controller
public class WebSocketController {
    // Handles client subscriptions and messages
    @MessageMapping("/subscribe")
    public WebSocketMessage handleSubscription(...) {
        // Manages client subscriptions to specific namespaces
    }
}
```

**What it does:**
- Handles client-to-server messages
- Manages subscriptions to specific namespaces
- Provides connection status and error handling

## 📨 Topics vs Queues

### **Topics** (`/topic/flags`)
- Like a TV channel - everyone subscribed gets the message
- Used for broadcasting to all clients
- Example: `/topic/flags` (all flag updates), `/topic/flags/bep` (BEP namespace only)

### **Queues** (`/user/queue/messages`)
- Like a private mailbox - only one specific user gets the message
- Used for personal notifications
- Example: `/user/queue/messages` (personal messages), `/user/queue/subscription` (subscription responses)

## 🎯 Why This Works So Well

### **1. Real-time Updates**
- No polling needed - clients get updates instantly
- Immediate UI updates when flags change

### **2. Efficient**
- One Redis message → many WebSocket clients
- No repeated HTTP requests

### **3. Scalable**
- Can handle thousands of connected clients
- Memory-efficient message broadcasting

### **4. Flexible**
- Clients can subscribe to specific namespaces or all updates
- Support for both general and targeted broadcasts

## 🧪 Testing the Implementation

### **Web Interface**
Access: `http://localhost:8282/websocket-test.html`

**Features:**
- Real-time connection status
- Subscribe to specific namespaces
- Message log with timestamps
- Clear message history

### **Manual Testing Steps**
1. Start the application: `./mvnw spring-boot:run`
2. Open test page in multiple browser tabs
3. Connect to WebSocket in each tab
4. Subscribe to different namespaces
5. Trigger Redis messages to see real-time updates

### **REST API Testing**
```bash
# Get WebSocket info
curl http://localhost:8282/api/v1/websocket/info

# Get available namespaces
curl http://localhost:8282/api/v1/websocket/namespaces

# Get connection status
curl http://localhost:8282/api/v1/websocket/status
```

## 📋 Message Format

### **Flag Update Message**
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

### **Connection Status Message**
```json
{
    "type": "connection_status",
    "message": "Successfully subscribed to flag updates",
    "timestamp": 1640995200000
}
```

### **Error Message**
```json
{
    "type": "error",
    "message": "Failed to subscribe: Invalid namespace",
    "timestamp": 1640995200000
}
```

## 🔧 Client Integration Examples

### **JavaScript/Web Client**
```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connected to WebSocket');
    
    // Subscribe to all flag updates
    stompClient.subscribe('/topic/flags', function (message) {
        const update = JSON.parse(message.body);
        console.log('Flag update:', update);
        updateUI(update);
    });
    
    // Subscribe to specific namespace
    stompClient.subscribe('/topic/flags/bep', function (message) {
        const update = JSON.parse(message.body);
        console.log('BEP flag update:', update);
    });
    
    // Subscribe to system messages
    stompClient.subscribe('/topic/system', function (message) {
        const systemMsg = JSON.parse(message.body);
        console.log('System message:', systemMsg);
    });
});
```

### **React Component Example**
```jsx
import { useEffect, useState } from 'react';

function FlagUpdates() {
    const [updates, setUpdates] = useState([]);
    
    useEffect(() => {
        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        
        stompClient.connect({}, function (frame) {
            stompClient.subscribe('/topic/flags', function (message) {
                const update = JSON.parse(message.body);
                setUpdates(prev => [...prev, update]);
            });
        });
        
        return () => {
            stompClient.disconnect();
        };
    }, []);
    
    return (
        <div>
            <h2>Real-time Flag Updates</h2>
            {updates.map((update, index) => (
                <div key={index}>
                    {update.action}: {update.flagKey} ({update.namespace})
                </div>
            ))}
        </div>
    );
}
```

## 🚀 Production Considerations

### **Security**
- Configure CORS appropriately for production
- Consider adding authentication for WebSocket connections
- Implement rate limiting for connections

### **Monitoring**
- Monitor WebSocket connection count
- Track message delivery rates
- Set up alerts for connection failures

### **Scaling**
- WebSocket connections are memory-intensive
- Consider horizontal scaling with Redis pub/sub for cross-instance communication
- Use sticky sessions for load balancing

## 🔍 Troubleshooting

### **Common Issues**

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

### **Debug Logging**
```yaml
logging:
  level:
    org.springframework.web.socket: DEBUG
    ibank.tech.money.transfer.service.WebSocketService: DEBUG
```

## 🎉 Summary

**Before**: "Hey server, any flag changes?" → "No" → (repeat every 30 seconds)

**After**: Server → "FLAG CHANGED!" → All connected clients instantly know

It's like upgrading from checking your mailbox every day to having a phone that rings immediately when you get important mail!

The implementation seamlessly integrates with your existing Redis pub/sub system while adding powerful real-time capabilities that provide immediate flag change notifications to all connected clients. 