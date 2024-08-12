package com.beyond.ordersystem.common.configs;

import feign.RequestInterceptor;
import org.apache.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor  requestInterceptor(){
        return request->{
            // 모든 feign 요청에 전역적으로 token 세팅하는 것
            String token = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
            request.header(HttpHeaders.AUTHORIZATION,token);
        };
    }
}
