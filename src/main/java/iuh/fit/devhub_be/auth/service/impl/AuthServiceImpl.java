package iuh.fit.devhub_be.auth.service.impl;

import iuh.fit.devhub_be.auth.dto.request.LoginRequest;
import iuh.fit.devhub_be.auth.dto.request.RegisterRequest;
import iuh.fit.devhub_be.auth.dto.response.AuthPair;
import iuh.fit.devhub_be.auth.dto.response.AuthResponse;
import iuh.fit.devhub_be.auth.dto.response.UserResponse;
import iuh.fit.devhub_be.auth.model.RefreshToken;
import iuh.fit.devhub_be.auth.model.Role;
import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.auth.repository.RefreshTokenRepository;
import iuh.fit.devhub_be.auth.repository.RoleRepository;
import iuh.fit.devhub_be.auth.repository.UserRepository;
import iuh.fit.devhub_be.auth.service.AuthService;
import iuh.fit.devhub_be.common.exception.BadRequestException;
import iuh.fit.devhub_be.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already in use");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));

        User user = new User();
        user.setEmail(request.email());
        user.setUserName(request.userName());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(userRole);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return buildAuthPair(user);
    }

    @Override
    @Transactional
    public AuthPair refresh(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = stored.getUser();
        refreshTokenRepository.delete(stored);

        return buildAuthPair(user);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshTokenRepository::delete);
    }

    private AuthPair buildAuthPair(User user) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpiration));
        refreshTokenRepository.save(refreshToken);

        String accessToken = generateAccessToken(user);
        AuthResponse authResponse = new AuthResponse(accessToken, "Bearer", toUserResponse(user));

        return new AuthPair(authResponse, rawToken);
    }

    private String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpiration))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getName())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().getName()
        );
    }
}
