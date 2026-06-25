package com.finsight.application.analytics;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class ProfileCurrentRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProfileCurrentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Map<String, Object>> findPayload(String userId) {
        return jdbcTemplate.query(
                "select as_of_date, overall_score, confidence, sample_months, payload_json, stale, "
                        + "computed_at, compute_duration_ms, error_message, profile_version "
                        + "from fin_profile_current where user_id = ?",
                rs -> {
                    if (!rs.next()) {
                        return Optional.<Map<String, Object>>empty();
                    }
                    String json = rs.getString("payload_json");
                    Map<String, Object> payload = json == null || json.isBlank()
                            ? new LinkedHashMap<>()
                            : JSON.parseObject(json, new TypeReference<Map<String, Object>>() { });
                    payload.put("materialized", true);
                    payload.put("stale", rs.getBoolean("stale"));
                    payload.put("computedAt", rs.getTimestamp("computed_at").toInstant().toString());
                    payload.put("computeDurationMs", rs.getObject("compute_duration_ms"));
                    payload.put("profileVersion", rs.getString("profile_version"));
                    if (rs.getString("error_message") != null) {
                        payload.put("errorMessage", rs.getString("error_message"));
                    }
                    if (!payload.containsKey("asOf") && rs.getDate("as_of_date") != null) {
                        payload.put("asOf", rs.getDate("as_of_date").toLocalDate().toString());
                    }
                    return Optional.of(payload);
                },
                userId);
    }

    public void upsert(String userId, Map<String, Object> payload, long durationMs) {
        Map<String, Object> stored = new LinkedHashMap<>(payload);
        stored.remove("materialized");
        stored.remove("stale");
        stored.remove("computedAt");
        stored.remove("computeDurationMs");
        stored.remove("profileVersion");
        stored.remove("errorMessage");
        stored.remove("needsRefresh");

        Object overall = payload.get("overallScore");
        Object confidence = payload.get("confidence");
        Object sampleMonths = payload.get("sampleMonths");
        String asOf = String.valueOf(payload.getOrDefault("asOf", LocalDate.now().toString()));

        jdbcTemplate.update(
                "insert into fin_profile_current "
                        + "(user_id, as_of_date, overall_score, confidence, sample_months, payload_json, stale, "
                        + "computed_at, compute_duration_ms, error_message, profile_version) "
                        + "values (?, ?, ?, ?, ?, ?, 0, ?, ?, null, 'v2.0.2') "
                        + "on duplicate key update as_of_date = values(as_of_date), overall_score = values(overall_score), "
                        + "confidence = values(confidence), sample_months = values(sample_months), "
                        + "payload_json = values(payload_json), stale = 0, computed_at = values(computed_at), "
                        + "compute_duration_ms = values(compute_duration_ms), error_message = null, "
                        + "profile_version = values(profile_version)",
                userId,
                LocalDate.parse(asOf.length() >= 10 ? asOf.substring(0, 10) : asOf),
                overall,
                confidence,
                sampleMonths,
                JSON.toJSONString(stored),
                Timestamp.from(Instant.now()),
                durationMs);
    }

    public void markStale(String userId) {
        jdbcTemplate.update(
                "update fin_profile_current set stale = 1 where user_id = ?",
                userId);
    }

    public boolean exists(String userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from fin_profile_current where user_id = ?",
                Integer.class,
                userId);
        return count != null && count > 0;
    }
}
