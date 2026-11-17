package com.wayfarer.travel.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.wayfarer.travel.TravelApplication.Settings;
import com.wayfarer.travel.api.ApiFailure;
import com.wayfarer.travel.api.TravelModels.Mode;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProviderAdaptersTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void mapsOnlyVerifiedPoiAndTransformsProviderSecondsAndPolyline() throws Exception {
        ProviderHttp http = mock(ProviderHttp.class);
        JsonNode district = json.readTree("""
            {"status":"1","districts":[{"name":"杭州市","adcode":"330100","level":"city"}]}
            """);
        JsonNode places = json.readTree("""
            {"status":"1","pois":[{"id":"lake","name":"西湖","address":"西湖区","location":"120.14,30.244"},
             {"id":"broken","name":"未定位","location":[]},
             {"id":"park","name":"柳浪闻莺","location":"120.146,30.24"}]}
            """);
        JsonNode route = json.readTree("""
            {"status":"1","route":{"paths":[{"distance":"940","cost":{"duration":"645"},
             "steps":[{"polyline":"120.14,30.244;120.146,30.24"}]}]}}
            """);
        when(http.call(any(), eq("高德地图"))).thenAnswer(call -> {
            HttpRequest request = call.getArgument(0);
            var uri = request.uri();
            return switch (uri.getPath()) {
                case "/v3/config/district" -> district;
                case "/v5/place/text" -> {
                    assertTrue(uri.getQuery().contains("keywords=西湖"));
                    assertTrue(uri.getQuery().contains("region=330100"));
                    assertTrue(uri.getQuery().contains("city_limit=true"));
                    yield places;
                }
                case "/v5/direction/walking" -> {
                    assertTrue(uri.getQuery().contains("show_fields=cost,polyline"));
                    yield route;
                }
                default -> throw new AssertionError(uri.getPath());
            };
        });
        Amap amap = new Amap(http, settings("https://example.test/chat/completions"));
        var city = amap.city("杭州");
        var matching = amap.search(city, "西湖");
        assertEquals(2, matching.size());
        var result = amap.route(matching.getFirst(), matching.getLast());
        assertEquals(Mode.WALK, result.mode());
        assertEquals(11, result.minutes());
        assertEquals(940, result.meters());
        assertEquals(2, result.line().size());
    }

    @Test
    void compatibleModelSendsToolsAndTreatsProviderFailureAsFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var captured = new AtomicReference<JsonNode>();
        server.createContext("/chat/completions", exchange -> {
            try (exchange) {
                captured.set(json.readTree(exchange.getRequestBody()));
                byte[] result = """
                    {"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"{}"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, result.length);
                exchange.getResponseBody().write(result);
            }
        });
        server.createContext("/quota", exchange -> {
            try (exchange) { exchange.sendResponseHeaders(429, -1); }
        });
        server.start();
        try {
            var transport = new ProviderHttp(json);
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var model = new CompatibleChat(json, transport, settings(base + "/chat/completions"));
            var messages = json.createArrayNode();
            messages.addObject().put("role", "user").put("content", "JSON");
            assertEquals("{}", model.reply(messages, true).path("content").asText());
            assertEquals("search_places", captured.get().path("tools").path(0).path("function").path("name").asText());
            assertEquals("json_object", captured.get().path("response_format").path("type").asText());
            model.reply(messages, false);
            assertFalse(captured.get().has("tools"));
            var limited = new CompatibleChat(json, transport, settings(base + "/quota"));
            ApiFailure failure = assertThrows(ApiFailure.class, () -> limited.reply(messages, true));
            assertEquals("PROVIDER_QUOTA", failure.code());
            assertTrue(failure.retryable());
        } finally {
            server.stop(0);
        }
    }

    private static Settings settings(String endpoint) {
        return new Settings(true, true, true, endpoint, "test-model", "test-key", "test-amap-key");
    }
}
