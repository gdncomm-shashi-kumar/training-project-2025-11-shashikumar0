package com.blibli.gdn.gateway.service;

import com.blibli.gdn.gateway.domain.Member;
import com.blibli.gdn.gateway.domain.Role;
import com.blibli.gdn.gateway.dto.*;
import com.blibli.gdn.gateway.exception.*;
import com.blibli.gdn.gateway.repository.MemberRepository;
import com.blibli.gdn.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenDenylistService tokenDenylistService;


    @Transactional
    public MemberResponse register(RegisterRequest request) {
        log.info("Registering new member: email={}", request.getEmail());

        if (memberRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists: {}", request.getEmail());
            throw new MemberAlreadyExistsException("Email already registered");
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Role.USER)
                .build();

        member = memberRepository.save(member);

        log.info("Member registered successfully: memberId={}, email={}",
                member.getMemberId(), member.getEmail());

        return mapToResponse(member);
    }


    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: email={}", request.getEmail());

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: member not found: {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            log.warn("Login failed: invalid password for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(
                member.getMemberId(),
                member.getEmail(),
                member.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(member.getMemberId());


        log.info("Login successful: memberId={}, email={}",
                member.getMemberId(), member.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationInSeconds())
                .build();
    }


    @Transactional(readOnly = true)
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        log.info("Refreshing access token");

        String refreshToken = request.getRefreshToken();

        if (tokenDenylistService.isTokenDenied(refreshToken)) {
            log.warn("Refresh token is in denylist (user logged out)");
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        UUID memberId;
        try {
            memberId = jwtUtil.extractMemberId(refreshToken);
            Claims claims = jwtUtil.parseToken(refreshToken);

            if (!jwtUtil.isRefreshToken(claims)) {
                throw new InvalidTokenException("Token is not a refresh token");
            }

            if (jwtUtil.isTokenExpired(claims)) {
                throw new InvalidTokenException("Refresh token expired");
            }
        } catch (Exception e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));


        String accessToken = jwtUtil.generateAccessToken(
                member.getMemberId(),
                member.getEmail(),
                member.getRole());

        log.info("Access token refreshed successfully: memberId={}", memberId);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpirationInSeconds())
                .build();
    }


    @Transactional(readOnly = true)
    public void logout(String accessToken, String refreshToken) {
        log.info("Logout request");

        try {
            UUID memberId = jwtUtil.extractMemberId(accessToken);

            tokenDenylistService.denyBothTokens(accessToken, refreshToken);

            log.info("Logout successful: memberId={}, both tokens denied", memberId);
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request: email={}", request.getEmail());

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        String resetToken = UUID.randomUUID().toString();
        member.setResetToken(resetToken);
        member.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        memberRepository.save(member);

        log.info("Reset token generated for member: {}", member.getMemberId());

        return resetToken;
    }


    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request");

        Member member = memberRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        if (member.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset token expired");
        }

        member.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        member.setResetToken(null);
        member.setResetTokenExpiry(null);
        memberRepository.save(member);

        log.info("Password reset successfully for member: {}", member.getMemberId());
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

