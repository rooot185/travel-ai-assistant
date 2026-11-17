package com.wayfarer.travel.api;

import com.wayfarer.travel.api.ApiErrors.ErrorBody;
import com.wayfarer.travel.api.TravelModels.Trip;
import com.wayfarer.travel.auth.AbuseGuard;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class PlanningJobs {
    private static final Logger LOG = LoggerFactory.getLogger(PlanningJobs.class);
    private static final Duration RETENTION = Duration.ofHours(2);
    private static final int MAX_RETAINED = 200;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(8), Thread.ofPlatform().name("planner-", 1).factory());
    private final Map<Key, Job> jobs = new HashMap<>();
    private final AbuseGuard guard;

    PlanningJobs(AbuseGuard guard) {
        this.guard = guard;
    }

    public enum State { QUEUED, RUNNING, COMPLETE, FAILED }
    public record Snapshot(UUID id, State state, String step, Trip trip, ErrorBody error) {}
    private record Key(UUID account, UUID id) {}
    private static final class Job {
        final Object request;
        volatile Snapshot current;
        volatile Instant expiresAt;

        Job(Object request, Snapshot current) {
            this.request = request;
            this.current = current;
        }
    }

    public synchronized Snapshot submit(UUID account, UUID id, Object request, String ip,
                                        Function<Consumer<String>, Trip> work) {
        prune();
        Key key = new Key(account, id);
        Job existing = jobs.get(key);
        if (existing != null) {
            if (!existing.request.equals(request)) {
                throw new ApiFailure(409, "IDEMPOTENCY_CONFLICT", "此请求标识已用于其他规划操作。", false);
            }
            return existing.current;
        }
        if (jobs.size() >= MAX_RETAINED || executor.getQueue().remainingCapacity() == 0) throw busy();
        boolean alreadyPlanning = jobs.entrySet().stream().anyMatch(entry -> entry.getKey().account().equals(account)
            && Set.of(State.QUEUED, State.RUNNING).contains(entry.getValue().current.state()));
        if (alreadyPlanning) {
            throw new ApiFailure(409, "PLAN_IN_PROGRESS", "已有行程正在处理，请等待完成。", true);
        }
        guard.take("plan:ip:" + ip, 20, Duration.ofHours(1));
        guard.take("plan:account:" + account, 8, Duration.ofHours(1));
        var job = new Job(request, new Snapshot(id, State.QUEUED, "queued", null, null));
        jobs.put(key, job);
        try {
            executor.execute(() -> run(id, job, work));
        } catch (RejectedExecutionException full) {
            jobs.remove(key);
            throw busy();
        }
        return job.current;
    }

    public synchronized Snapshot get(UUID account, UUID id) {
        Job job = jobs.get(new Key(account, id));
        if (job == null || (job.expiresAt != null && job.expiresAt.isBefore(Instant.now()))) throw ApiFailure.missing();
        return job.current;
    }

    private void run(UUID id, Job job, Function<Consumer<String>, Trip> work) {
        job.current = new Snapshot(id, State.RUNNING, "planning", null, null);
        long started = System.nanoTime();
        try {
            Trip trip = work.apply(step -> job.current = new Snapshot(id, State.RUNNING, step, null, null));
            job.current = new Snapshot(id, State.COMPLETE, "saved", trip, null);
            LOG.info("Planning complete job={} modelCalls={} elapsedMs={}", id, trip.plan().evidence().stream()
                .filter(e -> e.step().equals("model_call")).mapToInt(e -> e.calls()).sum(), elapsed(started));
        } catch (ApiFailure failure) {
            job.current = new Snapshot(id, State.FAILED, job.current.step(), null, ErrorBody.from(failure));
            LOG.info("Planning failed job={} step={} code={} elapsedMs={}", id,
                job.current.step(), failure.code(), elapsed(started));
        } catch (RuntimeException unexpected) {
            LOG.error("Planning failed job={} step={} type={}", id, job.current.step(), unexpected.getClass().getSimpleName());
            job.current = new Snapshot(id, State.FAILED, job.current.step(), null,
                new ErrorBody("INTERNAL_ERROR", "处理行程时发生错误，请重试。", true));
        } finally {
            job.expiresAt = Instant.now().plus(RETENTION);
        }
    }

    @Scheduled(fixedDelayString = "PT5M")
    synchronized void prune() {
        Instant now = Instant.now();
        jobs.values().removeIf(job -> job.expiresAt != null && job.expiresAt.isBefore(now));
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }

    private static long elapsed(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start); }
    private static ApiFailure busy() {
        return new ApiFailure(503, "PLANNER_BUSY", "规划服务当前繁忙，请稍后重试。", true);
    }
}
