package com.finsight.web.api.support;

import com.finsight.common.exception.AppServiceException;

/**
 * Callback used by {@link ControllerHelper} run helpers (checked exception).
 */
@FunctionalInterface
public interface AppServiceCallable<T> {

    T call() throws AppServiceException;
}
