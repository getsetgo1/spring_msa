package com.beyond.ordersystem.ordering.controller;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.dto.OrderSaveReqDto;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.ordering.service.OrderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {
    private final OrderingService orderingService;
    @Autowired
    public OrderController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }
    @PostMapping("/order/create")
    public ResponseEntity<?> orderCreate(@RequestBody List<OrderSaveReqDto> dto){
//        Ordering ordering = orderingService.orderRestTemplateCreate(dto);
        Ordering ordering = orderingService.orderFeignClientCreate(dto);

        //getId 안 하면 순환참조 걸림
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED,"정상완료",ordering.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/order/list")
    public ResponseEntity<?> orderList(){
        // 여기에 List<Ordering> 넣으면 순환참조 에러남!!!! 그래서 내가 헤매뮤ㅠ
        List<OrderListResDto> orderList = orderingService.orderList();
        CommonResDto commonResDto=new CommonResDto(HttpStatus.OK,"정상 완료", orderList);
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }
    //내 주문만 볼 수 있는 myOrders : order/myorders
    @GetMapping("/order/myorders")
    public ResponseEntity<?> myOrder(){
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK,"정상완료",orderingService.myOrders());
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

    // admin 사용자의 주문 취소 : /order/{id}/cancel -> orderstatus만 변경
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/order/{id}/cancel")
    public ResponseEntity<?> orderCancel(@PathVariable Long id){
        Ordering ordering = orderingService.orderCancel(id);
        //getId 안 하면 순환참조 걸림
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED,"정상 취소",ordering.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
    }






}
