package com.wayfarer.travel.api;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TravelModels {
    private TravelModels() {}

    public enum Pace { RELAXED, BALANCED, FULL }
    public enum Mode { WALK, DRIVE }

    public record Request(
        @NotBlank(message = "请输入目的地城市。")
        @Pattern(regexp = "[\\p{IsHan}·]{2,16}", message = "请输入中国大陆城市的中文名称。") String city,
        @NotNull(message = "请选择出发日期。") LocalDate startDate,
        @Min(value = 1, message = "至少安排一天。")
        @Max(value = 7, message = "单次最多安排七天。") int days,
        @Min(value = 1, message = "至少一位同行者。")
        @Max(value = 12, message = "最多十二位同行者。") int travelers,
        @Min(value = 100, message = "当地预算至少为 100 元。")
        @Max(value = 200000, message = "当地预算最多为 20 万元。") int budget,
        @NotNull(message = "请选择旅行节奏。") Pace pace,
        @NotNull @Size(max = 6) List<@NotBlank @Size(max = 20) String> preferences,
        @NotNull @Size(max = 5, message = "最多填写五个必去地点。")
        List<@NotBlank @Size(max = 40) String> mustVisit,
        @Size(max = 300, message = "补充要求最多 300 字。") String notes
    ) {}

    public record RevisionRequest(
        @Min(1) int revision,
        @NotEmpty(message = "请选择需要调整的日期。") @Size(max = 7)
        List<@NotNull @Min(1) @Max(7) Integer> days,
        @NotBlank(message = "请输入调整要求。") @Size(max = 400) String instruction
    ) {}

    public record LockRequest(@Min(1) int revision, boolean locked) {}

    public record Point(double lng, double lat) {}
    public record Place(String id, String name, String address, Point point) {}
    public record City(String name, String adcode) {}
    public record Route(Mode mode, int minutes, int meters, List<Point> line, Instant checkedAt) {}
    public record Stop(Place place, String start, String end, String note, int estimatedCost,
                       boolean locked, Instant placeCheckedAt, Route inbound) {}
    public record Day(int number, LocalDate date, String title, List<Stop> stops) {}
    public record Budget(int activities, int food, int lodging, int localTransport) {
        public long total() { return (long) activities + food + lodging + localTransport; }
    }
    public record Evidence(String step, int calls) {}
    public record Plan(Request request, City city, String title, String summary, List<Day> days,
                       Budget budget, List<String> mustVisitIds, List<Evidence> evidence,
                       String changeSummary, Instant generatedAt) {}
    public record Trip(UUID id, int revision, Instant updatedAt, Plan plan) {}
    public record TripSummary(UUID id, int revision, Instant updatedAt, String city, String title,
                              LocalDate startDate, int days) {}
}
