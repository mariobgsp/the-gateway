package com.example.gatewayservice.service.security;

import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.service.SystemPropertiesServices;
import com.example.gatewayservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private SystemPropertiesServices propertiesServices;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthUserService authUserService;

    private UserLoginRq loginRequest(String username, String password) {
        UserLoginRq request = new UserLoginRq();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void authLoginRejectsEmptyCredentials() {
        Response<Object> rs = authUserService.authLogin(loginRequest("", ""));

        assertEquals("04", rs.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, rs.getHttpStatus());
        verifyNoInteractions(userRepository);
    }

    @Test
    void authLoginBlockedAfterTooManyAttempts() {
        when(loginAttemptService.isBlocked("ario_test")).thenReturn(true);

        Response<Object> rs = authUserService.authLogin(loginRequest("ario_test", "password123"));

        assertEquals("05", rs.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, rs.getHttpStatus());
        verify(userRepository, never()).findDetailedByUsername(any());
    }

    @Test
    void authLoginUnknownUserRegistersFailureAndHidesExistence() {
        when(loginAttemptService.isBlocked("nobody")).thenReturn(false);
        when(userRepository.findDetailedByUsername("nobody")).thenReturn(Optional.empty());

        Response<Object> rs = authUserService.authLogin(loginRequest("nobody", "password123"));

        assertEquals("06", rs.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, rs.getHttpStatus());
        verify(loginAttemptService).registerFailure("nobody");
    }

    @Test
    void authLoginBadCredentialsReturnsGenericUnauthorized() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ario_test");
        user.setPassword("$2a$10$s8CVccrkN0d/eryxRHMz2.2m.YBmYLzRBPedMY34b9l6xyL8I8ru6");

        when(loginAttemptService.isBlocked("ario_test")).thenReturn(false);
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        Response<Object> rs = authUserService.authLogin(loginRequest("ario_test", "wrong"));

        assertEquals("06", rs.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, rs.getHttpStatus());
        verify(loginAttemptService).registerFailure("ario_test");
    }

    @Test
    void authLoginSuccessGeneratesTokenAndActivatesSession() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ario_test");
        user.setPassword("$2a$10$db.qhjUpDeOgc249ziI2oepgmTMHKrT6YYv276Lh4mN1U7zvwcija");
        user.setUserSessionStatus("INACTIVE");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("ario_test").password(user.getPassword()).authorities("ROLE_USER").build();
        Authentication authentication = new org.springframework.security.authentication.TestingAuthenticationToken(userDetails, null);

        when(loginAttemptService.isBlocked("ario_test")).thenReturn(false);
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken("ario_test")).thenReturn("jwt-token");
        when(propertiesServices.getProps("TOKEN_EXPIRATION")).thenReturn("3600");

        Response<Object> rs = authUserService.authLogin(loginRequest("ario_test", "password123"));

        assertEquals("00", rs.getCode());
        assertEquals("ACTIVE", user.getUserSessionStatus());
        verify(tokenBlacklistService).saveActiveToken("jwt-token");
        verify(loginAttemptService).registerSuccess("ario_test");
    }

    @Test
    void authLogoutRejectsMissingHeader() {
        Response<Object> rs = authUserService.authLogout(null);

        assertEquals("04", rs.getCode());
    }

    @Test
    void authLogoutBlacklistsTokenAndDeactivatesUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ario_test");

        when(jwtUtil.getUsernameFromToken("jwt-token")).thenReturn("ario_test");
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(user));

        Response<Object> rs = authUserService.authLogout("Bearer jwt-token");

        assertEquals("00", rs.getCode());
        verify(tokenBlacklistService).blacklistToken("jwt-token");
        assertEquals("INACTIVE", user.getUserSessionStatus());
    }
}
