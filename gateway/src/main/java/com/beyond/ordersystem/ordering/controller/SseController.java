package com.beyond.ordersystem.ordering.controller;

import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class SseController implements MessageListener {
    // SseEmiter는 연결된 사용자 정보를 의미
    // ConcurrentHashMap는 Thread-safe한 map(동시성 이슈 발생 안 함)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    // 여러번 구독을 방지하기 위한 ConcurrentHashSet의 변수 생성
    private Set<String> subscribelist = ConcurrentHashMap.newKeySet();

    @Qualifier("4")
    private final RedisTemplate<String,Object> sseRedisTemplate;

    public SseController(RedisMessageListenerContainer redisMessageListenerContainer, @Qualifier("4")RedisTemplate<String, Object> sseRedisTemplate) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.sseRedisTemplate = sseRedisTemplate;
    }

    // email에 해당되는 메시지를 listen하는 listener를 추가한 것
    public void subscribeChannel(String email){
        // 이미 구독한 email일 경우네는 더이상 구독하지 않는 분기처리
        // 이걸 안 하면 listening이 여러번 찍히고 그래서..
        if(!subscribelist.contains(email)){
            MessageListenerAdapter listenerAdapter = createListenerAdapter(this);
            redisMessageListenerContainer.addMessageListener(listenerAdapter,new PatternTopic(email));
            subscribelist.add(email);

        }

    }
    private MessageListenerAdapter createListenerAdapter(SseController sseController){
        return new MessageListenerAdapter(sseController,"onMessage");
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(){
        SseEmitter emitter = new SseEmitter(14400*60*1000L); //
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        emitters.put(email,emitter);
        emitter.onCompletion(()->emitters.remove(email));
        emitter.onTimeout(()->emitters.remove(email));
        try{
            // eventName : 실질적인 메세지임!!!
            emitter.send(SseEmitter.event().name("connect").data("connected!!"));
        }catch (IOException e){
            e.printStackTrace();
        }
        subscribeChannel(email);
        return emitter;
    }

    public void publicMessage(OrderListResDto dto, String email){
        SseEmitter emitter = emitters.get(email);
        // emitter 있으면 내가 처리
        // redis pub/sub 실습 테스트를 위해 잠시 주석처리해야함. sseRedisTemplate만 있고 나머지는 다 주석처리해야함
        if(emitter !=null){
            try {
                emitter.send(SseEmitter.event().name("ordered").data(dto));
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }
        //emitter 없으면 레디스에 배포(노션 sse 알림 노트 보면 좀 알거야)
        else{
            // redisconfig에 4번 qualifier야!
            // convertAndSend : 직렬화해서 보내겠다는 것
            // 레디스에 알림 메세지를 주는 것
            sseRedisTemplate.convertAndSend(email,dto);
        }

    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        아래는 message 내용 parsing 해주는 것
        ObjectMapper objectMapper  = new ObjectMapper();
        try {
            OrderListResDto dto=objectMapper.readValue(message.getBody(), OrderListResDto.class);
            String email = new String(pattern, StandardCharsets.UTF_8);
            SseEmitter emitter =emitters.get(email);

            if(emitter != null){
                emitter.send(SseEmitter.event().name("ordered").data(dto));
            }

            System.out.println("listening");
            System.out.println(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
