package com.blibli.gdn.memberService.service;

import com.blibli.gdn.memberService.config.CacheConfig;
import com.blibli.gdn.memberService.domain.Member;
import com.blibli.gdn.memberService.dto.*;
import com.blibli.gdn.memberService.exception.MemberNotFoundException;
import com.blibli.gdn.memberService.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.MEMBER_CACHE, key = "#memberId")
    public MemberResponse getMemberById(UUID memberId) {
        log.debug("Getting member by ID: memberId={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        return mapToResponse(member);
    }


    @Transactional
    @CacheEvict(value = CacheConfig.MEMBER_CACHE, key = "#memberId")
    public MemberResponse updateMember(UUID memberId, String name) {
        log.info("Updating member: memberId={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        member.setName(name);
        member = memberRepository.save(member);

        log.info("Member updated successfully: memberId={}", memberId);

        return mapToResponse(member);
    }


    @Transactional
    @CacheEvict(value = CacheConfig.MEMBER_CACHE, key = "#memberId")
    public void deleteMember(UUID memberId) {
        log.info("Deleting member: memberId={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        memberRepository.delete(member);

        log.info("Member deleted successfully: memberId={}", memberId);
    }


    private MemberResponse mapToResponse(Member member) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .name(member.getName())
                .role(member.getRole())
                .build();
    }
}
