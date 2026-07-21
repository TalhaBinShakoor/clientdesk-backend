package com.clientdesk.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final SecurityAuditLogger securityAuditLogger;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            LoginAttemptLimiter loginAttemptLimiter,
            SecurityAuditLogger securityAuditLogger
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.securityAuditLogger = securityAuditLogger;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String email = requestBody.email().trim();
        String clientIp = request.getRemoteAddr();
        if (!loginAttemptLimiter.isAllowed(email, clientIp)) {
            long retryAfterSeconds = loginAttemptLimiter.retryAfterSeconds(email, clientIp);
            response.setHeader(
                    HttpHeaders.RETRY_AFTER,
                    Long.toString(retryAfterSeconds)
            );
            securityAuditLogger.loginRateLimited(email, clientIp, retryAfterSeconds);
            throw new ResponseStatusException(
                    TOO_MANY_REQUESTS,
                    "Too many login attempts. Try again later"
            );
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email,
                            requestBody.password()
                    )
            );
        } catch (AuthenticationException exception) {
            loginAttemptLimiter.recordFailure(email, clientIp);
            securityAuditLogger.loginFailed(email, clientIp);
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid email or password");
        }

        loginAttemptLimiter.recordSuccess(email, clientIp);

        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        securityAuditLogger.loginSucceeded(user, clientIp);
        return AuthResponse.from(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        securityAuditLogger.logout(authentication);
        logoutHandler.logout(request, response, authentication);
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication required");
        }
        return AuthResponse.from(user);
    }
}
