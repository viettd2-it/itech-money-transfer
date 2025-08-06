package ibank.tech.money.transfer.config;

import ibank.tech.money.transfer.service.FliptUnifiedUpdateHandler;
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

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "redis.pubsub.enabled", havingValue = "true", matchIfMissing = false)
public class RedisListenerConfig {

    @Value("#{'${redis.pubsub.channels}'.split(',')}")
    private List<String> channelNames;

    private final FliptUnifiedUpdateHandler fliptUnifiedUpdateHandler;

    @Bean
    public MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(fliptUnifiedUpdateHandler, "handleMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        MessageListenerAdapter listener = messageListener();
        for (String channelName : channelNames) {
            ChannelTopic topic = new ChannelTopic(channelName);
            container.addMessageListener(listener, topic);
            log.info("Redis message listener configured for channel: {}", channelName);
        }

        return container;
    }
}
