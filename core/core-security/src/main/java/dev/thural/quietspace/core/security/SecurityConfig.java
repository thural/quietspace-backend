package dev.thural.quietspace.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final AuthenticationProvider authenticationProvider;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/**",
                                "/ws", "/ws/**",
                                "/hello/**",
                                "/v2/api-docs", "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-resources", "/swagger-resources/**",
                                "/configuration/ui", "/configuration/security",
                                "/swagger-ui/**", "/webjars/**", "/swagger-ui.html",
                                "/springwolf/**"
                        )
                )
                .authorizeHttpRequests(request ->
                                request
                                        .requestMatchers(
                                                "/ws",
                                                "/ws/**",
                                                "/api/v1/ws/**",
                                                "/api/v1/auth/**",
                                                "/hello/**",
                                                "/actuator/health",
                                                "/v2/api-docs",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**",
                                                "/swagger-resources",
                                                "/swagger-resources/**",
                                                "/configuration/ui",
                                                "/configuration/security",
                                                "/swagger-ui/**",
                                                "/webjars/**",
                                                "/swagger-ui.html",
                                                "/springwolf/**"
                                        )
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                        )
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'")
                                .reportOnly()
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(withDefaults())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                )
                .exceptionHandling((exception) -> {
                    exception.authenticationEntryPoint(jwtAuthEntryPoint);
                    exception.accessDeniedHandler(customAccessDeniedHandler);
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider);

        return http.build();
    }
}
