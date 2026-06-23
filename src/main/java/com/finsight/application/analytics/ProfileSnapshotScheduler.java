package com.finsight.application.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Daily profile snapshot refresh (explicit persist — never on GET profile).
 */
@Component
@ConditionalOnProperty(name = "finsight.analytics.profile-snapshot-scheduler-enabled", havingValue = "true")
public class ProfileSnapshotScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileSnapshotScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final FinancialProfileService profileService;

    public ProfileSnapshotScheduler(JdbcTemplate jdbcTemplate, FinancialProfileService profileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.profileService = profileService;
    }

    @Scheduled(cron = "${finsight.analytics.profile-snapshot-cron:0 0 2 * * *}")
    public void refreshDailySnapshots() {
        List<String> users = jdbcTemplate.queryForList(
                "select distinct coalesce(nullif(trim(created_by), ''), '_anonymous') as user_id "
                        + "from transaction where coalesce(deleted, 0) = 0 limit 32",
                String.class);
        for (String userId : users) {
            try {
                profileService.refreshProfileSnapshotsForUser(userId);
                LOG.info("profile.snapshot refreshed user={}", userId);
            } catch (Exception ex) {
                LOG.warn("profile.snapshot refresh failed user={}: {}", userId, ex.getMessage());
            }
        }
    }
}
