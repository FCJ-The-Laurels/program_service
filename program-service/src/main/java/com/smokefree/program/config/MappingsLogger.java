package com.smokefree.program.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// com.smokefree.program.config.MappingsLogger
@Slf4j
@Configuration
public class MappingsLogger {

    @Bean
    CommandLineRunner logMappings(
            @Qualifier("requestMappingHandlerMapping")
            RequestMappingHandlerMapping mapping) {
        return args -> {
            mapping.getHandlerMethods().forEach((info, handler) -> {
                var paths = info.getPathPatternsCondition() != null
                        ? info.getPathPatternsCondition().getPatternValues()
                        : (info.getPatternsCondition() != null
                        ? info.getPatternsCondition().getPatterns()
                        : java.util.Set.of());
                var methods = info.getMethodsCondition().getMethods();
                log.info("[API] {} {} -> {}", methods, paths, handler.getMethod().toGenericString());
            });
        };
    }
}


