package org.example.weektelevenassignment;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {
}
