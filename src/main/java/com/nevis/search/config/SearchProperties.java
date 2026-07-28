package com.nevis.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Search tuning knobs. Floors are model-specific and were tuned empirically
 * against the seed data (see README); they are not universal constants.
 */
@ConfigurationProperties(prefix = "nevis.search")
public record SearchProperties(
        int limitDefault,
        int limitMax,
        int channelDepth,
        int rrfK,
        double semanticFloor,
        double clientFloor
) {
    public SearchProperties {
        if (limitDefault <= 0) limitDefault = 20;
        if (limitMax <= 0) limitMax = 100;
        if (channelDepth <= 0) channelDepth = 50;
        if (rrfK <= 0) rrfK = 60;
    }

    /** Clamp a user-supplied limit into [1, limitMax], defaulting when absent. */
    public int clampLimit(Integer requested) {
        if (requested == null) return limitDefault;
        if (requested < 1) return 1;
        return Math.min(requested, limitMax);
    }
}
