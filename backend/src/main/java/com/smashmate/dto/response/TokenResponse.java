package com.smashmate.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
