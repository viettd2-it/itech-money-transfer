package ibank.tech.money.transfer.config;

import ibank.tech.money.transfer.service.FliptFlagsUpdateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "redis.pubsub.enabled", havingValue = "true", matchIfMissing = false)
public class RedisListenerConfig {

    @Value("${redis.pubsub.channel:flipt:flags:update}")
    private String channelName;

    private final FliptFlagsUpdateHandler fliptFlagsUpdateHandler;
    private final ChannelTopic topic;

    @Bean
    public MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(fliptFlagsUpdateHandler, "handleMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListener(), topic);
        log.info("Redis message listener container configured for channel: {}", channelName);
        return container;
    }
}
