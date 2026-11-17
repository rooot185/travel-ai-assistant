package com.wayfarer.travel.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.wayfarer.travel.TravelApplication.Settings;
import com.wayfarer.travel.api.ApiFailure;
import com.wayfarer.travel.api.TravelModels.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class Amap implements Maps {
    private static final String HOST = "https://restapi.amap.com";
    private static final String PROVIDER = "高德地图";
    private static final Set<String> MUNICIPAL = Set.of("110000", "120000", "310000", "500000");
    private static final Set<String> AUTH_ERRORS = Set.of("10001", "10002", "10005", "10006", "10007", "10008", "10009", "10012");
    private static final Set<String> QUOTA_ERRORS = Set.of("10003", "10004", "10010", "10014", "10019", "10020", "10021", "10029");
    private final ProviderHttp http;
    private final Settings settings;

    Amap(ProviderHttp http, Settings settings) {
        this.http = http;
        this.settings = settings;
    }

    @Override
    public City city(String input) {
        var data = query("/v3/config/district", Map.of("keywords", input, "subdistrict", "0"));
        for (JsonNode district : data.path("districts")) {
            String adcode = district.path("adcode").asText();
            String name = district.path("name").asText();
            boolean supported = "city".equals(district.path("level").asText()) || MUNICIPAL.contains(adcode);
            if (supported && adcode.matches("[1-6][0-9]{5}") && (input.equals(name) || (input + "市").equals(name))) {
                return new City(name, adcode);
            }
        }
        throw ApiFailure.invalid("高德未匹配到该城市，请填写完整的中国大陆城市名称，例如杭州市。");
    }

    @Override
    public List<Place> search(City city, String term) {
        var data = query("/v5/place/text", Map.of("keywords", term, "region", city.adcode(),
            "city_limit", "true", "page_size", "6"));
        List<Place> found = new ArrayList<>();
        for (JsonNode poi : data.path("pois")) {
            String id = poi.path("id").asText();
            String name = poi.path("name").asText();
            Point point = coordinate(poi.path("location").asText());
            if (!id.isBlank() && !name.isBlank() && point != null) {
                found.add(new Place(id, name, poi.path("address").asText(""), point));
            }
        }
        return List.copyOf(found);
    }

    @Override
    public Route route(Place from, Place to) {
        Mode mode = distance(from.point(), to.point()) < 2_000 ? Mode.WALK : Mode.DRIVE;
        String suffix = mode == Mode.WALK ? "walking" : "driving";
        var data = query("/v5/direction/" + suffix, Map.of("origin", format(from.point()),
            "destination", format(to.point()), "show_fields", "cost,polyline"));
        var path = data.path("route").path("paths").path(0);
        int seconds = path.path("cost").path("duration").asInt(-1);
        int meters = path.path("distance").asInt(-1);
        if (seconds < 0 || seconds > 86_400 || meters < 0 || meters > 1_000_000) {
            throw new ApiFailure(422, "ROUTE_NOT_FOUND",
                "高德未返回有效路段：" + from.name() + " → " + to.name() + "。请调整地点后重试。", false);
        }
        List<Point> points = new ArrayList<>();
        for (JsonNode step : path.path("steps")) {
            for (String value : step.path("polyline").asText().split(";")) {
                Point point = coordinate(value);
                if (point != null && (points.isEmpty() || !points.getLast().equals(point))) points.add(point);
            }
        }
        if (points.size() > 600) {
            List<Point> reduced = new ArrayList<>();
            for (int i = 0; i < 600; i++) reduced.add(points.get(i * (points.size() - 1) / 599));
            points = reduced;
        }
        return new Route(mode, (seconds + 59) / 60, meters, List.copyOf(points), Instant.now());
    }

    private JsonNode query(String path, Map<String, String> parameters) {
        var uri = UriComponentsBuilder.fromUriString(HOST).path(path).queryParam("key", settings.amapKey());
        parameters.forEach((key, value) -> uri.queryParam(key, "{" + key + "}"));
        var request = HttpRequest.newBuilder(uri.encode().buildAndExpand(parameters).toUri())
            .timeout(Duration.ofSeconds(7)).GET().build();
        var data = http.call(request, PROVIDER);
        if ("1".equals(data.path("status").asText())) return data;
        String code = data.path("infocode").asText();
        if (AUTH_ERRORS.contains(code)) throw ProviderHttp.credentials(PROVIDER);
        if (QUOTA_ERRORS.contains(code)) {
            throw new ApiFailure(503, "MAP_QUOTA", "高德查询额度不足或请求过多，请稍后再试。", true);
        }
        throw ProviderHttp.unavailable(PROVIDER);
    }

    private static String format(Point point) {
        return String.format(java.util.Locale.ROOT, "%.6f,%.6f", point.lng(), point.lat());
    }

    private static Point coordinate(String value) {
        String[] pair = value.split(",");
        if (pair.length != 2) return null;
        try {
            double lng = Double.parseDouble(pair[0]);
            double lat = Double.parseDouble(pair[1]);
            if (!Double.isFinite(lng) || !Double.isFinite(lat)
                || Math.abs(lng) > 180 || Math.abs(lat) > 90) return null;
            return new Point(lng, lat);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static double distance(Point a, Point b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double sinLat = Math.sin((lat2 - lat1) / 2);
        double sinLng = Math.sin(Math.toRadians(b.lng() - a.lng()) / 2);
        return 12_742_000 * Math.asin(Math.min(1, Math.sqrt(sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLng * sinLng)));
    }
}
