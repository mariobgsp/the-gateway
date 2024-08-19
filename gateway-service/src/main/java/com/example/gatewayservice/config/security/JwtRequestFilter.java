package com.example.gatewayservice.config.security;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.service.security.TokenBlacklistService;
import com.example.gatewayservice.util.CommonUtil;
import com.example.gatewayservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private void writeJsonResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Response<Object> rs = new Response<>();
        rs.setError(HttpStatus.valueOf(status), HttpStatus.valueOf(status).name(), "98", "98:Unauthorized:" + message);

        PrintWriter out = response.getWriter();
        out.print(CommonUtil.gson.toJson(rs));
        out.flush();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
            } catch (Exception e) {
                // Token is invalid, send unauthorized response
                writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails.getUsername()) && !tokenBlacklistService.isTokenBlacklisted(jwt)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else {
                // Token is invalid or blacklisted, send unauthorized response
                writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized or blacklisted JWT Token");
                return;
            }
        } else if (username == null) {
            // Authorization header is missing or doesn't start with "Bearer ", send unauthorized response
            writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Authorization header is missing or invalid");
            return;
        }

        chain.doFilter(request, response);
    }
}
