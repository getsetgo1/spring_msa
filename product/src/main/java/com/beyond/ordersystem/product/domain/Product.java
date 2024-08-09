package com.beyond.ordersystem.product.domain;

import com.beyond.ordersystem.common.domain.BaseTimeEntity;
import com.beyond.ordersystem.product.dto.ProductResDto;
import lombok.*;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Product extends BaseTimeEntity {
    // id, name, category, price, stockQuantity, String imagePath
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private String imagePath;

    public void updateImagePath(String imagePath){
        this.imagePath = imagePath;
    }
    public void updateStockQuantity(int quantity){
        this.stockQuantity = this.stockQuantity - quantity;
    }

}
