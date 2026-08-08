package com.example.gatewayservice.config.security;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.util.CommonUtil;
import com.example.gatewayservice.service.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers("/gateway/user/login").permitAll()
                                .anyRequest().authenticated())
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer ->
                        httpSecurityExceptionHandlingConfigurer
                                .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                                .accessDeniedHandler(jsonAccessDeniedHandler()))
                .sessionManagement(httpSecuritySessionManagementConfigurer -> httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, authException) -> writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "98", "unauthorized");
    }

    private AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeError(response, HttpServletResponse.SC_FORBIDDEN, "97", "forbidden");
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Response<Object> rs = new Response<>();
        rs.setError(HttpStatus.valueOf(status), HttpStatus.valueOf(status).name(), code, code + ":" + message);
        response.getWriter().print(CommonUtil.gson.toJson(rs));
        response.getWriter().flush();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        DelegatingPasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        passwordEncoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return passwordEncoder;
    }
}
