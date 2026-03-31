package com.finsight.application.service.support;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses comma-separated transaction ids from UI/query parameters.
 */
public final class TransactionIdList {

    private TransactionIdList() {
    }

    public static List<String> parseCommaSeparatedIds(String ids) {
        String s = StringUtils.trimToEmpty(ids);
        if (s.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String v = StringUtils.trimToEmpty(p);
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return out;
    }
}
