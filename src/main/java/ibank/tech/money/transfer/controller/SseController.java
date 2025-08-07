package ibank.tech.money.transfer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events controller for real-time Flipt updates
 */
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    // Store all active SSE connections
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    
    // Store namespace-specific connections
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> namespaceEmitters = new ConcurrentHashMap<>();

    /**
     * Subscribe to all Flipt updates
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout
        
        // Add to global emitters list
        emitters.add(emitter);
        
        log.info("New SSE client connected. Total clients: {}", emitters.size());
        
        // Send welcome message
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("{\"type\":\"connection_status\",\"message\":\"Connected to Flipt updates\",\"timestamp\":\"" + 
                          java.time.Instant.now().toString() + "\"}"));
        } catch (IOException e) {
            log.warn("Failed to send welcome message to SSE client", e);
        }
        
        // Handle client disconnect
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE client disconnected. Remaining clients: {}", emitters.size());
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("SSE client timed out. Remaining clients: {}", emitters.size());
        });
        
        emitter.onError((ex) -> {
            emitters.remove(emitter);
            log.warn("SSE client error. Remaining clients: {}", emitters.size(), ex);
        });
        
        return emitter;
    }

    /**
     * Subscribe to specific namespace updates
     */
    @GetMapping(value = "/subscribe/{namespace}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNamespace(@PathVariable String namespace) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        // Add to namespace-specific emitters
        namespaceEmitters.computeIfAbsent(namespace, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        log.info("New SSE client connected to namespace: {}. Total clients for {}: {}", 
                namespace, namespace, namespaceEmitters.get(namespace).size());
        
        // Send welcome message
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("{\"type\":\"connection_status\",\"message\":\"Connected to " + namespace + " updates\",\"timestamp\":\"" + 
                          java.time.Instant.now().toString() + "\"}"));
        } catch (IOException e) {
            log.warn("Failed to send welcome message to SSE client for namespace: {}", namespace, e);
        }
        
        // Handle client disconnect
        emitter.onCompletion(() -> {
            CopyOnWriteArrayList<SseEmitter> nsEmitters = namespaceEmitters.get(namespace);
            if (nsEmitters != null) {
                nsEmitters.remove(emitter);
                log.info("SSE client disconnected from namespace: {}. Remaining: {}", namespace, nsEmitters.size());
            }
        });
        
        emitter.onTimeout(() -> {
            CopyOnWriteArrayList<SseEmitter> nsEmitters = namespaceEmitters.get(namespace);
            if (nsEmitters != null) {
                nsEmitters.remove(emitter);
                log.info("SSE client timed out for namespace: {}. Remaining: {}", namespace, nsEmitters.size());
            }
        });
        
        emitter.onError((ex) -> {
            CopyOnWriteArrayList<SseEmitter> nsEmitters = namespaceEmitters.get(namespace);
            if (nsEmitters != null) {
                nsEmitters.remove(emitter);
                log.warn("SSE client error for namespace: {}. Remaining: {}", namespace, nsEmitters.size(), ex);
            }
        });
        
        return emitter;
    }

    /**
     * Broadcast message to all connected clients
     */
    public void broadcastToAll(String eventName, String data) {
        log.info("Broadcasting SSE message to {} clients: {}", emitters.size(), eventName);
        
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                return false; // Keep emitter
            } catch (IOException e) {
                log.warn("Failed to send SSE message to client, removing", e);
                return true; // Remove emitter
            }
        });
    }

    /**
     * Broadcast message to namespace-specific clients
     */
    public void broadcastToNamespace(String namespace, String eventName, String data) {
        CopyOnWriteArrayList<SseEmitter> nsEmitters = namespaceEmitters.get(namespace);
        if (nsEmitters == null || nsEmitters.isEmpty()) {
            log.debug("No SSE clients connected to namespace: {}", namespace);
            return;
        }
        
        log.info("Broadcasting SSE message to {} clients in namespace {}: {}", nsEmitters.size(), namespace, eventName);
        
        nsEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                return false; // Keep emitter
            } catch (IOException e) {
                log.warn("Failed to send SSE message to client in namespace {}, removing", namespace, e);
                return true; // Remove emitter
            }
        });
    }

    /**
     * Get connection statistics
     */
    @GetMapping("/stats")
    public Object getStats() {
        return new Object() {
            public final int totalClients = emitters.size();
            public final int namespaceClients = namespaceEmitters.values().stream()
                    .mapToInt(CopyOnWriteArrayList::size).sum();
            public final java.util.Map<String, Integer> namespaceStats = namespaceEmitters.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            java.util.Map.Entry::getKey,
                            entry -> entry.getValue().size()
                    ));
        };
    }
}
