package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.InvalidUserException;
import com.example.gatewayservice.exception.definition.UserNotFoundException;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class UserServices {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemPropertiesServices systemPropertiesServices;

    public Response<Object> login(UserLoginRq request){
        Response<Object> response = new Response<>();

        try{
            // validateUser
            User user = validateUser(request);

            // acquire role
            String role = user.getRole().getRole();
            String secretKey = systemPropertiesServices.getSystemProperties(role+"_secret_key");

            // createSession
            Map<String, Object> sessionDetail = createSession(user, role, secretKey);


        }catch (Exception e){
            Map error = (Map) e;

            log.error("error validate user {}", e.getMessage());
            response.setError((HttpStatus) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
        }
        return response;
    }



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

    public Map<String, Object> createSession(User user, String role, String key){
        Map<String, Object> map = new LinkedHashMap<>();

        return map;
    }


}
