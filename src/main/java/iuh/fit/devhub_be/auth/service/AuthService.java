package iuh.fit.devhub_be.auth.service;

import iuh.fit.devhub_be.auth.dto.request.LoginRequest;
import iuh.fit.devhub_be.auth.dto.request.RegisterRequest;
import iuh.fit.devhub_be.auth.dto.response.AuthPair;

public interface AuthService {
    void register(RegisterRequest request);
    AuthPair login(LoginRequest request);
    AuthPair refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
}
