package com.beyond.ordersystem.member.controller;

import com.beyond.ordersystem.common.dto.CommonErrorDto;
import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.MemberListDto;
import com.beyond.ordersystem.member.dto.MemberLoginDto;
import com.beyond.ordersystem.member.dto.MemberRefreshDto;
import com.beyond.ordersystem.member.dto.ResetPasswordDto;
import com.beyond.ordersystem.member.dto.create.MemberResDto;
import com.beyond.ordersystem.member.dto.create.MemberSaveReqDto;
import com.beyond.ordersystem.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.beyond.ordersystem.common.auth.JwtTokenProvider;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class MemberController {
    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    @Autowired
    public MemberController(MemberService memberService, JwtTokenProvider jwtTokenProvider, @Qualifier("2") RedisTemplate<String, Object> redisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberService = memberService;
        this.redisTemplate = redisTemplate;
    }
    @PostMapping("member/create")
    // ? = object
    // restapi로 갈거니 @RequestBody로 받아야함
    @Valid
    public ResponseEntity<?> createMember(@RequestBody @Valid MemberSaveReqDto dto){
        Member member = memberService.memberCreate(dto);
        // 아래에서 member.getId() 말고 member 넘기면 순환참조 이슈 있을 수 있다.
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED,"member is created",member.getId());
        return new ResponseEntity<>(commonResDto,HttpStatus.CREATED);
    }

    // 어드민만 회원 목록 전체 조회 가능



    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/member/list")
    public ResponseEntity<?> memberList(Pageable pageable){
        Page<MemberResDto> dtos = memberService.memberList(pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "members are found", dtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    // 본인은 본인 회원 정보만 조회 가능
    // /member/myinfo  -> MemberResDto, 이젠 매개변수 필요 없음!!!
    @GetMapping("/member/myInfo")
    public ResponseEntity<?> myInfo(){
        CommonResDto commonResDto= new CommonResDto(HttpStatus.OK,"member info success",memberService.myInfo()) ;
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginDto dto){
        //email, password가 일치한지 검증
        Member member = memberService.login(dto);

        // 일치할 경우 accessToken 생성
        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString());

        // refreshToken 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().toString());

        // redis에 email과 rt를 key:value로 하여 저장
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken,240, TimeUnit.HOURS); // 240시간

        // 생성된 토큰을 CommonResDto에 담아 사용자에게 return
        Map<String,Object> loginInfo = new HashMap<>();
        loginInfo.put("id",member.getEmail());
        loginInfo.put("token",jwtToken);
        loginInfo.put("refreshToken", refreshToken);

        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "login is successful",loginInfo);
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

    @PostMapping("/refresh-token")

    public ResponseEntity<?> generateNewAccessToken(@RequestBody MemberRefreshDto dto){
        String rt = dto.getRefreshToken();
        Claims claims =null;
        try {
            // 코드를 통해 rt 검증
            claims = Jwts.parser().setSigningKey(secretKeyRt).parseClaimsJws(rt).getBody();
        } catch (Exception e){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(),"invalide refresh token"),HttpStatus.BAD_REQUEST);
        }

        // accesstoken 새로 만드려고 꺼내는 것
        String email = claims.getSubject(); // 이메일 꺼내기
        String role = claims.get("role").toString();

        // redis를 조회하여 rt 추가 검증
        Object obj = redisTemplate.opsForValue().get(email);
        if(obj==null||!obj.toString().equals(rt)){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(),"invalide refresh token"),HttpStatus.BAD_REQUEST);
        }

        String newAt = jwtTokenProvider.createToken(email,role);
        // 생성된 토큰을 CommonResDto에 담아 사용자에게 return
        Map<String,Object> info = new HashMap<>();
        info.put("token",newAt);

        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "AccessToken is renewed",info);
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

    @PatchMapping("/member/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto dto){
        memberService.resetPassword(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK,"password is renewed","ok");
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }
}


