package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.configs.RabbitMqConfig;
import com.beyond.ordersystem.ordering.dto.StockDecreaseEvent;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

@Component
public class StockDecreaseEventHandler {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private OrderingService orderingService;
    private final ProductRepository productRepository;
    @Autowired
    public StockDecreaseEventHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void publish(StockDecreaseEvent event){
        rabbitTemplate.convertAndSend(RabbitMqConfig.STOCK_DECREASE_QUEUE,event);
    }

    // 트랜잭션이 완료된 이후에 그다음 메시지 수신하므로, 동시성 이슈 발생 안 함.
    @Transactional
    @RabbitListener(queues = RabbitMqConfig.STOCK_DECREASE_QUEUE)
    public void listen(Message message)  {
        String messageBody = new String(message.getBody());
        // messagebody : json 형태
        // json 메시지를 ObjectMapper로 직접 parsing
        ObjectMapper objectMapper = new ObjectMapper();
        // objectmapper 코드 외워...자주 써!
        // controller에서 requestbody 하면 해주던건데...직접 구현도 해야지?
        try {
            StockDecreaseEvent stockDecreaseEvent = objectMapper.readValue(messageBody, StockDecreaseEvent.class);
            // 재고 update
            Product product =productRepository.findById(stockDecreaseEvent.getProductId()).orElseThrow(()->new EntityNotFoundException("해당 상품은 없습니다."));
            product.updateStockQuantity(stockDecreaseEvent.getProductCount());
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }


    }
}
