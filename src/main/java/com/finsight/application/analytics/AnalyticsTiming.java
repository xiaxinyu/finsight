package com.finsight.application.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight timing helper for analytics read paths with explicit latency budgets.
 */
final class AnalyticsTiming {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsTiming.class);

    private AnalyticsTiming() {
    }

    static TimedCall start(String module, long budgetMs) {
        return new TimedCall(module, budgetMs, System.nanoTime());
    }

    static void logCacheHit(String module, boolean hit) {
        LOG.debug("analytics.{} cacheHit={}", module, hit);
    }

    static final class TimedCall implements AutoCloseable {
        private final String module;
        private final long budgetMs;
        private final long startedNs;

        TimedCall(String module, long budgetMs, long startedNs) {
            this.module = module;
            this.budgetMs = budgetMs;
            this.startedNs = startedNs;
        }

        @Override
        public void close() {
            long elapsedMs = (System.nanoTime() - startedNs) / 1_000_000L;
            if (elapsedMs > budgetMs) {
                LOG.warn("analytics.{} elapsedMs={} budgetMs={} exceeded=true", module, elapsedMs, budgetMs);
            } else {
                LOG.debug("analytics.{} elapsedMs={} budgetMs={}", module, elapsedMs, budgetMs);
            }
        }
    }
}
