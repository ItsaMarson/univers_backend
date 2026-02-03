/* (C)2025-2026 */
package com.univers.univers_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Add /api prefix to all @RestController annotated classes
        configurer.addPathPrefix(
                "/api",
                c ->
                        c.isAnnotationPresent(
                                org.springframework.web.bind.annotation.RestController.class));
    }
}
