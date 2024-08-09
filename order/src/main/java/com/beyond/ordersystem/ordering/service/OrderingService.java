package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.ordering.controller.SseController;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.dto.OrderSaveReqDto;
//import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.dto.StockDecreaseEvent;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
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
    private final StockInventoryService stockInventoryService;
    private final StockDecreaseEventHandler stockDecreaseEventHandler;
    private final SseController sseController;
//    private final OrderDetailRepository orderDetailRepository;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, StockInventoryService stockInventoryService, StockDecreaseEventHandler stockDecreaseEventHandler, SseController sseController) {
        this.orderingRepository = orderingRepository;
        this.stockInventoryService = stockInventoryService;
        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
        this.sseController = sseController;
    }

    //        방법1.쉬운방식
//        Ordering생성 : member_id, status
    public Ordering orderCreate(List<OrderSaveReqDto> dtos){
//        방법2. JPA에 최적화된 방식
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("해당 ID가 존재하지 않습니다."));

//        아래꺼 그냥 외우면 됨. -> 이해 하구^^
//        토큰 사용할때 간결하게 사용할 수 있는
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // orderStatus는 초기화했고, orderDetail은 없다고 가정 (아래서 add하는 방식 사용하기 위해)
        // 즉, member만 builder에 넣어주면 됨 => 이렇게 ordering 객체 생성
        Ordering ordering = Ordering.builder()
                .memberEmail(memberEmail)
                //.orderDetails()
                .build();

//        for(OrderSaveReqDto orderCreateReqDto : dtos){
//            System.out.println(orderCreateReqDto);
//            int quantity = orderCreateReqDto.getProductCnt();
////            Product API에 요청을 통해 product객체를 조회해야함.
//
//            if(quantity < 1){
//                throw new IllegalArgumentException("구매 수량은 1개 이상만 가능합니다");
//            }
//            // 구매 가능한지 재고 비교
//            if (product.getName().contains("sale")) { // sale인 상품일 때만 redis를 통해 재고관리
//                // 동시성 해결 => redis를 통한 재고관리 및 재고 잔량 확인
//                int newQuantity = stockInventoryService.decreaseStock(orderCreateReqDto.getProductId(), orderCreateReqDto.getProductCnt()).intValue();
//                // 여기서 분기처리 ㄱㄱ
//                if (newQuantity < 0) { // 재고가 더 부족할 때 -1L 반환한거
//                    throw new IllegalArgumentException("재고 부족");
//                }
//                // rdb에 재고 업데이트 (product 테이블에 업데이트) => 이전까진 100개수량에서 마이너스가 안되고 있었음
//                // rabbitmq를 통해 비동기적으로 이벤트 처리
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(), orderCreateReqDto.getProductCnt()));
//
//            } else {
//                if (product.getStockQuantity() < quantity) {
//                    throw new IllegalArgumentException("재고 부족");
//                }
//                log.info("재고 확인 (전) : " + product.getStockQuantity());
//                product.updateStockQuantity(quantity);
//                log.info("재고 확인 (후) : " + product.getStockQuantity());
//
//            }
//            // 구매 가능하면 진행
//            OrderDetail orderDetail =  OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    // 아직 save가 안됐는데 어떻게 이 위의 ordering이 들어가나? => jpa가 알 아 서 해줌⭐
//                    .ordering(ordering)
//                    .build();
//            ordering.getOrderDetails().add(orderDetail);
//        }
        Ordering savedOrdering = orderingRepository.save(ordering);

//        보내려 하는 정보와, 보내고자 하는 사람의 이메일을 보낸다.
        sseController.publicMessage(savedOrdering.fromEntity(), "admin@test.com");
        return savedOrdering;
    }

        // 방법2 : orderdetail repository 없게 만드는 것
    // JPA에 최적화된 방식
    // syncronized 설정한다 하더라도, 재고 감소가 DB에 반영되는 시점은 트랜잭션이 커밋되고 종료되는 시점
//    public Ordering orderCreate(List<OrderSaveReqDto> dtos) {
////        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
//        // authentication 객체로 member 찾아오는 것 적용
//        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()->new EntityNotFoundException("해당 회원이 없습니다"));
//        Ordering ordering = Ordering.builder().member(member).build();
//        for (OrderSaveReqDto orderDto : dtos) {
//            Product product = productRepository.findById(orderDto.getProductId()).orElse(null);
//            int quantity = orderDto.getProductCount();
//            if(product.getName().contains("sale")){
//            // redis를 통한 재고관리 및 재고 잔량 확인
//                int newQuantity = stockInventoryService.decreaseStock(orderDto.getProductId(), orderDto.getProductCount()).intValue();
//                if(newQuantity<0) throw new IllegalArgumentException("재고 부족");
//                // rdb에 재고를 업데이트. rabiitmq를 통해 비동기적으로 이벤트 처리
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(),orderDto.getProductCount()));
//            }else{
//                if(product.getStockQuantity() < quantity){
//                    throw new IllegalArgumentException("재고 부족");
//                }
//                product.updateStockQuantity(quantity); // 더티체킹(변경 감지)로 인해 별도의 save 불필요 -- 추가면 모르겠는데 update된 거는 spring이 알아서 해줌
//            }
//
//            // 여기까지ㅣㅣㅣㅣ
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//            ordering.getOrderDetails().add(orderDetail);
////            orderingRepository.save(ordering);
//        }
//
//        Ordering savedOrdering = orderingRepository.save(ordering);
//
//        //sse 알림 send
//        sseController.publicMessage(savedOrdering.fromEntity(),"admin@test.com");
//
//        return savedOrdering;
//
//
//
////        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("없는 회원입니다."));
////
////        Ordering ordering = Ordering.builder()
////                .member(member)
////                .build();
////
////        for(OrderSaveReqDto.OrderDto orderDetailDto: dto.getOrderDtos()){
////            Product product = productRepository.findById(orderDetailDto.getProductId()).orElseThrow(()->new EntityNotFoundException("없는 상품입니다."));
////            OrderDetail orderDetail = OrderDetail.builder()
////                    .ordering(ordering)
////                    .product(product)
////                    .quantity(orderDetailDto.getProductCount())
////                    .build();
////            ordering.getOrderDetails().add(orderDetail); // order의 list에 add 하는 것!!!!!
////        }
////
////
////        // orderdetail이랑 order는 서로 필요로함
////        Ordering savedOrdering = orderingRepository.save(ordering);
////        // toentity로 하기에는 너무 복잡해서 service에서 조립
//    }
    public List<OrderListResDto> orderList(){
        List<Ordering> orderings =orderingRepository.findAll();
        List<OrderListResDto> orderListResDtos=new ArrayList<>();
        for(Ordering ordering : orderings){
            orderListResDtos.add(ordering.fromEntity());
        }
        return orderListResDtos;
    }

    public List<OrderListResDto> myOrders(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Ordering> orderings = orderingRepository.findByMemberEmail(email);
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

