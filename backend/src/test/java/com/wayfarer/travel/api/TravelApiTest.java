package com.wayfarer.travel.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.travel.agent.ChatModel;
import com.wayfarer.travel.agent.Maps;
import com.wayfarer.travel.api.TravelModels.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "TRAVEL_TEST_DB", matches = "jdbc:postgresql:.*")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "travel.ai-enabled=true", "travel.amap-key=test", "travel.model-key=test", "travel.secure-cookie=false"
})
class TravelApiTest {
    @LocalServerPort int port;
    private final RestTemplate http = new RestTemplate(new JdkClientHttpRequestFactory());
    @Autowired ObjectMapper json;
    @MockitoBean Maps maps;
    @MockitoBean ChatModel model;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("TRAVEL_TEST_DB"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("TRAVEL_TEST_DB_USER", "travel"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("TRAVEL_TEST_DB_PASSWORD", ""));
    }

    @BeforeEach
    void upstreamContracts() {
        http.setErrorHandler(new NoOpResponseErrorHandler());
        City city = new City("杭州市", "330100");
        when(maps.city("杭州市")).thenReturn(city);
        when(maps.search(city, "西湖")).thenReturn(List.of(new Place("lake", "西湖", "湖滨", new Point(120.15, 30.25))));
        when(model.reply(any(), anyBoolean())).thenReturn(json.createObjectNode().put("content", """
            {"title":"西湖一日","summary":"安排西湖游览","food":200,"lodging":0,"localTransport":50,
             "days":[{"number":1,"title":"湖畔","stops":[{"placeId":"lake","start":"09:00","minutes":60,
             "estimatedCost":0,"note":"游览"}]}]}
            """));
    }

    @Test
    void protectsSessionsAndPersistsOnlyOneTripForDuplicateRequest() throws InterruptedException {
        var registerWithoutOriginProof = http.postForEntity(base("/api/auth/register"), Map.of(
            "email", "x@example.com", "displayName", "路书测试", "password", "password-a-very-long-one"), JsonNode.class);
        assertEquals(403, registerWithoutOriginProof.getStatusCode().value());
        assertEquals(401, send("/api/trips", HttpMethod.GET, null, null, null).getStatusCode().value());

        String cookie = register();
        String other = register();
        Request request = new Request("杭州市", LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(2),
            1, 2, 500, Pace.BALANCED, List.of(), List.of("西湖"), "");
        UUID key = UUID.randomUUID();
        assertEquals(202, send("/api/trips", HttpMethod.POST, request, cookie, key).getStatusCode().value());
        assertEquals(202, send("/api/trips", HttpMethod.POST, request, cookie, key).getStatusCode().value());
        var job = awaitJob(cookie, key);
        assertEquals("COMPLETE", job.path("state").asText(), job.toString());
        String id = job.path("trip").path("id").asText();
        assertEquals(1, body(send("/api/trips", HttpMethod.GET, null, cookie, null)).size());
        assertEquals(404, send("/api/trips/" + id, HttpMethod.GET, null, other, null).getStatusCode().value());
        assertEquals(404, send("/api/jobs/" + key, HttpMethod.GET, null, other, null).getStatusCode().value());

        String stop = "/api/trips/" + id + "/stops/lake";
        var locked = body(send(stop, HttpMethod.PATCH, new LockRequest(1, true), cookie, null));
        assertEquals(2, locked.path("revision").asInt());
        assertTrue(locked.path("plan").path("days").path(0).path("stops").path(0).path("locked").asBoolean());
        assertEquals(409, send(stop, HttpMethod.PATCH, new LockRequest(1, false), cookie, null).getStatusCode().value());
        assertEquals(409, send("/api/trips/" + id + "?revision=1", HttpMethod.DELETE, null, cookie, null).getStatusCode().value());
        assertEquals(204, send("/api/trips/" + id + "?revision=2", HttpMethod.DELETE, null, cookie, null).getStatusCode().value());
        assertEquals(404, send("/api/trips/" + id, HttpMethod.GET, null, cookie, null).getStatusCode().value());
        assertEquals(200, send("/api/auth/logout", HttpMethod.POST, null, cookie, null).getStatusCode().value());
        assertEquals(401, send("/api/trips", HttpMethod.GET, null, cookie, null).getStatusCode().value());
    }

    private JsonNode awaitJob(String cookie, UUID id) throws InterruptedException {
        for (int attempt = 0; attempt < 40; attempt++) {
            JsonNode job = body(send("/api/jobs/" + id, HttpMethod.GET, null, cookie, null));
            if (List.of("FAILED", "COMPLETE").contains(job.path("state").asText())) return job;
            Thread.sleep(60);
        }
        throw new AssertionError("Planning job did not finish");
    }

    private String register() {
        var response = send("/api/auth/register", HttpMethod.POST, Map.of(
            "email", UUID.randomUUID() + "@example.com", "displayName", "路书测试",
            "password", "password-a-very-long-one"), null, null);
        assertEquals(200, response.getStatusCode().value(), () -> body(response).toString());
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookie);
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        return cookie.split(";", 2)[0];
    }

    private ResponseEntity<JsonNode> send(String url, HttpMethod method, Object body, String cookie, UUID key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Travel-Client", "web");
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null) headers.set(HttpHeaders.COOKIE, cookie);
        if (key != null) headers.set("Idempotency-Key", key.toString());
        return http.exchange(base(url), method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private String base(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private static JsonNode body(ResponseEntity<JsonNode> response) {
        assertNotNull(response.getBody());
        return response.getBody();
    }
}
