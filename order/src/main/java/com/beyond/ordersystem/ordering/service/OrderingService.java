package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.ordering.controller.SseController;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.*;
//import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    private final ProductFeign productFeign;
//    private final OrderDetailRepository orderDetailRepository;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, StockInventoryService stockInventoryService, StockDecreaseEventHandler stockDecreaseEventHandler, SseController sseController, RestTemplate restTemplate, ProductFeign productFeign) {
        this.orderingRepository = orderingRepository;
        this.stockInventoryService = stockInventoryService;
        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
        this.sseController = sseController;
        this.restTemplate = restTemplate;
        this.productFeign = productFeign;
    }

    //        방법1.쉬운방식
//        Ordering생성 : member_id, status
    public Ordering orderRestTemplateCreate(List<OrderSaveReqDto> dtos){
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

        for(OrderSaveReqDto orderCreateReqDto : dtos){
            System.out.println(orderCreateReqDto);
            int quantity = orderCreateReqDto.getProductCount();
//            Product API에 요청을 통해 product객체를 조회해야함.

            if(quantity < 1){
                throw new IllegalArgumentException("구매 수량은 1개 이상만 가능합니다");
            }
            // Product API에 요청을 통해 prouct객체를 조회해야함
            String productGetUrl = "http://product-service/product/"+ orderCreateReqDto.getProductId();
            HttpHeaders httpHeaders = new HttpHeaders();
            String token=(String)SecurityContextHolder.getContext().getAuthentication().getCredentials();
            httpHeaders.set("Authorization",token);
            HttpEntity<String> entity=new HttpEntity<>(httpHeaders);
            // restemplate return 형식은 무조건 response entity이다
            // 근데 왜 commonresdto 쓰냐면 우리가 commonresdto로 줄 거기 때문에 저렇게 받는 것 왜냐면 이전에 코드들이 controller에서 commonresdto로 줬기 때문
            ResponseEntity<CommonResDto> productEntity = restTemplate.exchange(productGetUrl, HttpMethod.GET, entity, CommonResDto.class);
            ObjectMapper objectMapper = new ObjectMapper();

            ProductDto productDto=objectMapper.convertValue(productEntity.getBody().getResult(),ProductDto.class); // productEntity가 json으로 들어오기 때문에 parsing 필요
            System.out.println("productDto:"+productDto);
            if (productDto.getName().contains("sale")) { // sale인 상품일 때만 redis를 통해 재고관리
                // 동시성 해결 => redis를 통한 재고관리 및 재고 잔량 확인
                int newQuantity = stockInventoryService.decreaseStock(orderCreateReqDto.getProductId(), orderCreateReqDto.getProductCount()).intValue();
                // 여기서 분기처리 ㄱㄱ
                if (newQuantity < 0) { // 재고가 더 부족할 때 -1L 반환한거
                    throw new IllegalArgumentException("재고 부족");
                }
                // rdb에 재고 업데이트 (product 테이블에 업데이트) => 이전까진 100개수량에서 마이너스가 안되고 있었음
                // rabbitmq를 통해 비동기적으로 이벤트 처리
                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), orderCreateReqDto.getProductCount()));

            } else {
                if (productDto.getStockQuantity() < quantity) {
                    throw new IllegalArgumentException("재고 부족");
                }
                // restTempalte을 통한 update 요청 필요
//                product.updateStockQuantity(quantity);
                String updateUrl = "http://product-service/product/updatestock";
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<ProductUpdatStockDto> updateEntity = new HttpEntity<>(new ProductUpdatStockDto(orderCreateReqDto.getProductId(), orderCreateReqDto.getProductCount()),httpHeaders);

                restTemplate.exchange(updateUrl,HttpMethod.PUT,updateEntity,Void.class);

            }
            // 구매 가능하면 진행
            OrderDetail orderDetail =  OrderDetail.builder()
                    .productId(productDto.getId())
                    .quantity(quantity)
                    // 아직 save가 안됐는데 어떻게 이 위의 ordering이 들어가나? => jpa가 알 아 서 해줌⭐
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetails().add(orderDetail);
        }
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


    public Ordering orderFeignClientCreate(List<OrderSaveReqDto> dtos){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Ordering ordering = Ordering.builder()
                .memberEmail(memberEmail)
                .build();

        for(OrderSaveReqDto orderCreateReqDto : dtos){
            System.out.println(orderCreateReqDto);
            int quantity = orderCreateReqDto.getProductCount();

            if(quantity < 1){
                throw new IllegalArgumentException("구매 수량은 1개 이상만 가능합니다");
            }

            //==feign이 훨배 좋음
//            String productGetUrl = "http://product-service/product/"+ orderCreateReqDto.getProductId();
//            HttpHeaders httpHeaders = new HttpHeaders();
//            String token=(String)SecurityContextHolder.getContext().getAuthentication().getCredentials();
//            httpHeaders.set("Authorization",token);
//            HttpEntity<String> entity=new HttpEntity<>(httpHeaders);
//            ResponseEntity<CommonResDto> productEntity = restTemplate.exchange(productGetUrl, HttpMethod.GET, entity, CommonResDto.class);
//            ObjectMapper objectMapper = new ObjectMapper();
//
//            ProductDto productDto=objectMapper.convertValue(productEntity.getBody().getResult(),ProductDto.class); // productEntity가 json으로 들어오기 때문에 parsing 필요


            // ResponseEntity가 기본 응답값이므로 바로 CommonResDto로 매핑
            CommonResDto commonResDto=productFeign.getProductById(orderCreateReqDto.getProductId());
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDto productDto = objectMapper.convertValue(commonResDto.getResult(), ProductDto.class);


            System.out.println("productDto:"+productDto);
            if (productDto.getName().contains("sale")) {
                int newQuantity = stockInventoryService.decreaseStock(orderCreateReqDto.getProductId(), orderCreateReqDto.getProductCount()).intValue();
                if (newQuantity < 0) {
                    throw new IllegalArgumentException("재고 부족");
                }
                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), orderCreateReqDto.getProductCount()));

            } else {
                if (productDto.getStockQuantity() < quantity) {
                    throw new IllegalArgumentException("재고 부족");
                }
                //==restTemplate 사용하는 것..! feign이 훨배 좋아
//                String updateUrl = "http://product-service/product/updatestock";
//                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//                HttpEntity<ProductUpdatStockDto> updateEntity = new HttpEntity<>(new ProductUpdatStockDto(orderCreateReqDto.getProductId(), orderCreateReqDto.getProductCount()),httpHeaders);
//
//                restTemplate.exchange(updateUrl,HttpMethod.PUT,updateEntity,Void.class);

                productFeign.updateProductStock(new ProductUpdatStockDto(orderCreateReqDto.getProductId(), orderCreateReqDto.getProductCount()));

            }
            OrderDetail orderDetail =  OrderDetail.builder()
                    .productId(productDto.getId())
                    .quantity(quantity)
                    .ordering(ordering)
                    .build();
            ordering.getOrderDetails().add(orderDetail);
        }
        Ordering savedOrdering = orderingRepository.save(ordering);

        sseController.publicMessage(savedOrdering.fromEntity(), "admin@test.com");
        return savedOrdering;
    }




//    public Ordering orderFeignKafkaCreate(List<OrderSaveReqDto> dtos){}


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

