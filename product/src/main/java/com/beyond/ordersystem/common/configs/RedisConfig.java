package com.beyond.ordersystem.common.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.KeyBoundCursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    // application.yml의 spring.redis.host의 정보를 소스코드의 변수로 가져오는 것
    @Value("${spring.redis.host}")
    public String host;

    @Value("${spring.redis.port}")
    public int port;


    @Bean
    @Qualifier("2")
    // RedisConnectionFactory는 Redis서버와의 연결을 설정하는 역할
    // LettuceConnectionFactory는 RedisConnectionFactory의 구현체로서 실질적인 역할 수행
    public RedisConnectionFactory redisConnectionFactory(){
//        return new LettuceConnectionFactory("localhost",6379);
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        // 1번 db 사용
        configuration.setDatabase(1);
//        configuration.setDatabase(2); //아직 안 쓰니까 주석처리..!
//        configuration.setPassword("1234");
        return new LettuceConnectionFactory(configuration);
    }

    //redisTemplate은 redis와 상호작용할 때 redis key,value의 형식을 정의
    @Bean
    @Qualifier("2")
    public RedisTemplate<String,Object> redisTemplate(@Qualifier("2") RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    @Qualifier("3")
    public RedisConnectionFactory stockFactory(){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        // 2번 db 사용
        configuration.setDatabase(2);
//        configuration.setDatabase(2); //아직 안 쓰니까 주석처리..!
//        configuration.setPassword("1234");
        return new LettuceConnectionFactory(configuration);
    }

    //redisTemplate은 redis와 상호작용할 때 redis key,value의 형식을 정의
    @Bean
    @Qualifier("3")
    public RedisTemplate<String,Object> stockRedisTemplate(@Qualifier("3") RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    // for ssecontroller
    @Bean
    @Qualifier("4")
    public RedisConnectionFactory sseFactory(){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(3);
        return new LettuceConnectionFactory(configuration);
    }

    // for ssecontroller
    //redisTemplate은 redis와 상호작용할 때 redis key,value의 형식을 정의
    @Bean
    @Qualifier("4")
    public RedisTemplate<String,Object> sseRedisTemplate(@Qualifier("4") RedisConnectionFactory sseFactory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 객체안에 객체 직렬화 이슈로 인해 아래와 같이 serializer를 커스텀한 것 // 이거 기억해!!!
        Jackson2JsonRedisSerializer<Object> serializer=new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        serializer.setObjectMapper(objectMapper);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setConnectionFactory(sseFactory);
        return redisTemplate;
    }

    // for ssecontroller
    // sse 알림 관련 리스너 객체 생성
    @Bean
    @Qualifier("4")
    public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier("4") RedisConnectionFactory sseFactory){
        RedisMessageListenerContainer container= new RedisMessageListenerContainer();
        container.setConnectionFactory(sseFactory);
        return container;
    }




    //redisTemplate.opsForValue().set(key,value)
    //redisTemplate.opsForValue().get(key)
    //redisTemplate.opsForValue().increment 또는 decrement
}
