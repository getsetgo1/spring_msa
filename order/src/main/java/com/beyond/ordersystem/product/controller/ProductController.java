package com.beyond.ordersystem.product.controller;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductSaveReqDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController
public class ProductController {
    private final ProductService productService;
    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    // create
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/product/create")
    public ResponseEntity<?> productCreate(@ModelAttribute ProductSaveReqDto dto){
//        Product product = productService.productCreate(dto);
        Product product = productService.createAwsProduct(dto);

        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED,"succeffuly", product.getId());
        return new ResponseEntity<>(commonResDto,HttpStatus.CREATED);
//        Product product  = productService.productCreate(dto,imagePath);
    }

    @GetMapping("/product/list")
    //페이지네이션만을 위한 것
//    public ResponseEntity<?> productList(Pageable pageable){
    // 검색까지 추가
    public ResponseEntity<?> productList(ProductSearchDto dto, Pageable pageable){
        Page<ProductResDto> dtos=productService.productList(dto,pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK,"OK",dtos);
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }



}
