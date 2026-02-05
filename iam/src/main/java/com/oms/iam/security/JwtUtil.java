package com.oms.iam.security;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.oms.iam.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    @Value("${jwt.private-key}")
    private String privateKeyStr;

    @Value("${jwt.public-key}")
    private String publicKeyStr;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        // Sanitize keys: remove whitespace and newlines just in case
        String cleanPrivateKeyStr = privateKeyStr.replaceAll("\\s", "");
        String cleanPublicKeyStr = publicKeyStr.replaceAll("\\s", "");

        byte[] privateKeyBytes = Base64.getDecoder().decode(cleanPrivateKeyStr);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(privateKeySpec);

        byte[] publicKeyBytes = Base64.getDecoder().decode(cleanPublicKeyStr);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        this.publicKey = keyFactory.generatePublic(publicKeySpec);
    }

    public JwtUtil() {
        // Keys are initialized in init() via @PostConstruct because @Value injection
        // happens after constructor execution, so privateKeyStr and publicKeyStr would
        // be null here
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        String roleName = user.getRole().name();
        // Add both for maximum compatibility
        claims.put("role", roleName);
        claims.put("scope", roleName);
        claims.put("email", user.getEmail());
        claims.put("userId", user.getId());

        return createToken(claims, user.getEmail());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setHeaderParam("kid", "oms-key-id") // Crucial for JWKS matching
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer("oms-iam")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Parses and validates JWT: verifies signature, checks expiration, and ensures
    // token integrity
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyAsString() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public Map<String, Object> getJwks() {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) publicKey)
                .keyID("oms-key-id")
                .build();
        return new JWKSet(jwk).toJSONObject();
    }
}
