package com.beyond.ordersystem.member.dto.create;

import com.beyond.ordersystem.common.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embedded;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberResDto {
    private Long id;
    private String name;
    private String email;
//    private String password;
    @Embedded
    private Address address;
    private int orderCount;
}
