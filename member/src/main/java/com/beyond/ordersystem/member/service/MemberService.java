package com.beyond.ordersystem.member.service;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.domain.Role;
import com.beyond.ordersystem.member.dto.MemberListDto;
import com.beyond.ordersystem.member.dto.MemberLoginDto;
import com.beyond.ordersystem.member.dto.ResetPasswordDto;
import com.beyond.ordersystem.member.dto.create.MemberResDto;
import com.beyond.ordersystem.member.dto.create.MemberSaveReqDto;
import com.beyond.ordersystem.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
@Transactional
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // create
    public Member memberCreate(MemberSaveReqDto dto){
        if(memberRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        if(dto.getPassword().length()<8)  throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        return memberRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
//        return memberRepository.save(dto.toEntity(dto.getPassword()));

    }

    // list
    public Page<MemberResDto> memberList(Pageable pageable){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("해당 회원이 없습니다"));
        if(admin.getRole() != Role.ADMIN) throw new IllegalArgumentException("어드민만 memberlist 조회 가능합니다");
        Page<Member> members = memberRepository.findAll(pageable);
        return members.map(a->a.fromEntity());
    }

    public Member login(MemberLoginDto dto){
        // email이 있는지 존재 여부 체크
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(()->new EntityNotFoundException("존재하지 않은 이메일입니다."));
        // password 일치 여부(들어온 비번을 encode한 후 비교)
        // matches가 암호화해서 알아서 비교해줌
        if(!passwordEncoder.matches(dto.getPassword(),member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return member;
    }

    public MemberResDto myInfo(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("해당 회원이 없습니다"));
        return member.fromEntity();
    }

    public void resetPassword(ResetPasswordDto dto){
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(()->new EntityNotFoundException("존재하지 않은 이메일입니다."));
        if(!passwordEncoder.matches(dto.getAsIsPassword(),member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        member.updatePassword(passwordEncoder.encode(dto.getToBePassword()));
    }

}
