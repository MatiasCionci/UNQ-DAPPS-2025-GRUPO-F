package com.dappstp.dappstp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dappstp.dappstp.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;


@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired private UserService userService;
    @Autowired private JwtToken jwtToken;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {

        String path = req.getServletPath();
        System.out.println("Requested path: " + path);

        // Mostrar el header Authorization para debug
        String authHeader = req.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);

        // Permitir acceso sin autenticación a /auth/register y /auth/login
        if (path.startsWith("/auth/")) {
            System.out.println("Public path detected, skipping JWT validation");
            chain.doFilter(req, res);
            return;
        }

        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            username = jwtToken.extractUsername(jwt);
            System.out.println("Extracted username from token: " + username);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtToken.validateToken(jwt, userDetails)) {
                System.out.println("JWT is valid. Setting authentication.");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("Invalid JWT.");
            }
        }

        chain.doFilter(req, res);
    }
}

