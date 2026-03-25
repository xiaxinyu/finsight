package com.finsight.web.restful.model;

import java.util.Collections;
import java.util.List;

/**
 * Helpers for empty EasyUI datagrid payloads (prefer over returning {@code null}).
 */
public final class CollectionResults {

    private CollectionResults() {
    }

    public static <T> CollectionResult<T> empty() {
        CollectionResult<T> r = new CollectionResult<>();
        r.setTotal(0);
        r.setRows(Collections.emptyList());
        return r;
    }

    public static <T> CollectionResult<T> of(int total, List<T> rows) {
        CollectionResult<T> r = new CollectionResult<>();
        r.setTotal(total);
        r.setRows(rows);
        return r;
    }
}
