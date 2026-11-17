package com.wayfarer.travel.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.wayfarer.travel.TravelApplication.Settings;
import com.wayfarer.travel.api.ApiFailure;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class CompatibleChat implements ChatModel {
    private final ObjectMapper json;
    private final ProviderHttp http;
    private final Settings settings;

    CompatibleChat(ObjectMapper json, ProviderHttp http, Settings settings) {
        this.json = json;
        this.http = http;
        this.settings = settings;
    }

    @Override
    public JsonNode reply(ArrayNode messages, boolean toolsAllowed) {
        URI endpoint;
        try {
            endpoint = URI.create(settings.modelUrl());
        } catch (IllegalArgumentException invalid) {
            throw new ApiFailure(503, "MODEL_CONFIG", "模型接口地址格式无效，请检查部署配置。", false);
        }
        String host = endpoint.getHost();
        boolean local = host != null && List.of("localhost", "127.0.0.1", "[::1]").contains(host);
        if (host == null || (!"https".equals(endpoint.getScheme()) && !(local && "http".equals(endpoint.getScheme())))) {
            throw new ApiFailure(503, "MODEL_CONFIG", "模型地址必须使用 HTTPS。", false);
        }
        var requestBody = json.createObjectNode().put("model", settings.modelName()).put("max_tokens", 4500);
        requestBody.set("messages", messages);
        requestBody.putObject("response_format").put("type", "json_object");
        if ("api.deepseek.com".equals(endpoint.getHost())) {
            requestBody.putObject("thinking").put("type", "disabled");
        }
        if (toolsAllowed) {
            requestBody.set("tools", json.valueToTree(List.of(Map.of("type", "function", "function", Map.of(
                "name", "search_places",
                "description", "Search live Amap records within the selected city for one place or category.",
                "parameters", Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")),
                    "required", List.of("query"), "additionalProperties", false))))));
        }
        var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(35))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + settings.modelKey())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString())).build();
        var choice = http.call(request, "模型服务").path("choices").path(0);
        if ("length".equals(choice.path("finish_reason").asText())) {
            throw new ApiFailure(502, "MODEL_TRUNCATED", "模型结果超出长度限制，请缩短行程或减少补充要求。", false);
        }
        var message = choice.path("message");
        if (!message.isObject()) throw ProviderHttp.unavailable("模型服务");
        return message;
    }
}
