package com.blibli.gdn.memberService.repository;

import com.blibli.gdn.memberService.domain.Member;
import com.blibli.gdn.memberService.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MemberRepository Integration Tests")
class MemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("Should save and find member by ID")
    void testSaveAndFindById() {
        // Given
        Member member = Member.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        Member saved = entityManager.persistAndFlush(member);
        Optional<Member> found = memberRepository.findById(saved.getMemberId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getMemberId(), found.get().getMemberId());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    @DisplayName("Should find member by email")
    void testFindByEmail() {
        // Given
        Member member = Member.builder()
                .email("unique@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(member);

        // When
        Optional<Member> found = memberRepository.findByEmail("unique@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("unique@example.com", found.get().getEmail());
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void testFindByEmail_NotFound() {
        // When
        Optional<Member> found = memberRepository.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should check if email exists")
    void testExistsByEmail() {
        // Given
        Member member = Member.builder()
                .email("exists@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entityManager.persistAndFlush(member);

        // When & Then
        assertTrue(memberRepository.existsByEmail("exists@example.com"));
        assertFalse(memberRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void testUniqueEmailConstraint() {
        // Given
        Member member1 = Member.builder()
                .email("duplicate@example.com")
                .passwordHash("hashedPassword1")
                .name("User 1")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Member member2 = Member.builder()
                .email("duplicate@example.com")
                .passwordHash("hashedPassword2")
                .name("User 2")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        entityManager.persistAndFlush(member1);

        // Then
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(member2);
        });
    }

    @Test
    @DisplayName("Should delete member")
    void testDelete() {
        // Given
        Member member = Member.builder()
                .email("todelete@example.com")
                .passwordHash("hashedPassword")
                .name("To Delete")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Member saved = entityManager.persistAndFlush(member);
        UUID memberId = saved.getMemberId();

        // When
        memberRepository.delete(saved);
        entityManager.flush();

        // Then
        Optional<Member> found = memberRepository.findById(memberId);
        assertFalse(found.isPresent());
    }
}

