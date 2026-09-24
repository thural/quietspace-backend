package dev.thural.quietspace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.thural.quietspace.security.AuthenticationProvider;
import dev.thural.quietspace.security.JwtTokenService;
import dev.thural.quietspace.security.JwtTokenServiceImpl;
import dev.thural.quietspace.security.UserAuthenticationProvider;
import dev.thural.quietspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppConfig {
    private final UserRepository userRepository;

    @Value("${custom.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return username -> {
            log.debug("loading user by identifier");
            return userRepository.findUserEntityByEmail(username).orElseGet(
                    () -> userRepository.findUserByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found"))
            );
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedHeaders(Arrays.asList(
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.CACHE_CONTROL,
                HttpHeaders.ORIGIN,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.AUTHORIZATION
        ));
        config.setAllowedMethods(Stream.of(
                HttpMethod.GET,
                HttpMethod.POST,
                HttpMethod.DELETE,
                HttpMethod.PUT,
                HttpMethod.PATCH
        ).map(HttpMethod::toString).toList());
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AuditEventRepository auditEventRepository() {
        return new InMemoryAuditEventRepository();
    }

    @Bean
    public JwtTokenService jwtTokenService() {
        return new JwtTokenServiceImpl();
    }
}