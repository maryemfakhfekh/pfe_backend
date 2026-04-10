package com.asm.gestion_stagiaires.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "file:C:/Users/lenovo/Desktop/pfe/gestion-stagiaires/gestion-stagiaires/uploads/cvs/"
                );
    }
}