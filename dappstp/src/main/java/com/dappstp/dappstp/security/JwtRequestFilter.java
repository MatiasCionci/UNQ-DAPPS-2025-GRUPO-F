package com.dappstp.dappstp.security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
  
    @Autowired// Hacemos la dependencia opcional
    private JwtToken jwtToken;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {

        String path = req.getServletPath();
        logger.info("Requested path: {}", path);

        // Mostrar el header Authorization para debug
        String authHeader = req.getHeader("Authorization");
        logger.info("Authorization header: " + authHeader);

        // Permitir acceso sin autenticación a /auth/register y /auth/login
        if (path.startsWith("/auth/")) {
            logger.info("Public path detected, skipping JWT validation");
            chain.doFilter(req, res);
            return;
        }

        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            username = jwtToken.extractUsername(jwt);
            logger.info("Extracted username from token: " + username);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtToken.validateToken(jwt, userDetails)) {
               logger.info("JWT is valid. Setting authentication.");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                logger.info("Invalid JWT.");
            }
        }

        chain.doFilter(req, res);
    }
}

