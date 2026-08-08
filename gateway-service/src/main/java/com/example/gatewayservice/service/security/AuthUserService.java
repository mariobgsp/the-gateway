package com.example.gatewayservice.service.security;

import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.exception.definition.UserNotFoundException;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.models.user.UserLoginRs;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.service.SystemPropertiesServices;
import com.example.gatewayservice.util.CommonUtil;
import com.example.gatewayservice.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    @Autowired
    private LoginAttemptService loginAttemptService;

    public Response<Object> authLogin(UserLoginRq request) {
        Response<Object> rs = new Response<>();
        String username = null;

        try {
            if (request == null || request.getUsername() == null || request.getUsername().isBlank()
                    || request.getPassword() == null || request.getPassword().isBlank()) {
                throw new InvalidRequestException("username and password are required");
            }
            username = request.getUsername().trim();

            if (loginAttemptService.isBlocked(username)) {
                rs.setError(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.name(), "05",
                        "05:TooManyRequests:too many login attempts, please try again later");
                return rs;
            }

            log.info("start authLogin for username: {}", username);

            Optional<User> user = userRepository.findDetailedByUsername(username);
            if (user.isEmpty()) {
                throw new UserNotFoundException("user not found!");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtUtil.generateToken(userDetails.getUsername());

            // set user ACTIVE session
            user.get().setUserSessionStatus("ACTIVE");
            user.get().setUserLastLogin(LocalDateTime.now());
            userRepository.save(user.get());
            loginAttemptService.registerSuccess(username);

            // save enabled token
            tokenBlacklistService.saveActiveToken(accessToken);

            UserLoginRs userLoginRs = new UserLoginRs();
            userLoginRs.setLoginMessage("success login!");
            userLoginRs.setAccessToken(accessToken);
            userLoginRs.setTokenLifetime(propertiesServices.getProps("TOKEN_EXPIRATION"));

            rs.setSuccess(userLoginRs);
        } catch (Exception e) {
            log.error("error authLogin for user: {}", e.getMessage());
            if (e instanceof org.springframework.security.core.AuthenticationException
                    || e instanceof UserNotFoundException) {
                loginAttemptService.registerFailure(username);
                rs.setError(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        org.springframework.http.HttpStatus.UNAUTHORIZED.name(), "06",
                        "06:Unauthorized:invalid username or password");
            } else {
                CommonUtil.applyError(rs, e);
            }
        }
        log.info("done authLogin status={} code={}", rs.getStatus(), rs.getCode());
        return rs;
    }

    public Response<Object> authLogout(String authHeader) {
        Response<Object> rs = new Response<>();

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new InvalidRequestException("Authorization header should not be empty");
            }
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);
            log.info("start authLogout for username: {}", username);

            tokenBlacklistService.blacklistToken(token);

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
        } catch (Exception e) {
            log.error("error authLogout: {}", e.getMessage());
            CommonUtil.applyError(rs, e);
        }
        log.info("done authLogout status={} code={}", rs.getStatus(), rs.getCode());
        return rs;
    }
}
