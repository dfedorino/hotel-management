package ru.hotel.hotelservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the OpenAPI contract from hotel-service-api so Swagger UI shows the source spec, not one
 * re-derived from code.
 */
@Configuration
@ConditionalOnProperty(name = "hotel.api-docs.enabled", havingValue = "true")
public class ApiContractConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi/**").addResourceLocations("classpath:/openapi/");
    }
}
