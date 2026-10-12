package com.neueda.leap.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Value("${jwt.shared-secret}")
  private String sharedSecret;

  @Bean
  public JwtDecoder jwtDecoder() {
    SecretKey key = new SecretKeySpec(sharedSecret.getBytes(UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }

  @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/public").permitAll()
        .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }
}
