package com.wayfarer.travel.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.travel.api.ApiFailure;
import com.wayfarer.travel.api.TravelModels.Plan;
import com.wayfarer.travel.api.TravelModels.Trip;
import com.wayfarer.travel.api.TravelModels.TripSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class Trips {
    private final JdbcTemplate db;
    private final ObjectMapper json;

    Trips(JdbcTemplate db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    public Trip create(UUID account, Plan plan) {
        UUID id = UUID.randomUUID();
        return db.queryForObject("""
            insert into trips (id, account_id, revision, plan) values (?, ?, 1, cast(? as jsonb))
            returning id, revision, plan, updated_at
            """, this::trip, id, account, serialize(plan));
    }

    public List<TripSummary> list(UUID account, int offset) {
        return db.query("""
            select id, revision, updated_at, plan #>> '{city,name}' as city, plan ->> 'title' as title,
                (plan #>> '{request,startDate}')::date as start_date,
                (plan #>> '{request,days}')::integer as days
            from trips where account_id = ? order by updated_at desc, id limit 25 offset ?
            """, (rs, row) -> new TripSummary(rs.getObject("id", UUID.class), rs.getInt("revision"),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant(), rs.getString("city"),
                rs.getString("title"), rs.getObject("start_date", LocalDate.class), rs.getInt("days")),
            account, offset);
    }

    public Trip get(UUID account, UUID id) {
        return db.query("select id, revision, plan, updated_at from trips where account_id = ? and id = ?",
            this::trip, account, id).stream().findFirst().orElseThrow(ApiFailure::missing);
    }

    public Trip update(UUID account, UUID id, int revision, Plan plan) {
        return db.query("""
            update trips set plan = cast(? as jsonb), revision = revision + 1, updated_at = now()
            where account_id = ? and id = ? and revision = ? returning id, revision, plan, updated_at
            """, this::trip, serialize(plan), account, id, revision).stream().findFirst().orElseThrow(() -> {
                get(account, id);
                return ApiFailure.conflict("行程已更新，请打开最新版本后再提交。");
            });
    }

    public void delete(UUID account, UUID id, int revision) {
        if (db.update("delete from trips where account_id = ? and id = ? and revision = ?", account, id, revision) == 0) {
            get(account, id);
            throw ApiFailure.conflict("行程已更新，请打开最新版本后再删除。");
        }
    }

    private Trip trip(ResultSet rs, int row) throws SQLException {
        try {
            return new Trip(rs.getObject("id", UUID.class), rs.getInt("revision"),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                json.readValue(rs.getString("plan"), Plan.class));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Invalid stored trip data", failure);
        }
    }

    private String serialize(Plan plan) {
        try {
            return json.writeValueAsString(plan);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not serialize trip", failure);
        }
    }
}
