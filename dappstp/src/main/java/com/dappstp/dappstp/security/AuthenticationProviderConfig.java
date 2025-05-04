package com.dappstp.dappstp.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import com.dappstp.dappstp.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationProviderConfig {

    @Autowired
    public void configureGlobal(
        AuthenticationManagerBuilder auth,
        UserService userService,
        PasswordEncoder passwordEncoder
    ) throws Exception {
        auth.userDetailsService(userService)
            .passwordEncoder(passwordEncoder);
    }
}
