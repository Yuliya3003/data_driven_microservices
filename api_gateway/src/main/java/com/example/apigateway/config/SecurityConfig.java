package com.example.apigateway.config;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public GatewayFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationGatewayFilterFactory(jwtSecret).apply(config -> {});
    }

    public static class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {
        private final String jwtSecret;

        public JwtAuthenticationGatewayFilterFactory(String jwtSecret) {
            super(Config.class);
            this.jwtSecret = jwtSecret;
        }

        @Override
        public GatewayFilter apply(Config config) {
            return (exchange, chain) -> {
                String path = exchange.getRequest().getURI().getPath();

                if (path.startsWith("/api/auth") || path.startsWith("/actuator")) {
                    return chain.filter(exchange);
                }

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }

                try {
                    String token = authHeader.substring(7);
                    Jwts.parser()
                            .setSigningKey(jwtSecret.getBytes())
                            .build()
                            .parseClaimsJws(token);
                    return chain.filter(exchange);
                } catch (Exception e) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            };
        }

        public static class Config {

        }
    }
}
