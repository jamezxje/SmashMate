package com.smashmate.mapper;

import com.smashmate.dto.response.MemberResponse;
import com.smashmate.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "hasAccount",
             expression = "java(member.getEmail() != null && member.getPassword() != null)")
    MemberResponse toResponse(Member member);
}
