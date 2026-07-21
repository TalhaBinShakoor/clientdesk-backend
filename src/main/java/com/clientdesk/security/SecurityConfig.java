package com.clientdesk.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String[] BUSINESS_RESOURCES = {
            "/api/clients", "/api/clients/**",
            "/api/work-requests", "/api/work-requests/**",
            "/api/project-tasks", "/api/project-tasks/**",
            "/api/comments", "/api/comments/**",
            "/api/activity-events", "/api/activity-events/**",
            "/api/request-attachments", "/api/request-attachments/**",
            "/api/quotes", "/api/quotes/**"
    };

    private static final String[] CLIENT_SUBMISSION_ROUTES = {
            "/api/work-requests",
            "/api/comments",
            "/api/request-attachments"
    };

    private static final String[] TEAM_CREATE_ROUTES = {
            "/api/clients",
            "/api/project-tasks",
            "/api/quotes",
            "/api/ai-assistant/**"
    };

    private static final String[] TEAM_CHANGE_ROUTES = {
            "/api/clients/**",
            "/api/work-requests/**",
            "/api/project-tasks/**",
            "/api/quotes/**"
    };

    private static final String[] ADMIN_DELETE_ROUTES = {
            "/api/clients/**",
            "/api/work-requests/**",
            "/api/project-tasks/**",
            "/api/quotes/**"
    };

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    FilterRegistrationBean<ApiRateLimitFilter> apiRateLimitFilterRegistration(
            ApiRateLimitFilter apiRateLimitFilter
    ) {
        FilterRegistrationBean<ApiRateLimitFilter> registration = new FilterRegistrationBean<>(apiRateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${clientdesk.frontend.origin}") String frontendOrigin
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CorsConfigurationSource corsConfigurationSource,
            ApiRateLimitFilter apiRateLimitFilter,
            SecurityAuditLogger securityAuditLogger,
            @Value("${server.servlet.session.cookie.secure}") boolean secureCookies
    ) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .secure(secureCookies)
                .sameSite("Lax")
        );

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(permissions -> permissions.policy(
                                "camera=(), geolocation=(), microphone=(), payment=(), usb=()"
                        ))
                )
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            securityAuditLogger.authenticationRequired(
                                    request.getMethod(),
                                    request.getRequestURI()
                            );
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            securityAuditLogger.routeAuthorizationDenied(
                                    SecurityContextHolder.getContext().getAuthentication(),
                                    request.getMethod(),
                                    request.getRequestURI()
                            );
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                        })
                )
                .addFilterAfter(apiRateLimitFilter, AuthorizationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/status", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, BUSINESS_RESOURCES).authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/ai-assistant/**")
                                .hasAnyRole("ADMIN", "TEAM_MEMBER")
                        .requestMatchers(HttpMethod.POST, CLIENT_SUBMISSION_ROUTES).authenticated()
                        .requestMatchers(HttpMethod.POST, TEAM_CREATE_ROUTES).hasAnyRole("ADMIN", "TEAM_MEMBER")
                        .requestMatchers(HttpMethod.PUT, TEAM_CHANGE_ROUTES).hasAnyRole("ADMIN", "TEAM_MEMBER")
                        .requestMatchers(HttpMethod.PATCH, TEAM_CHANGE_ROUTES).hasAnyRole("ADMIN", "TEAM_MEMBER")
                        .requestMatchers(HttpMethod.DELETE, ADMIN_DELETE_ROUTES).hasRole("ADMIN")
                        .anyRequest().denyAll()
                )
                .build();
    }
}
