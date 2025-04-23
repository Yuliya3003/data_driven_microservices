package com.example.userservice.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        System.out.println("Processing request for path: " + path);


        if (path.equals("/auth/register") || path.equals("/auth/login") ||
                path.equals("/api/auth/register") || path.equals("/api/auth/login") ||
                path.startsWith("/actuator")) {
            System.out.println("Skipping JWT filter for path: " + path);
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        System.out.println("Authorization header: " + header);
        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("No valid Authorization header, returning 403");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            String token = header.replace("Bearer ", "");
            System.out.println("JWT token: " + token);

            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String username = claims.getSubject();

            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            username, null, null));
            chain.doFilter(request, response);
        } catch (Exception e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
    }
}