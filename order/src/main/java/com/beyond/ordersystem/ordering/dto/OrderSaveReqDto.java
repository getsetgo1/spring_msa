package com.beyond.ordersystem.ordering.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSaveReqDto {
        private Long productId;
        private Integer productCount; // 단 list로 받아야하기 때문에 service에서 List로 받으면 됨

}
