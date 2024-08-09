package com.beyond.ordersystem.member.dto;


import com.beyond.ordersystem.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberListDto {
    private Long id;
    private String name;
    private String email;
    private String city;
    private String street;
    private String zipcode;

    public static MemberListDto ListMember(Member member){
        return MemberListDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
//                .city(member.getCity())
//                .street(member.getStreet())
//                .zipcode(member.getZipcode())
                .build();
    }
}
