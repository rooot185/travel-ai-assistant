package com.wayfarer.travel.auth;

import com.wayfarer.travel.api.ApiFailure;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class Accounts {
    public static final Duration SESSION_LIFE = Duration.ofDays(7);
    public record Account(UUID id, String email, String displayName) {}
    public record Session(Account account, String token) {}
    private record Credential(Account account, String passwordHash) {}

    private final JdbcTemplate db;
    private final SecureRandom random = new SecureRandom();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final String dummyHash = encoder.encode(UUID.randomUUID().toString());

    Accounts(JdbcTemplate db) {
        this.db = db;
    }

    @Transactional
    public Session register(String name, String email, String password) {
        validatePassword(password, true);
        var account = new Account(UUID.randomUUID(), normalize(email), name.strip());
        try {
            db.update("insert into accounts (id, email, display_name, password_hash) values (?, ?, ?, ?)",
                account.id(), account.email(), account.displayName(), encoder.encode(password));
        } catch (DuplicateKeyException duplicate) {
            throw new ApiFailure(409, "ACCOUNT_EXISTS", "邮箱已注册，请直接登录。", false);
        }
        return issueSession(account);
    }

    public Session login(String email, String password) {
        validatePassword(password, false);
        var credential = db.query("""
            select id, email, display_name, password_hash from accounts where email = ?
            """, (rs, row) -> new Credential(new Account(rs.getObject("id", UUID.class),
                rs.getString("email"), rs.getString("display_name")), rs.getString("password_hash")),
            normalize(email)).stream().findFirst().orElse(null);
        boolean valid = encoder.matches(password, credential == null ? dummyHash : credential.passwordHash());
        if (!valid || credential == null) {
            throw new ApiFailure(401, "INVALID_LOGIN", "邮箱或密码有误。", false);
        }
        return issueSession(credential.account());
    }

    public Account require(String token) {
        var account = find(token);
        if (account == null) {
            throw new ApiFailure(401, "SIGN_IN_REQUIRED", "请先登录，再继续规划。", false);
        }
        return account;
    }

    public Account find(String token) {
        if (!validTokenFormat(token)) return null;
        return db.query("""
            select a.id, a.email, a.display_name
            from sessions s join accounts a on a.id = s.account_id
            where s.token_hash = ? and s.expires_at > now()
            """, (rs, row) -> new Account(rs.getObject("id", UUID.class),
                rs.getString("email"), rs.getString("display_name")), hash(token))
            .stream().findFirst().orElse(null);
    }

    public void logout(String token) {
        if (validTokenFormat(token)) db.update("delete from sessions where token_hash = ?", hash(token));
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void purgeExpired() {
        db.update("delete from sessions where expires_at <= now()");
    }

    private Session issueSession(Account account) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        db.update("insert into sessions values (?, ?, now() + (? * interval '1 second'))",
            hash(token), account.id(), SESSION_LIFE.toSeconds());
        return new Session(account, token);
    }

    private static void validatePassword(String password, boolean registering) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72
            || (registering && password.codePointCount(0, password.length()) < 10)) {
            throw ApiFailure.invalid("密码至少 10 个字符且不得超过 72 字节。");
        }
    }

    private static boolean validTokenFormat(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }

    private static byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(unavailable);
        }
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
