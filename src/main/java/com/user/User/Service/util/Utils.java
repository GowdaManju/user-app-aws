package com.user.User.Service.util;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Utils {

    @Bean
    public ModelMapper getMapper(){
    return new ModelMapper();
}


}
