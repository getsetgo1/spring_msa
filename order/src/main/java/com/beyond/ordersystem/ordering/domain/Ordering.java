package com.beyond.ordersystem.ordering.domain;

import com.beyond.ordersystem.common.domain.BaseTimeEntity;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Ordering extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus orderStatus=OrderStatus.ORDERED;

    // 만들때 같이 만들어주는 것도 cascade
    @OneToMany(mappedBy = "ordering", cascade = CascadeType.PERSIST)
    // 빌더 패턴에서도 ArrayList로 초기화되도록 하는 설정
    @Builder.Default
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public OrderListResDto fromEntity(){
        List<OrderDetail> orderDetailList = this.getOrderDetails();
        List<OrderListResDto.OrderDetailDto> orderDetailDtos = new ArrayList<>();

        for(OrderDetail orderDetail : orderDetailList){
            orderDetailDtos.add(orderDetail.fromEntity());
        }


        OrderListResDto orderListResDto = OrderListResDto.builder()
                .id(this.id)
                .memberEmail(this.memberEmail)
                .orderStatus(this.orderStatus)
                .orderDetailDtos(orderDetailDtos)
                .build();
        return orderListResDto;
    }

    public void updateOrderStatus(OrderStatus orderStatus){
        this.orderStatus = orderStatus;
    }
}
