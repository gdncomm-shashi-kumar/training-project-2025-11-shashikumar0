package com.blibli.gdn.memberService.service;

import com.blibli.gdn.memberService.domain.Member;
import com.blibli.gdn.memberService.domain.Role;
import com.blibli.gdn.memberService.dto.MemberResponse;
import com.blibli.gdn.memberService.exception.MemberNotFoundException;
import com.blibli.gdn.memberService.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Unit Tests")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;
    private UUID testMemberId;

    @BeforeEach
    void setUp() {
        testMemberId = UUID.randomUUID();
        testMember = Member.builder()
                .memberId(testMemberId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should get member by ID successfully")
    void testGetMemberById_Success() {
        // Given
        when(memberRepository.findById(testMemberId)).thenReturn(Optional.of(testMember));

        // When
        MemberResponse response = memberService.getMemberById(testMemberId);

        // Then
        assertNotNull(response);
        assertEquals(testMemberId, response.getMemberId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals(Role.USER, response.getRole());
        verify(memberRepository, times(1)).findById(testMemberId);
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException when member not found")
    void testGetMemberById_NotFound() {
        // Given
        when(memberRepository.findById(testMemberId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.getMemberById(testMemberId);
        });
        verify(memberRepository, times(1)).findById(testMemberId);
    }

    @Test
    @DisplayName("Should update member successfully")
    void testUpdateMember_Success() {
        // Given
        String newName = "Updated Name";
        when(memberRepository.findById(testMemberId)).thenReturn(Optional.of(testMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setName(newName);
            return member;
        });

        // When
        MemberResponse response = memberService.updateMember(testMemberId, newName);

        // Then
        assertNotNull(response);
        assertEquals(newName, response.getName());
        assertEquals(testMemberId, response.getMemberId());
        verify(memberRepository, times(1)).findById(testMemberId);
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException when updating non-existent member")
    void testUpdateMember_NotFound() {
        // Given
        when(memberRepository.findById(testMemberId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.updateMember(testMemberId, "New Name");
        });
        verify(memberRepository, times(1)).findById(testMemberId);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should delete member successfully")
    void testDeleteMember_Success() {
        // Given
        when(memberRepository.findById(testMemberId)).thenReturn(Optional.of(testMember));
        doNothing().when(memberRepository).delete(any(Member.class));

        // When
        memberService.deleteMember(testMemberId);

        // Then
        verify(memberRepository, times(1)).findById(testMemberId);
        verify(memberRepository, times(1)).delete(testMember);
    }

    @Test
    @DisplayName("Should throw MemberNotFoundException when deleting non-existent member")
    void testDeleteMember_NotFound() {
        // Given
        when(memberRepository.findById(testMemberId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.deleteMember(testMemberId);
        });
        verify(memberRepository, times(1)).findById(testMemberId);
        verify(memberRepository, never()).delete(any(Member.class));
    }
}

