package com.wayfarer.travel.auth;

import com.wayfarer.travel.TravelApplication.Settings;
import com.wayfarer.travel.api.ApiFailure;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class AuthApi {
    public static final String SESSION = "travel_session";
    private final Accounts accounts;
    private final AbuseGuard guard;
    private final Settings settings;

    AuthApi(Accounts accounts, AbuseGuard guard, Settings settings) {
        this.accounts = accounts;
        this.guard = guard;
        this.settings = settings;
    }

    public record Status(boolean plannerConfigured, boolean registrationOpen) {}
    public record Identity(Accounts.Account account) {}
    public record Register(
        @NotBlank(message = "请输入称呼。") @Size(max = 40) String displayName,
        @NotBlank @Email(message = "请输入有效的邮箱地址。") @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password) {}
    public record Login(
        @NotBlank @Email(message = "请输入有效的邮箱地址。") @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password) {}

    @GetMapping("/status")
    Status status() {
        return new Status(settings.plannerReady(), settings.allowRegistration());
    }

    @GetMapping("/auth/me")
    Identity me(@CookieValue(value = SESSION, required = false) String token) {
        return new Identity(accounts.find(token));
    }

    @PostMapping("/auth/register")
    Identity register(@Valid @RequestBody Register body, HttpServletRequest request, HttpServletResponse response) {
        if (!settings.allowRegistration()) {
            throw new ApiFailure(403, "REGISTRATION_CLOSED", "此实例暂未开放注册。", false);
        }
        guard.take("signup:" + request.getRemoteAddr(), 5, Duration.ofMinutes(15));
        return establish(accounts.register(body.displayName(), body.email(), body.password()), response);
    }

    @PostMapping("/auth/login")
    Identity login(@Valid @RequestBody Login body, HttpServletRequest request, HttpServletResponse response) {
        guard.take("login:ip:" + request.getRemoteAddr(), 30, Duration.ofMinutes(15));
        guard.take("login:email:" + body.email().strip().toLowerCase(Locale.ROOT), 10, Duration.ofMinutes(15));
        return establish(accounts.login(body.email(), body.password()), response);
    }

    @PostMapping("/auth/logout")
    Identity logout(@CookieValue(value = SESSION, required = false) String token, HttpServletResponse response) {
        accounts.logout(token);
        cookie(response, "", Duration.ZERO);
        return new Identity(null);
    }

    private Identity establish(Accounts.Session session, HttpServletResponse response) {
        cookie(response, session.token(), Accounts.SESSION_LIFE);
        return new Identity(session.account());
    }

    private void cookie(HttpServletResponse response, String token, Duration lifetime) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(SESSION, token)
            .httpOnly(true).secure(settings.secureCookie()).sameSite("Lax")
            .path("/").maxAge(lifetime).build().toString());
    }
}
