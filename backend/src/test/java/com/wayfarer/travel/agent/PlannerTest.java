package com.wayfarer.travel.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.wayfarer.travel.api.ApiFailure;
import com.wayfarer.travel.api.TravelModels.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {
    private static final City CITY = new City("杭州市", "330100");
    private static final Place LAKE = new Place("lake", "西湖", "", new Point(120.14, 30.24));
    private static final Place TEMPLE = new Place("temple", "灵隐寺", "", new Point(120.10, 30.24));
    private static final Place MARKET = new Place("market", "河坊街", "", new Point(120.17, 30.24));
    private static final Instant CHECKED = Instant.parse("2026-11-01T12:00:00Z");
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void callsRealAdapterContractAndRepairsTimeConflictUsingRouteDuration() {
        var calls = new ArrayList<String>();
        var modelTurns = new AtomicInteger();
        ChatModel model = (messages, toolsAllowed) -> switch (modelTurns.incrementAndGet()) {
            case 1 -> {
                assertTrue(toolsAllowed);
                yield tool("search-1", "灵隐寺");
            }
            case 2 -> {
                assertEquals("tool", messages.get(messages.size()-1).path("role").asText());
                assertTrue(messages.toString().contains("temple"));
                yield answer(draft(100, schedule(1, visit("lake", "09:00"), visit("temple", "10:00"))));
            }
            case 3 -> {
                assertTrue(messages.toString().contains("35 分钟"));
                yield answer(draft(100, schedule(1, visit("lake", "09:00"), visit("temple", "10:45"))));
            }
            default -> throw new AssertionError("Unbounded model requests");
        };
        var stages = new ArrayList<String>();
        Plan plan = new Planner(json, maps(calls), model).create(request(1, 300), stages::add);
        assertEquals(List.of("city", "search:西湖", "search:灵隐寺", "route:lake:temple"), calls);
        assertEquals(List.of("city", "places", "planning", "routing", "repairing", "routing"), stages);
        assertEquals(30, plan.days().getFirst().stops().getLast().inbound().minutes());
        assertEquals(List.of("lake"), plan.mustVisitIds());
        assertEquals(120, plan.budget().total());
    }

    @Test
    void overBudgetDraftIsRejectedEvenWhenTheModelRepeatsIt() {
        var calls = new ArrayList<String>();
        var attempts = new AtomicInteger();
        ChatModel model = (messages, tools) -> {
            if (attempts.incrementAndGet() == 2) assertTrue(messages.toString().contains("超过 100 元预算"));
            return answer(draft(101, schedule(1, visit("lake", "09:00"))));
        };
        var failure = assertThrows(ApiFailure.class,
            () -> new Planner(json, maps(calls), model).create(request(1, 100), ignored -> {}));
        assertEquals("PLAN_CONSTRAINTS", failure.code());
        assertEquals(2, attempts.get());
        assertFalse(calls.stream().anyMatch(call -> call.startsWith("route")));
    }

    @Test
    void revisionPreservesUntouchedDayAndLockedStopWhileAddingAVerifiedPlace() {
        var request = request(2, 300);
        Stop pinned = new Stop(LAKE, "09:00", "10:00", "保留这段备注", 30, true, CHECKED, null);
        Stop temple = new Stop(TEMPLE, "09:00", "10:00", "上午参观", 5, false, CHECKED, null);
        Day originalDayTwo = new Day(2, request.startDate().plusDays(1), "寺院之行", List.of(temple));
        Plan original = new Plan(request, CITY, "两日游", "原来的安排",
            List.of(new Day(1, request.startDate(), "湖畔", List.of(pinned)), originalDayTwo),
            new Budget(35, 10, 10, 10), List.of("lake"), List.of(), "", CHECKED);
        var calls = new ArrayList<String>();
        var attempts = new AtomicInteger();
        ChatModel model = (messages, tools) -> switch (attempts.incrementAndGet()) {
            case 1 -> answer(draft(1, schedule(1, visit("lake", "09:30"))));
            case 2 -> {
                assertTrue(messages.toString().contains("不能修改已锁定地点"));
                yield tool("search-2", "河坊街");
            }
            case 3 -> answer(draft(400, schedule(1, visit("lake", "09:00"), visit("market", "12:00"))));
            default -> throw new AssertionError();
        };
        Plan revised = new Planner(json, maps(calls), model).revise(original,
            new RevisionRequest(1, List.of(1), "添加河坊街"), ignored -> {});
        assertSame(originalDayTwo, revised.days().getLast());
        assertEquals(pinned, revised.days().getFirst().stops().getFirst());
        assertEquals("market", revised.days().getFirst().stops().getLast().place().id());
        assertEquals(75, revised.budget().total());
        assertEquals(List.of("search:河坊街", "route:lake:market"), calls);
    }

    @Test
    void providerErrorIsReportedWithoutASyntheticPlan() {
        var error = new ApiFailure(502, "PROVIDER_UNAVAILABLE", "模型不可用", true);
        ChatModel model = (messages, tools) -> { throw error; };
        assertSame(error, assertThrows(ApiFailure.class,
            () -> new Planner(json, maps(new ArrayList<>()), model).create(request(1, 300), ignored -> {})));
    }

    @Test
    void modelCannotInventAnUnknownPlaceId() {
        ChatModel model = (messages, tools) -> answer(draft(50, schedule(1, visit("invented", "09:00"))));
        ApiFailure failure = assertThrows(ApiFailure.class,
            () -> new Planner(json, maps(new ArrayList<>()), model).create(request(1, 300), ignored -> {}));
        assertEquals("PLAN_CONSTRAINTS", failure.code());
        assertTrue(failure.getMessage().contains("地点 ID 未经高德核实"));
    }

    private static Maps maps(List<String> calls) {
        return new Maps() {
            @Override public City city(String input) { calls.add("city"); return CITY; }
            @Override public List<Place> search(City city, String query) {
                assertEquals(CITY, city);
                calls.add("search:" + query);
                return List.of(switch (query) {
                    case "西湖" -> LAKE;
                    case "灵隐寺" -> TEMPLE;
                    case "河坊街" -> MARKET;
                    default -> throw new AssertionError("Unexpected query " + query);
                });
            }
            @Override public Route route(Place from, Place to) {
                calls.add("route:" + from.id() + ":" + to.id());
                return new Route(Mode.DRIVE, 30, 8000, List.of(from.point(), to.point()), CHECKED);
            }
        };
    }

    private Request request(int days, int budget) {
        return new Request("杭州市", LocalDate.of(2026, 12, 1), days, 2, budget,
            Pace.BALANCED, List.of(), List.of("西湖"), "");
    }

    private com.fasterxml.jackson.databind.JsonNode tool(String id, String query) {
        var reply = json.createObjectNode().putNull("content");
        var call = reply.putArray("tool_calls").addObject().put("id", id).put("type", "function");
        call.putObject("function").put("name", "search_places")
            .put("arguments", json.createObjectNode().put("query", query).toString());
        return reply;
    }

    private com.fasterxml.jackson.databind.JsonNode answer(String draft) {
        return json.createObjectNode().put("content", draft);
    }

    private static String draft(int food, String... days) {
        return """
            {"title":"杭州行程","summary":"安排了市内活动","changeSummary":"按要求调整地点",
             "food":%d,"lodging":0,"localTransport":0,"days":[%s]}
            """.formatted(food, String.join(",", days));
    }

    private static String schedule(int number, String... stops) {
        return """
            {"number":%d,"title":"今日安排","stops":[%s]}
            """.formatted(number, String.join(",", stops));
    }

    private static String visit(String id, String time) {
        return """
            {"placeId":"%s","start":"%s","minutes":60,"estimatedCost":10,"note":"游览"}
            """.formatted(id, time);
    }
}
