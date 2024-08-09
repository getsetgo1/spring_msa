package com.beyond.ordersystem.common.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;

@Service
public class StockInventoryService {
    @Qualifier("3")
    private final RedisTemplate<String,Object> redisTemplate;

    public StockInventoryService(@Qualifier("3") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 상품 등록 시 increaseStock 호출
    public Long increaseStock(Long itemId, int quantity){
        // 레디스가 알아서 아래 메서드의 리턴 값은 잔량값을 리턴

        // 레디스가 음수로 내려갈 경우 추후 재고 update 상황에서 increase값이 정확하지 않을 수 있으므로,
        // 음수면 0으로 setting로직이 필요

        return redisTemplate.opsForValue().increment(String.valueOf(itemId),quantity);
    }
    // 주문 등록 시 decreaseStock 호출
    public Long decreaseStock(Long itemId, int quantity){
        // 레디스가 알아서 아래 메서드의 리턴 값은 잔량값을 리턴
        Object remains = redisTemplate.opsForValue().get(String.valueOf(itemId));
        int longRemains = Integer.parseInt(remains.toString());
        if(longRemains < quantity){
            return -1L;
        }else {
            return redisTemplate.opsForValue().decrement(String.valueOf(itemId), quantity);
        }
    }
}
