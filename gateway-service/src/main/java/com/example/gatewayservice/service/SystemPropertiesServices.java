package com.example.gatewayservice.service;

import com.example.gatewayservice.models.entity.SystemProperties;
import com.example.gatewayservice.repository.SystemPropertiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemPropertiesServices {

    @Autowired
    private SystemPropertiesRepository systemPropertiesRepository;

    public String getProps(String key){
        Optional<SystemProperties> s = systemPropertiesRepository.findByKey(key);
        if(s.isPresent()){
            return s.get().getValue();
        }
        return "";
    }
}
