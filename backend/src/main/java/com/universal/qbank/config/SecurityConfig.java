package com.universal.qbank.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/**", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/questions", "/api/questions/**")
                    .permitAll()
                    .requestMatchers("/api/knowledge-points", "/api/knowledge-points/**")
                    .permitAll()
                    .requestMatchers("/api/analytics", "/api/analytics/**")
                    .permitAll()
                    .requestMatchers("/api/papers", "/api/papers/**")
                    .permitAll()
                    .requestMatchers("/api/exams", "/api/exams/**")
                    .permitAll()
                    .requestMatchers("/api/exam-plans", "/api/exam-plans/**")
                    .permitAll()
                    .requestMatchers("/api/user/**", "/api/files/**", "/uploads/**")
                    .permitAll()
                    .requestMatchers("/api/import/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**", "/api/stats/**")
                    .permitAll()
                    .requestMatchers("/api/organizations/**", "/api/my-organizations/**")
                    .permitAll()
                    .requestMatchers("/api/roles/**", "/api/permissions/**", "/api/users/**")
                    .permitAll()
                    .requestMatchers("/api/universities/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .headers(headers -> headers.frameOptions(frame -> frame.disable())); // For H2 Console

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*")); // Allow all origins
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
