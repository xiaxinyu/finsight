package com.finsight.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "finsight")
public class FinsightFeatureProperties {

    private Planning planning = new Planning();
    private Advisor advisor = new Advisor();
    private Profile profile = new Profile();
    private Forecast forecast = new Forecast();
    private MerchantMining merchantMining = new MerchantMining();
    private Metrics metrics = new Metrics();
    private Analytics analytics = new Analytics();
    private Security security = new Security();

    public Planning getPlanning() {
        return planning;
    }

    public void setPlanning(Planning planning) {
        this.planning = planning;
    }

    public Advisor getAdvisor() {
        return advisor;
    }

    public void setAdvisor(Advisor advisor) {
        this.advisor = advisor;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
    }

    public MerchantMining getMerchantMining() {
        return merchantMining;
    }

    public void setMerchantMining(MerchantMining merchantMining) {
        this.merchantMining = merchantMining;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public static class Planning {
        private boolean persist;

        public boolean isPersist() {
            return persist;
        }

        public void setPersist(boolean persist) {
            this.persist = persist;
        }
    }

    public static class Advisor {
        private boolean enabled = true;
        private boolean localAiEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLocalAiEnabled() {
            return localAiEnabled;
        }

        public void setLocalAiEnabled(boolean localAiEnabled) {
            this.localAiEnabled = localAiEnabled;
        }
    }

    public static class Profile {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Forecast {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class MerchantMining {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Metrics {
        /** When true, advisor layers fall back to report SQL if reconciliation fails. */
        private boolean reconcileGate;

        public boolean isReconcileGate() {
            return reconcileGate;
        }

        public void setReconcileGate(boolean reconcileGate) {
            this.reconcileGate = reconcileGate;
        }
    }

    public static class Analytics {
        /** Profile read cache TTL (seconds). Default 10 minutes. */
        private int profileCacheTtlSeconds = 600;
        /** Advisor recommendations cache TTL (seconds). Default 10 minutes. */
        private int advisorCacheTtlSeconds = 600;
        /** Forecast preview cache TTL (seconds). Default 10 minutes. */
        private int forecastCacheTtlSeconds = 600;
        /** When true, {@code ProfileSnapshotScheduler} persists daily snapshots per ledger user. */
        private boolean profileSnapshotSchedulerEnabled = false;
        /** Cron for profile snapshot scheduler (Spring {@code @Scheduled}). */
        private String profileSnapshotSchedulerCron = "0 0 2 * * *";

        public int getProfileCacheTtlSeconds() {
            return profileCacheTtlSeconds;
        }

        public void setProfileCacheTtlSeconds(int profileCacheTtlSeconds) {
            this.profileCacheTtlSeconds = profileCacheTtlSeconds;
        }

        public int getAdvisorCacheTtlSeconds() {
            return advisorCacheTtlSeconds;
        }

        public void setAdvisorCacheTtlSeconds(int advisorCacheTtlSeconds) {
            this.advisorCacheTtlSeconds = advisorCacheTtlSeconds;
        }

        public int getForecastCacheTtlSeconds() {
            return forecastCacheTtlSeconds;
        }

        public void setForecastCacheTtlSeconds(int forecastCacheTtlSeconds) {
            this.forecastCacheTtlSeconds = forecastCacheTtlSeconds;
        }

        public boolean isProfileSnapshotSchedulerEnabled() {
            return profileSnapshotSchedulerEnabled;
        }

        public void setProfileSnapshotSchedulerEnabled(boolean profileSnapshotSchedulerEnabled) {
            this.profileSnapshotSchedulerEnabled = profileSnapshotSchedulerEnabled;
        }

        public String getProfileSnapshotSchedulerCron() {
            return profileSnapshotSchedulerCron;
        }

        public void setProfileSnapshotSchedulerCron(String profileSnapshotSchedulerCron) {
            this.profileSnapshotSchedulerCron = profileSnapshotSchedulerCron;
        }
    }

    public static class Security {
        private boolean csrfEnabled;
        private boolean actuatorPublic = true;
        private int loginMaxAttempts = 8;
        private long loginLockoutSeconds = 900;

        public boolean isCsrfEnabled() {
            return csrfEnabled;
        }

        public void setCsrfEnabled(boolean csrfEnabled) {
            this.csrfEnabled = csrfEnabled;
        }

        public boolean isActuatorPublic() {
            return actuatorPublic;
        }

        public void setActuatorPublic(boolean actuatorPublic) {
            this.actuatorPublic = actuatorPublic;
        }

        public int getLoginMaxAttempts() {
            return loginMaxAttempts;
        }

        public void setLoginMaxAttempts(int loginMaxAttempts) {
            this.loginMaxAttempts = loginMaxAttempts;
        }

        public long getLoginLockoutSeconds() {
            return loginLockoutSeconds;
        }

        public void setLoginLockoutSeconds(long loginLockoutSeconds) {
            this.loginLockoutSeconds = loginLockoutSeconds;
        }
    }
}
