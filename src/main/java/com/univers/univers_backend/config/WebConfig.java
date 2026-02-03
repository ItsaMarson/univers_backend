/* (C)2025-2026 */
package com.univers.univers_backend.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static assets (CSS, JS, images) from frontend/dist/assets
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600)
                .resourceChain(true);

        // Serve other static files (favicon, manifest, etc.)
        registry.addResourceHandler(
                        "/*.js", "/*.css", "/*.ico", "/*.png", "/*.json", "/*.webp", "/*.svg")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);

        // Handle client-side routing - forward all non-API, non-static requests to index.html
        // This must be the last handler to act as a fallback
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0)
                .resourceChain(true)
                .addResolver(
                        new PathResourceResolver() {
                            @Override
                            protected Resource getResource(String resourcePath, Resource location)
                                    throws IOException {
                                // Don't handle /api or /ws requests
                                if (resourcePath.startsWith("api")
                                        || resourcePath.startsWith("/api")
                                        || resourcePath.startsWith("ws")
                                        || resourcePath.startsWith("/ws")) {
                                    return null;
                                }

                                Resource requestedResource = location.createRelative(resourcePath);

                                // If the requested resource exists, serve it
                                if (requestedResource.exists() && requestedResource.isReadable()) {
                                    return requestedResource;
                                }

                                // Otherwise, return index.html for client-side routing
                                return new ClassPathResource("/static/index.html");
                            }
                        });
    }
}
