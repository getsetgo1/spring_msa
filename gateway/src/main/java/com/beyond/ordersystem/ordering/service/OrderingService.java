package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.controller.SseController;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.dto.OrderSaveReqDto;
//import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.dto.StockDecreaseEvent;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final StockInventoryService stockInventoryService;
    private final StockDecreaseEventHandler stockDecreaseEventHandler;
    private final SseController sseController;
    //    private final OrderDetailRepository orderDetailRepository;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository, ProductRepository productRepository, StockInventoryService stockInventoryService, StockDecreaseEventHandler stockDecreaseEventHandler, SseController sseController) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
//        this.orderDetailRepository = orderDetailRepository;
        this.stockInventoryService = stockInventoryService;
        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
        this.sseController = sseController;
    }

    //        방법1.쉬운방식
//        Ordering생성 : member_id, status
//    public Ordering orderCreate(OrderSaveReqDto dto) {
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(() -> new EntityNotFoundException("없음"));
//        Ordering ordering = orderingRepository.save(dto.toEntity(member));
//
////        OrderDetail생성 : order_id, product_id, quantity
//        for (OrderSaveReqDto.OrderDto orderDto : dto.getOrderDtos()) {
//            Product product = productRepository.findById(orderDto.getProductId()).orElse(null);
//            int quantity = orderDto.getProductCount();
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//            orderDetailRepository.save(orderDetail);
//        }
//        return ordering;

        // 방법2 : orderdetail repository 없게 만드는 것
    // JPA에 최적화된 방식
    // syncronized 설정한다 하더라도, 재고 감소가 DB에 반영되는 시점은 트랜잭션이 커밋되고 종료되는 시점
    public Ordering orderCreate(List<OrderSaveReqDto> dtos) {
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        // authentication 객체로 member 찾아오는 것 적용
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()->new EntityNotFoundException("해당 회원이 없습니다"));
        Ordering ordering = Ordering.builder().member(member).build();
        for (OrderSaveReqDto orderDto : dtos) {
            Product product = productRepository.findById(orderDto.getProductId()).orElse(null);
            int quantity = orderDto.getProductCount();
            if(product.getName().contains("sale")){
            // redis를 통한 재고관리 및 재고 잔량 확인
                int newQuantity = stockInventoryService.decreaseStock(orderDto.getProductId(), orderDto.getProductCount()).intValue();
                if(newQuantity<0) throw new IllegalArgumentException("재고 부족");
                // rdb에 재고를 업데이트. rabiitmq를 통해 비동기적으로 이벤트 처리
                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(),orderDto.getProductCount()));
            }else{
                if(product.getStockQuantity() < quantity){
                    throw new IllegalArgumentException("재고 부족");
                }
                product.updateStockQuantity(quantity); // 더티체킹(변경 감지)로 인해 별도의 save 불필요 -- 추가면 모르겠는데 update된 거는 spring이 알아서 해줌
            }

            // 여기까지ㅣㅣㅣㅣ
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(quantity)
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetails().add(orderDetail);
//            orderingRepository.save(ordering);
        }

        Ordering savedOrdering = orderingRepository.save(ordering);

        //sse 알림 send
        sseController.publicMessage(savedOrdering.fromEntity(),"admin@test.com");

        return savedOrdering;



//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("없는 회원입니다."));
//
//        Ordering ordering = Ordering.builder()
//                .member(member)
//                .build();
//
//        for(OrderSaveReqDto.OrderDto orderDetailDto: dto.getOrderDtos()){
//            Product product = productRepository.findById(orderDetailDto.getProductId()).orElseThrow(()->new EntityNotFoundException("없는 상품입니다."));
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .ordering(ordering)
//                    .product(product)
//                    .quantity(orderDetailDto.getProductCount())
//                    .build();
//            ordering.getOrderDetails().add(orderDetail); // order의 list에 add 하는 것!!!!!
//        }
//
//
//        // orderdetail이랑 order는 서로 필요로함
//        Ordering savedOrdering = orderingRepository.save(ordering);
//        // toentity로 하기에는 너무 복잡해서 service에서 조립
    }
    public List<OrderListResDto> orderList(){
        List<Ordering> orderings =orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos=new ArrayList<>();
        for(Ordering ordering : orderings){
            orderListResDtos.add(ordering.fromEntity());
        }
        return orderListResDtos;
    }

    public List<OrderListResDto> myOrders(){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()->new EntityNotFoundException("해당 회원이 없습니다"));
        List<Ordering> orderings = orderingRepository.findByMember(member);
        List<OrderListResDto> orderList = new ArrayList<>();
        for(Ordering ordering : orderings) orderList.add(ordering.fromEntity());
        return orderList;
    }

    public Ordering orderCancel(Long orderId){

        Ordering findOrder = orderingRepository.findById(orderId).orElseThrow(()->new EntityNotFoundException("해당 주문 정보가 없습니다."));
        findOrder.updateOrderStatus(OrderStatus.CANCELED);
        return findOrder;
    }



}

