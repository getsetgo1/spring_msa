package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.configs.FeignConfig;
import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.ordering.dto.ProductUpdatStockDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="product-service", configuration = FeignConfig.class)
public interface ProductFeign {
    @GetMapping(value="product/{id}")
    CommonResDto getProductById(@PathVariable("id") Long id);
    @PutMapping(value="/product/updatestock")
    void updateProductStock(@RequestBody ProductUpdatStockDto dto);
}
