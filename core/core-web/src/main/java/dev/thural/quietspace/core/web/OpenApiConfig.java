package dev.thural.quietspace.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenApi specification for QuietSpace")
                        .version("1.0")
                        .description("OpenApi documentation for Spring Security")
                        .termsOfService("Terms of Service")
                        .contact(new Contact()
                                .name("QuietsSpace")
                                .email("contact@quietspace.live"))
                        .license(new License()
                                .name("GNU General Public License (GPL)")
                                .url("quietspace.live/open-api-license")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local ENV"))
                .addServersItem(new Server()
                        .url("https://quietspace.live")
                        .description("Prod ENV"))
                .schemaRequirement("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("JWT Auth Scheme"))
                .security(List.of(new SecurityRequirement().addList("bearerAuth")));
    }
}
