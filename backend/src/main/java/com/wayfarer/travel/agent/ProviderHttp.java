package com.wayfarer.travel.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.travel.api.ApiFailure;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ProviderHttp {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    ProviderHttp(ObjectMapper json) {
        this.json = json;
    }

    JsonNode call(HttpRequest request, String provider) {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status == 401 || status == 403) throw credentials(provider);
            if (status == 429) {
                throw new ApiFailure(503, "PROVIDER_QUOTA", provider + "调用额度不足或请求过多，请稍后再试。", true);
            }
            if (status < 200 || status >= 300 || response.body().length > 2_000_000) {
                throw unavailable(provider);
            }
            JsonNode data = json.readTree(response.body());
            if (data == null || !data.isObject()) throw unavailable(provider);
            return data;
        } catch (IOException ignored) {
            throw unavailable(provider);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable(provider);
        }
    }

    static ApiFailure unavailable(String provider) {
        return new ApiFailure(502, "PROVIDER_UNAVAILABLE", provider + "暂时没有返回可用结果，请重试。", true);
    }

    static ApiFailure credentials(String provider) {
        return new ApiFailure(503, "PROVIDER_CREDENTIALS", provider + "未授权，请让部署者检查密钥和接口权限。", false);
    }
}
