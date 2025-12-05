package com.blibli.gdn.gateway.controller;

import com.blibli.gdn.gateway.dto.*;
import com.blibli.gdn.gateway.service.AuthenticationService;
import com.blibli.gdn.gateway.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtUtil jwtUtil;


    @PostMapping("/register")
    @Operation(summary = "Register new member", description = "Create a new member account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Member registered successfully",
                    content = @Content(schema = @Schema(implementation = GdnResponseData.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<GdnResponseData<MemberResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Register request received: email={}", request.getEmail());

        MemberResponse response = authenticationService.register(request);

        GdnResponseData<MemberResponse> gdnResponse = GdnResponseData.success(
                response,
                "Member registered successfully");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.status(HttpStatus.CREATED).body(gdnResponse);
    }


    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate and receive JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<GdnResponseData<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request received: email={}", request.getEmail());

        LoginResponse response = authenticationService.login(request);

        GdnResponseData<LoginResponse> gdnResponse = GdnResponseData.success(
                response,
                "Login successful");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }


    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<GdnResponseData<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Refresh token request received");

        LoginResponse response = authenticationService.refreshAccessToken(request);

        GdnResponseData<LoginResponse> gdnResponse = GdnResponseData.success(
                response,
                "Token refreshed successfully");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate both access and refresh tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<GdnResponseData<String>> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Logout request received");

        String accessToken = authHeader.replace("Bearer ", "");
        String refreshToken = request.getRefreshToken();

        authenticationService.logout(accessToken, refreshToken);

        GdnResponseData<String> gdnResponse = GdnResponseData.success(
                "Logged out successfully",
                "Logout successful");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }


    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiate password reset flow")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reset token generated"),
            @ApiResponse(responseCode = "404", description = "Member not found")
    })
    public ResponseEntity<GdnResponseData<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("Forgot password request received");

        String resetToken = authenticationService.forgotPassword(request);

        GdnResponseData<String> gdnResponse = GdnResponseData.success(
                resetToken,
                "Reset token generated successfully (check email)");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }


    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<GdnResponseData<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Reset password request received");

        authenticationService.resetPassword(request);

        GdnResponseData<String> gdnResponse = GdnResponseData.success(
                "Password reset successfully",
                "Password reset successfully");
        gdnResponse.setTraceId(MDC.get("traceId"));

        return ResponseEntity.ok(gdnResponse);
    }
}

