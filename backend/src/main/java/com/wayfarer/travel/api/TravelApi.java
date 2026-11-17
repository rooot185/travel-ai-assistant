package com.wayfarer.travel.api;

import com.wayfarer.travel.TravelApplication.Settings;
import com.wayfarer.travel.agent.Planner;
import com.wayfarer.travel.api.TravelModels.*;
import com.wayfarer.travel.auth.Accounts;
import com.wayfarer.travel.auth.AuthApi;
import com.wayfarer.travel.store.Trips;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TravelApi {
    private static final String SESSION = AuthApi.SESSION;
    private final Accounts accounts;
    private final Trips trips;
    private final Planner planner;
    private final PlanningJobs jobs;
    private final Settings settings;

    TravelApi(Accounts accounts, Trips trips, Planner planner, PlanningJobs jobs, Settings settings) {
        this.accounts = accounts;
        this.trips = trips;
        this.planner = planner;
        this.jobs = jobs;
        this.settings = settings;
    }

    private record RevisionJob(UUID trip, RevisionRequest request) {}

    @GetMapping("/trips")
    List<TripSummary> list(@CookieValue(value = SESSION, required = false) String token,
                           @RequestParam(defaultValue = "0") int offset) {
        if (offset < 0 || offset > 10_000) throw ApiFailure.invalid("分页位置超出范围。");
        return trips.list(accounts.require(token).id(), offset);
    }

    @PostMapping("/trips")
    @ResponseStatus(HttpStatus.ACCEPTED)
    PlanningJobs.Snapshot create(@CookieValue(value = SESSION, required = false) String token,
                                 @RequestHeader("Idempotency-Key") UUID jobId, @Valid @RequestBody Request request,
                                 HttpServletRequest servlet) {
        UUID account = accounts.require(token).id();
        ready();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if (request.startDate().isBefore(today) || request.startDate().isAfter(today.plusYears(1))) {
            throw ApiFailure.invalid("请选择今天起一年内的出发日期。");
        }
        return jobs.submit(account, jobId, request, servlet.getRemoteAddr(), progress -> {
            Plan plan = planner.create(request, progress);
            progress.accept("saving");
            return trips.create(account, plan);
        });
    }

    @GetMapping("/trips/{id}")
    Trip get(@CookieValue(value = SESSION, required = false) String token, @PathVariable UUID id) {
        return trips.get(accounts.require(token).id(), id);
    }

    @PostMapping("/trips/{id}/revisions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    PlanningJobs.Snapshot revise(@CookieValue(value = SESSION, required = false) String token,
                                 @RequestHeader("Idempotency-Key") UUID jobId, @PathVariable UUID id,
                                 @Valid @RequestBody RevisionRequest request, HttpServletRequest servlet) {
        UUID account = accounts.require(token).id();
        ready();
        // Replayed requests must be looked up before the optimistic version check.
        return jobs.submit(account, jobId, new RevisionJob(id, request), servlet.getRemoteAddr(), progress -> {
            Trip current = trips.get(account, id);
            requireRevision(current, request.revision());
            if (request.days().stream().anyMatch(day -> day > current.plan().days().size())) {
                throw ApiFailure.invalid("请选择行程中已有的日期。");
            }
            var plan = planner.revise(current.plan(), request, progress);
            progress.accept("saving");
            return trips.update(account, id, request.revision(), plan);
        });
    }

    @PatchMapping("/trips/{id}/stops/{stopId}")
    Trip lock(@CookieValue(value = SESSION, required = false) String token, @PathVariable UUID id,
              @PathVariable String stopId, @Valid @RequestBody LockRequest request) {
        UUID account = accounts.require(token).id();
        Trip current = trips.get(account, id);
        requireRevision(current, request.revision());
        Plan old = current.plan();
        boolean exists = old.days().stream().flatMap(day -> day.stops().stream())
            .anyMatch(stop -> stop.place().id().equals(stopId));
        if (!exists) throw ApiFailure.missing();
        List<Day> days = old.days().stream().map(day -> new Day(day.number(), day.date(), day.title(),
            day.stops().stream().map(stop -> stop.place().id().equals(stopId)
                ? new Stop(stop.place(), stop.start(), stop.end(), stop.note(), stop.estimatedCost(), request.locked(),
                    stop.placeCheckedAt(), stop.inbound()) : stop).toList())).toList();
        Plan plan = new Plan(old.request(), old.city(), old.title(), old.summary(), days, old.budget(), old.mustVisitIds(),
            old.evidence(), old.changeSummary(), old.generatedAt());
        return trips.update(account, id, current.revision(), plan);
    }

    @DeleteMapping("/trips/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CookieValue(value = SESSION, required = false) String token, @PathVariable UUID id,
                @RequestParam int revision) {
        trips.delete(accounts.require(token).id(), id, revision);
    }

    @GetMapping("/jobs/{id}")
    PlanningJobs.Snapshot job(@CookieValue(value = SESSION, required = false) String token, @PathVariable UUID id) {
        return jobs.get(accounts.require(token).id(), id);
    }

    private void ready() {
        if (!settings.plannerReady()) {
            throw new ApiFailure(503, "PLANNER_NOT_CONFIGURED", "此实例尚未配置实时模型和高德服务。", false);
        }
    }

    private static void requireRevision(Trip current, int revision) {
        if (current.revision() != revision) throw ApiFailure.conflict("行程版本有变化，请刷新后重试。");
    }
}
