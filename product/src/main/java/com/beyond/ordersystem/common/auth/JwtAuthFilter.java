package com.beyond.ordersystem.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Component
public class JwtAuthFilter extends GenericFilter {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String bearertoken = ((HttpServletRequest)request).getHeader("Authorization");
        try{
            if(bearertoken != null){
                // token은 관례적으로 Bearer로 시작하는 문구를 넣어서 요청
                if(!bearertoken.substring(0,7).equals("Bearer ")){
                    throw new AuthenticationServiceException("Bearer 형식이 아닙니다");
                }
                String token = bearertoken.substring(7);
                // token 검증 및 claims(사용자 정보) 추출
                // token 생성 시에 사용한 secret키값을 넣어 토큰 검증에 사용
                // getbody 해서 페이로드 정보 가져오는 것
                // 이 아래 한 줄이 검증코드임!
                Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
                // Authentication 객체 생성(UserDetails 객체도 필요)
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_"+claims.get("role")));
                UserDetails userDetails = new User(claims.getSubject(),"",authorities);
                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,"",userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }

            // filterchain에서 그 다음 filtering으로 넘어가도록 하는 메서드
            chain.doFilter(request,response);
        }
        catch(Exception e){
            log.error(e.getMessage());
            // spring 앞단계여서 sout하는 거 못하니 httpservletresponse 객체 사용해야돼
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType("application/json");
            httpServletResponse.getWriter().write("token 에러");
        }
    }
}
