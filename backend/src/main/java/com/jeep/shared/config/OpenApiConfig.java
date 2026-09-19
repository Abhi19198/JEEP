package com.jeep.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "JEEP API",
        version = "v1",
        description = "Java Enterprise Engineering Platform API",
        license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
    )
)
public class OpenApiConfig {
}
