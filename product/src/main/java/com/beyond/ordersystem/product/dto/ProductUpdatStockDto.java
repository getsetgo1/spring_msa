package com.beyond.ordersystem.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProductUpdatStockDto {
    private Long productId;
    private Integer productQuantity;
}
