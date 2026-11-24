
package com.orderflow.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productServiceOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("OrderFlow - Product Service API")
                        .description("""
                                REST APIs for managing products in the
                                OrderFlow enterprise e-commerce platform.

                                Features:
                                - Product creation and management
                                - Pagination and sorting
                                - Dynamic product filtering
                                - Product soft deletion
                                - Centralized error handling
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("OrderFlow Development Team"))
                        .license(new License()
                                .name("MIT License")));
    }
}
