package com.example.gatewayservice.service.security;

import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.exception.definition.UserNotFoundException;
import com.example.gatewayservice.exception.models.CommonException;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.rqrs.response.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.models.user.UserLoginRs;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.service.SystemPropertiesServices;
import com.example.gatewayservice.service.redis.RedisServices;
import com.example.gatewayservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.concurrent.TimeUnit;

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
    private RedisServices redisServices;

    public Response<Object> authLogin(UserLoginRq request) {
        log.info("start authLogin {}", request);
        Response<Object> rs = new Response<Object>();

        try {
            Optional<User> user = userRepository.findDetailedByUsername(request.getUsername());
            if (user.isEmpty()) {
                throw new UserNotFoundException("user not found!");
            }
            redisServices.setWithTTL("username_"+request.getUsername(), request.getUsername(), 1440, TimeUnit.MINUTES);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtUtil.generateToken(userDetails.getUsername());

            // set user ACTIVE session
            user.get().setUserSessionStatus("ACTIVE");
            user.get().setUserLastLogin(LocalDateTime.now());
            userRepository.save(user.get());

            // save enabled token
            tokenBlacklistService.saveActiveToken(accessToken);

            UserLoginRs userLoginRs = new UserLoginRs();
            userLoginRs.setLoginMessage("success login!");
            userLoginRs.setAccessToken(accessToken);
            userLoginRs.setTokenLifetime(Long.valueOf(propertiesServices.getProps("TOKEN_EXPIRATION")));

            rs.setSuccess(userLoginRs);
        } catch (Exception e) {
            log.error("error authLogin {}", e.getMessage());
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }
        log.info("done authLogin {}", rs);
        return rs;
    }

    public Response<Object> authLogout(String authHeader) {
        log.info("start authLogout {}", authHeader);
        Response<Object> rs = new Response<Object>();

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
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }
        log.info("done authLogout {}", rs);
        return rs;
    }

    public User findByUsername(String username){
        Optional<User> user = userRepository.findDetailedByUsername(username);
        return user.orElse(null);
    }

    public String findUsernameUsingJwt(String authorization){
        String jwt = authorization.substring(7);
        String username = null;

        try {
            username = jwtUtil.getUsernameFromToken(jwt);
        } catch (Exception e) {
            log.error("failed parse jwt token {}", e.getMessage());
        }

        return username;
    }

}
