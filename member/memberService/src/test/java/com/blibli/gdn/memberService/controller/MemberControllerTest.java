package com.blibli.gdn.memberService.controller;

import com.blibli.gdn.memberService.domain.Role;
import com.blibli.gdn.memberService.dto.MemberResponse;
import com.blibli.gdn.memberService.dto.UpdateMemberRequest;
import com.blibli.gdn.memberService.exception.MemberNotFoundException;
import com.blibli.gdn.memberService.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@DisplayName("MemberController Integration Tests")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testMemberId;
    private MemberResponse testMemberResponse;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        testMemberId = UUID.randomUUID();
        testMemberResponse = MemberResponse.builder()
                .memberId(testMemberId)
                .email("test@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Should get member by ID successfully")
    void testGetMemberById_Success() throws Exception {
        // Given
        when(memberService.getMemberById(testMemberId)).thenReturn(testMemberResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/members/{id}", testMemberId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(testMemberId.toString()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.message").value("Member retrieved successfully"));

        verify(memberService, times(1)).getMemberById(testMemberId);
    }

    @Test
    @DisplayName("Should return 404 when member not found")
    void testGetMemberById_NotFound() throws Exception {
        // Given
        when(memberService.getMemberById(testMemberId))
                .thenThrow(new MemberNotFoundException("Member not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/members/{id}", testMemberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Member not found"));

        verify(memberService, times(1)).getMemberById(testMemberId);
    }

    @Test
    @DisplayName("Should update member successfully")
    void testUpdateMember_Success() throws Exception {
        // Given
        UpdateMemberRequest request = UpdateMemberRequest.builder()
                .name("Updated Name")
                .build();
        MemberResponse updatedResponse = MemberResponse.builder()
                .memberId(testMemberId)
                .email("test@example.com")
                .name("Updated Name")
                .role(Role.USER)
                .build();

        when(memberService.updateMember(eq(testMemberId), eq("Updated Name")))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/members/{id}", testMemberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.message").value("Member updated successfully"));

        verify(memberService, times(1)).updateMember(eq(testMemberId), eq("Updated Name"));
    }

    @Test
    @DisplayName("Should return 400 when update request is invalid")
    void testUpdateMember_InvalidRequest() throws Exception {
        // Given
        UpdateMemberRequest request = UpdateMemberRequest.builder()
                .name("") // Invalid: empty name
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/members/{id}", testMemberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).updateMember(any(), any());
    }

    @Test
    @DisplayName("Should delete member successfully")
    void testDeleteMember_Success() throws Exception {
        // Given
        doNothing().when(memberService).deleteMember(testMemberId);

        // When & Then
        mockMvc.perform(delete("/api/v1/members/{id}", testMemberId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Member deleted successfully"))
                .andExpect(jsonPath("$.message").value("Delete successful"));

        verify(memberService, times(1)).deleteMember(testMemberId);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent member")
    void testDeleteMember_NotFound() throws Exception {
        // Given
        doThrow(new MemberNotFoundException("Member not found"))
                .when(memberService).deleteMember(testMemberId);

        // When & Then
        mockMvc.perform(delete("/api/v1/members/{id}", testMemberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Member not found"));

        verify(memberService, times(1)).deleteMember(testMemberId);
    }
}

