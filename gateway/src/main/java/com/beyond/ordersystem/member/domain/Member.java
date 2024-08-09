package com.beyond.ordersystem.member.domain;

import com.beyond.ordersystem.common.Address;
import com.beyond.ordersystem.common.domain.BaseTimeEntity;
import com.beyond.ordersystem.member.dto.create.MemberResDto;
import com.beyond.ordersystem.ordering.domain.Ordering;
import lombok.*;
import java.util.List;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    private String password;
    @Embedded // 공통적으로 삽입할수 있는 형태로 받는 걸 Embedded 어노테이션으로 선언
    private Address address;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role=Role.USER;

    @OneToMany(mappedBy = "member",fetch = FetchType.LAZY)
    private List<Ordering> orderList;

    public MemberResDto fromEntity(){
        return MemberResDto.builder()
                .id(this.id)
                .address(this.address)
                .email(this.email)
                .name(this.name)
                .orderCount(this.orderList.size())
                .build();
    }

    public void updatePassword(String password){
        this.password = password;
    }


}

