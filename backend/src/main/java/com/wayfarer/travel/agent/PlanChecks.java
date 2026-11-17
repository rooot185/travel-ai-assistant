package com.wayfarer.travel.agent;

import com.wayfarer.travel.api.TravelModels.*;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class PlanChecks {
    private PlanChecks() {}

    static Set<String> validate(List<Day> days, Budget budget, Set<String> mustVisitIds, int limit) {
        Set<String> failures = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        for (Day day : days) {
            Stop previous = null;
            for (Stop stop : day.stops()) {
                if (!visited.add(stop.place().id())) failures.add("地点重复：" + stop.place().name());
                if (previous != null && stop.inbound() != null) {
                    long gap = LocalTime.parse(previous.end()).until(LocalTime.parse(stop.start()), ChronoUnit.MINUTES);
                    if (gap < stop.inbound().minutes() + 5L) {
                        failures.add("第 " + day.number() + " 天从 " + previous.place().name() + " 到 "
                            + stop.place().name() + " 至少需要 " + (stop.inbound().minutes() + 5) + " 分钟（含缓冲）。");
                    }
                }
                previous = stop;
            }
        }
        if (!visited.containsAll(mustVisitIds)) failures.add("缺少必去地点 ID：" + missing(mustVisitIds, visited));
        if (budget.total() > limit) failures.add("估算共 " + budget.total() + " 元，超过 " + limit + " 元预算。");
        return failures;
    }

    private static Set<String> missing(Set<String> expected, Set<String> actual) {
        Set<String> missing = new LinkedHashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }
}
