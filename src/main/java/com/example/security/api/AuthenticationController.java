package com.example.security.api;


import com.example.security.dto.LoginRequest;
import com.example.security.dto.LoginResponse;
import com.example.security.entity.UserWithRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@RestController
@RequestMapping("/auth/")
@CrossOrigin
public class AuthenticationController {

    @Value("${app.token-issuer}")
    private String tokenIssuer;

    @Value("${app.token-expiration}")
    private long tokenExpiration;
    private final AuthenticationManager authenticationManager;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    JwtEncoder encoder;

    public AuthenticationController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            UsernamePasswordAuthenticationToken uat = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
            Authentication authentication = authenticationManager.authenticate(uat);

            UserWithRoles user = (UserWithRoles) authentication.getPrincipal();
            Instant now = Instant.now();
            long expiry = tokenExpiration;
            String scope = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(joining(" "));
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(tokenIssuer)  //Only this for simplicity
                    .issuedAt(now)
                    .audience(Arrays.asList("not used"))
                    .expiresAt(now.plusSeconds(tokenExpiration))
                    .subject(user.getUsername())
                    .claim("roles", scope)
                    .build();

            JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").type("JWT").build();

            String token = encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();


            List<String> roles = user.getRoles().stream().map(role -> role.toString()).collect(Collectors.toList());
            return ResponseEntity.ok()
                    .body(new LoginResponse(user.getUsername(), token, roles));
        } catch (BadCredentialsException ex) {
            throw ex;
            //throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Username or password wrong");
        }
    }

    @PostMapping("/validateToken")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // Extract the token from the Authorization header
            String token = authorizationHeader.substring(7);

            // Decode the token using the JwtDecoder
            Jwt jwt = jwtDecoder.decode(token);

            // Check if the token is expired
            if (jwt.getExpiresAt().isBefore(new Date().toInstant())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Token is valid, return a success response
            return ResponseEntity.ok().build();
        } catch (JwtException ex) {
            // Token is invalid, return an error response
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
