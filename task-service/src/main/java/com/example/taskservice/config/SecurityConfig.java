package com.example.taskservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import reactor.core.publisher.Mono;
import io.jsonwebtoken.JwtParser;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/tasks/**").authenticated()
                        .anyExchange().permitAll()
                )
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    @Bean
    public AuthenticationWebFilter jwtAuthenticationFilter() {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(jwtAuthenticationManager());
        filter.setServerAuthenticationConverter(jwtAuthenticationConverter());
        return filter;
    }

    @Bean
    public ReactiveAuthenticationManager jwtAuthenticationManager() {
        return authentication -> Mono.just(authentication)
                .filter(auth -> auth.isAuthenticated())
                .switchIfEmpty(Mono.error(new SecurityException("Invalid authentication")));
    }

    @Bean
    public ServerAuthenticationConverter jwtAuthenticationConverter() {
        return exchange -> {
            String path = exchange.getRequest().getURI().getPath();

            if (path.startsWith("/actuator")) {
                return Mono.empty();
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    JwtParser parser = Jwts.parser()
                            .setSigningKey(jwtSecret.getBytes())
                            .build();
                    Claims claims = parser.parseClaimsJws(token).getBody();
                    String username = claims.getSubject();
                    return Mono.just(new UsernamePasswordAuthenticationToken(username, null, null));
                } catch (Exception e) {
                    return Mono.empty();
                }
            }
            return Mono.empty();
        };
    }
}