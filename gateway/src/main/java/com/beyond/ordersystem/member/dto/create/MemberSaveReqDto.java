package com.beyond.ordersystem.member.dto.create;

import com.beyond.ordersystem.common.Address;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

// build gradle에 추가한 starter.validation으로 dto 에러 처리
// NotEmpty,Size

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberSaveReqDto {
    private String name;
    @NotEmpty(message ="email is essential")
    private String email;
    @NotEmpty(message ="password is essential")
    @Size(min = 8, message = "password minimum length is 8")
    private String password;
    private Address address;
    private Role role=Role.USER;


    public Member toEntity(String password){
        return Member.builder()
                .name(this.name)
                .email(this.email)
                .password(password)
                .role(this.role)
                .address(this.address)
                .build();
    }
}
