package com.finsight.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "finsight")
public class FinsightFeatureProperties {

    private Planning planning = new Planning();
    private Advisor advisor = new Advisor();
    private Metrics metrics = new Metrics();
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

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
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

    public static class Security {
        private boolean csrfEnabled;
        private boolean actuatorPublic = true;

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
    }
}
