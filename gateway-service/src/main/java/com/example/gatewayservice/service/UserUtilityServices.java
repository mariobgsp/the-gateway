package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.InternalErrorException;
import com.example.gatewayservice.exception.definition.InvalidSessionException;
import com.example.gatewayservice.exception.definition.InvalidUserException;
import com.example.gatewayservice.exception.definition.UserNotFoundException;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.entity.UserLog;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.repository.UserLogRepository;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.util.DateTimeUtil;
import com.example.gatewayservice.util.SecretKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class UserUtilityServices {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SystemPropertiesServices systemPropertiesServices;
    @Autowired
    private UserLogRepository userLogRepository;

    public User validateUser(UserLoginRq request) throws Exception{
        // find by userName
        Optional<User> user = userRepository.findDetailedByUsername(request.getUsername());
        if(user.isEmpty()){
            throw new UserNotFoundException("user not found");
        }

        // check password
        if(!user.get().getPassword().equals(request.getPassword())){
            throw new InvalidUserException("invalid user password");
        }
        return user.get();
    }

    public Map<String, Object> createSession(User user, String role, String key) throws InternalErrorException {
        Map<String, Object> map = new LinkedHashMap<>();

        try{
            String loginDate = DateTimeUtil.formatDateTime(DateTimeUtil.getCurrentDateTime());

            // generate sessionId
            String sessionId = SecretKeyUtil.generateHash(user+loginDate+key);

            // generate tokenKey
            String tokenKey = SecretKeyUtil.generateHash(user+role+key);

            // get active time based on role
            int sessionTime = Integer.parseInt(systemPropertiesServices.getSystemProperties(role+"_session_time"));

            map.put("loginDate", loginDate);
            map.put("sessionId", sessionId);
            map.put("token", tokenKey);
            map.put("sessionTime", sessionTime);

        }catch (Exception e){
            log.error("error create session message:{}", e.getMessage());
            throw new InternalErrorException(String.format("error create session message: %s", e.getMessage()));
        }

        return map;
    }

    public boolean checkSession(User user, String role, String token) throws Exception{

        // getUserLog By Token
        Optional<UserLog> userLog = userLogRepository.findByUserToken(token);
        if(userLog.isEmpty()){
            throw new InvalidSessionException("invalid session, please re-login!");
        }
        log.info("session valid!");

        // count depends on role
        int sessionTime = Integer.parseInt(systemPropertiesServices.getSystemProperties(role+"_session_time"));
        LocalDateTime userLastLogin = user.getUserLastLogin();
        LocalDateTime currentTime = DateTimeUtil.getCurrentDateTime();

        long minutesDifference = ChronoUnit.MINUTES.between(userLastLogin, currentTime);

        if (minutesDifference >= -sessionTime && minutesDifference <= sessionTime) {
            System.out.println("User's last login is within " + sessionTime + " minutes of the current time.");
            return true;
        } else {
            System.out.println("User's last login is more than " + sessionTime + " minutes ago or in the future.");
            return false;
        }
    }

    public UserLog findUserLogByToken(String token){
        Optional<UserLog> userLog = userLogRepository.findByUserToken(token);
        if(userLog.isEmpty()){
            return null;
        }
        return userLog.get();
    }

    public UserLog findUserLogDetailedByToken(String token){
        Optional<UserLog> userLog = userLogRepository.findUserLogByToken(token);
        if(userLog.isEmpty()){
            return null;
        }
        return userLog.get();
    }



    public void saveUserLog(UserLog userLog, User user, String status){
        try {
            updateUser(user, status);
            userLogRepository.save(userLog);
        }catch (Exception e){
            log.error("failed save user_log {}", e.getMessage());
        }
    }

    public void updateUser(User user, String status){
        try {
            user.setUserLastLogin(DateTimeUtil.getCurrentDateTime());
            user.setUserSessionStatus(status);
            userRepository.save(user);
        }catch (Exception e){
            log.error("failed updateUser{}", e.getMessage());
        }
    }
}
