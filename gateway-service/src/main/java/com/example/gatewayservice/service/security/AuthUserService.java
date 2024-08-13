package com.example.gatewayservice.service.security;

import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.exception.definition.UserNotFoundException;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.models.user.UserLoginRs;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.service.SystemPropertiesServices;
import com.example.gatewayservice.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AuthUserService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    @Autowired
    private SystemPropertiesServices propertiesServices;
    @Autowired
    private UserRepository userRepository;

    public Response<Object> authLogin(UserLoginRq request) {
        log.info("start authLogin {}", request);
        Response<Object> rs = new Response<>();

        try {
            Optional<User> user = userRepository.findDetailedByUsername(request.getUsername());
            if (user.isEmpty()) {
                throw new UserNotFoundException("user not found!");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtUtil.generateToken(userDetails.getUsername());

            // set user ACTIVE session
            user.get().setUserSessionStatus("ACTIVE");
            userRepository.save(user.get());

            // save enabled token
            tokenBlacklistService.saveActiveToken(accessToken);

            UserLoginRs userLoginRs = new UserLoginRs();
            userLoginRs.setLoginMessage("success login!");
            userLoginRs.setAccessToken(accessToken);
            userLoginRs.setTokenLifetime(propertiesServices.getProps("TOKEN_EXPIRATION"));

            rs.setSuccess(userLoginRs);
        } catch (Exception e) {
            log.error("error authLogin {}", e.getMessage());
            Map error = (Map) e;
            if (error.get("errorCode") != null) {
                rs.setError((HttpStatus) error.get("httpStatus"), (String) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
            } else {
                rs.setError(e.getMessage());
            }
        }
        log.info("done authLogin {}", rs);
        return rs;
    }

    public Response<Object> authLogout(String authHeader) {
        log.info("start authLogout {}", authHeader);
        Response<Object> rs = new Response<>();

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.getUsernameFromToken(token);
                boolean isSuccess = tokenBlacklistService.blacklistToken(token);
                log.info("is success blacklist {}", isSuccess);

                Optional<User> user = userRepository.findDetailedByUsername(username);
                if (user.isEmpty()) {
                    throw new UserNotFoundException("user not found!");
                }

                // set user INACTIVE session
                user.get().setUserSessionStatus("INACTIVE");
                userRepository.save(user.get());

                UserLoginRs userLoginRs = new UserLoginRs();
                userLoginRs.setLogoutMessage("success logout!");

                rs.setSuccess(userLoginRs);
            } else {
                throw new InvalidRequestException("Authorization header should not be empty");
            }

        } catch (Exception e) {
            Map error = (Map) e;
            if (error.get("errorCode") != null) {
                rs.setError((HttpStatus) error.get("httpStatus"), (String) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
            } else {
                rs.setError(e.getMessage());
            }
        }
        log.info("done authLogout {}", rs);
        return rs;
    }


}
