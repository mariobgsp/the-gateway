package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.InvalidSessionException;
import com.example.gatewayservice.exception.definition.InvalidUserException;
import com.example.gatewayservice.exception.definition.UserNotFoundException;
import com.example.gatewayservice.models.entity.Role;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.entity.UserLog;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.repository.UserLogRepository;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.util.CommonUtil;
import com.example.gatewayservice.util.DateTimeUtil;
import com.example.gatewayservice.util.SecretKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class UserServices extends UserUtilityServices{

    @Autowired
    private SystemPropertiesServices systemPropertiesServices;

    public Response<Object> login(UserLoginRq request){
        Response<Object> response = new Response<>();
        UserLog userLog = new UserLog();
        User user = new User();
        String sessionStatus = "";
        String activityName = "";

        try{
            // validateUser
            user = validateUser(request);

            // acquire role
            String role = user.getRole().getRole();
            log.info("generate secret key {}", user.getUsername());
            String secretKey = systemPropertiesServices.getSystemProperties(role+"_secret_key");

            // createSession
            Map<String, Object> sessionDetail = createSession(user, role, secretKey);
            log.info("success create session {}", user.getUsername());

            // insert successful login into user_log
            activityName = systemPropertiesServices.getSystemProperties("login_activity_name");
            sessionStatus = systemPropertiesServices.getSystemProperties("active_session_status");
            userLog = CommonUtil.constructUserLog(
                    user,
                    activityName,
                    sessionStatus,
                    (String) sessionDetail.get("sessionId"),
                    (String) sessionDetail.get("token"),
                    null);

            // construct response
            Map<String, Object> rs = new LinkedHashMap<>();
            rs.put("loginMessage", "Login Successful!");
            rs.put("sessionStatus", sessionStatus);
            rs.put("userSession", sessionDetail.get("sessionId"));
            rs.put("userToken", sessionDetail.get("token"));
            rs.put("loginSessionTime", sessionDetail.get("sessionTime"));

            response.setSuccess(rs);
        }catch (Exception e){
            Map error = (Map) e;

            sessionStatus = systemPropertiesServices.getSystemProperties("failed_session_status");
            activityName = systemPropertiesServices.getSystemProperties("login_activity_name");
            userLog = CommonUtil.constructUserLog(
                    user,
                    activityName,
                    sessionStatus,
                    null,
                    null,
                    e.getMessage());

            log.error("error login {}", e.getMessage());
            response.setError((HttpStatus) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
        }
        // save log after login activity
        saveUserLog(userLog, user, sessionStatus);
        return response;
    }

    public Response<Object> validateSession(String token){
        Response<Object> rs = new Response<>();
        String sessionStatus = "";
        String activityName = "";
        UserLog userLog = new UserLog();

        try{
            sessionStatus = systemPropertiesServices.getSystemProperties("active_session_status");
            activityName = systemPropertiesServices.getSystemProperties("validate_activity_name");

            userLog = findUserLogDetailedByToken(token);
            if(userLog==null){
                throw new InvalidSessionException("invalid session, please re-login!");
            }
            User user = userLog.getUser();
            if(user.getUserSessionStatus().equals("INACTIVE")){
                throw new InvalidSessionException("invalid session, please re-login!");
            }
            Role role = findUserRole(user.getUsername());

            if(checkSessionV2(user, role.getRole())){
                // renew userLog
                userLog = findUserLogByToken(token);
                userLog.setUserActivityName(activityName);
                userLog.setUserSessionStatus(sessionStatus);
            }else{
                throw new InvalidSessionException("invalid session, please re-login!");
            };

            rs.setSuccessMessage("API Ready to Test!");
        }catch (Exception e){
            Map error = (Map) e;
            sessionStatus = systemPropertiesServices.getSystemProperties("inactive_session_status");

            // renew userLog
            userLog = findUserLogByToken(token);
            userLog.setUserActivityName(activityName);
            userLog.setUserSessionStatus(sessionStatus);
            userLog.setErrorMessage(e.getMessage());

            log.error("error logout {}", e.getMessage());
            rs.setError((HttpStatus) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
        }
        return rs;
    }

    public void logout(UserLoginRq request, String token){
        Response<Object> response = new Response<>();
        User user = new User();
        UserLog userLog = new UserLog();
        String sessionStatus = "";
        String activityName = "";

        try {
            // get setting
            sessionStatus = systemPropertiesServices.getSystemProperties("inactive_session_status");
            activityName = systemPropertiesServices.getSystemProperties("logout_activity_name");

            // validateUser
            user = validateUser(request);
            if(user.getUserSessionStatus().equals("INACTIVE")){
                throw new InvalidSessionException("invalid session, please re-login!");
            }

            // acquire role
            String role = user.getRole().getRole();
            if(checkSession(user, role, token)){
                // renew userLog
                userLog = findUserLogByToken(token);
                userLog.setUserActivityName(activityName);
                userLog.setUserSessionStatus(sessionStatus);
            }else{
                throw new InvalidSessionException("invalid session, please re-login!");
            }

        }catch (Exception e){
            Map error = (Map) e;
            sessionStatus = systemPropertiesServices.getSystemProperties("inactive_session_status");

            // renew userLog
            userLog = findUserLogByToken(token);
            userLog.setUserActivityName(activityName);
            userLog.setUserSessionStatus(sessionStatus);
            userLog.setErrorMessage(e.getMessage());

            log.error("error logout {}", e.getMessage());
            response.setError((HttpStatus) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
        }
        saveUserLog(userLog, user, sessionStatus);
    }






}
