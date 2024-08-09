package com.beyond.ordersystem.ordering.domain;

import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.EnableMBeanExport;

import javax.persistence.*;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    @JoinColumn(name = "ordering_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Ordering ordering;

    private Long productId; // product에는 안걸어줘도 돼 product에서 안 쓸 거니까

    public OrderListResDto.OrderDetailDto fromEntity(){
        OrderListResDto.OrderDetailDto orderDetailDto = OrderListResDto.OrderDetailDto.builder()
                .id(this.id)
                .count(this.quantity)
                .build();
        return orderDetailDto;
    }

}
