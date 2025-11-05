package com.smokefree.program.config;

import com.smokefree.program.auth.DevAutoUserFilter;
import com.smokefree.program.auth.HeaderUserContextFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Optional;

@Configuration
public class SecurityConfig {

    private final HeaderUserContextFilter headerFilter;
    private final Optional<DevAutoUserFilter> devAuto; // chỉ có khi profile=dev

    @Autowired
    public SecurityConfig(HeaderUserContextFilter headerFilter,
                          @Autowired(required = false) DevAutoUserFilter devAuto) {
        this.headerFilter = headerFilter;
        this.devAuto = Optional.ofNullable(devAuto);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/debug/**").permitAll()
                .anyRequest().authenticated());

        http.addFilterBefore(headerFilter, BasicAuthenticationFilter.class);
        // đảm bảo chạy SAU HeaderUserContextFilter
        devAuto.ifPresent(f -> http.addFilterAfter(f, HeaderUserContextFilter.class));

        return http.build();
    }

    // Đăng ký HeaderUserContextFilter làm bean để tiêm Environment
    @Bean
    public HeaderUserContextFilter headerUserContextFilter(org.springframework.core.env.Environment env) {
        return new HeaderUserContextFilter(env);
    }
}
