package com.blibli.gdn.memberService.controller;

import com.blibli.gdn.memberService.dto.GdnResponseData;
import com.blibli.gdn.memberService.dto.MemberResponse;
import com.blibli.gdn.memberService.dto.UpdateMemberRequest;
import com.blibli.gdn.memberService.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "Member management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID", description = "Retrieve member details by ID")
    public ResponseEntity<GdnResponseData<MemberResponse>> getMemberById(@PathVariable UUID id) {
        log.info("Get member request: memberId={}", id);

        MemberResponse response = memberService.getMemberById(id);

        GdnResponseData<MemberResponse> gdnResponse = GdnResponseData.success(
                response,
                "Member retrieved successfully");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update member", description = "Update member details")
    public ResponseEntity<GdnResponseData<MemberResponse>> updateMember(
            @PathVariable @NotNull(message = "Member ID is required") UUID id,
            @Valid @RequestBody UpdateMemberRequest request) {

        log.info("Update member request: memberId={}", id);

        MemberResponse response = memberService.updateMember(id, request.getName());

        GdnResponseData<MemberResponse> gdnResponse = GdnResponseData.success(
                response,
                "Member updated successfully");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete member", description = "Delete member (admin only - authorization handled by gateway)")
    public ResponseEntity<GdnResponseData<String>> deleteMember(@PathVariable UUID id) {
        log.info("Delete member request: memberId={}", id);

        memberService.deleteMember(id);

        GdnResponseData<String> gdnResponse = GdnResponseData.success(
                "Member deleted successfully",
                "Delete successful");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }
}
