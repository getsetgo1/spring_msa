package com.beyond.ordersystem.ordering.dto;

import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductDto {
    private Long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private String imagePath;

    public static ProductResDto FromEntity(Product product){
        return ProductResDto.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imagePath(product.getImagePath())
                .build();
    }


}
