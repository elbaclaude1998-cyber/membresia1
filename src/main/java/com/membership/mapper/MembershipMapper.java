package com.membership.mapper;

import com.membership.domain.Membership;
import com.membership.domain.Payment;
import com.membership.dto.MembershipDtos.MembershipResponse;
import com.membership.dto.MembershipDtos.PaymentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MembershipMapper {

    MembershipResponse toResponse(Membership membership);

    PaymentResponse toResponse(Payment payment);
}
