package com.blibli.gdn.gateway.dto;

import com.blibli.gdn.gateway.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private UUID memberId;
    private String email;
    private String name;
    private Role role;
}

