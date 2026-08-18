package com.smashmate.controller;

import com.smashmate.common.ApiResponse;
import com.smashmate.dto.request.ChangePasswordRequest;
import com.smashmate.dto.request.LoginRequest;
import com.smashmate.dto.response.TokenResponse;
import com.smashmate.security.JwtProperties;
import com.smashmate.security.JwtService;
import com.smashmate.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final MemberService memberService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        String accessToken = jwtService.generateAccessToken(req.getEmail(), role);
        String refreshToken = jwtService.generateRefreshToken(req.getEmail(), role);

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge((int) (jwtProperties.getRefreshTokenExpirationMs() / 1000));
        res.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.success(
            TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000)
                .build(),
            "Login successful"
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                .body(ApiResponse.error("MISSING_REFRESH_TOKEN", "Refresh token not found"));
        }
        String email = jwtService.extractEmail(refreshToken);
        String role = jwtService.extractRole(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(
            TokenResponse.builder()
                .accessToken(jwtService.generateAccessToken(email, role))
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000)
                .build(),
            "Token refreshed"
        ));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Principal principal) {
        memberService.changePassword(principal.getName(), req);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}
