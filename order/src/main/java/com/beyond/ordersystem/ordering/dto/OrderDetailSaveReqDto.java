//package com.beyond.ordersystem.ordering.dto;
//
//import com.beyond.ordersystem.ordering.domain.OrderDetail;
//import com.beyond.ordersystem.product.domain.Product;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@AllArgsConstructor
//@NoArgsConstructor
//@Data
//@Builder
//public class OrderDetailSaveReqDto {
//    private Product product;
//    private Integer productCount;
////    private Long orderingId;
//
//    public OrderDetail toEntity(){
//        return OrderDetail.builder()
//                .product(this.product)
//                .quantity(this.productCount)
////                .orderingId(this.orderingId)
//                .build();
//    }
//}
