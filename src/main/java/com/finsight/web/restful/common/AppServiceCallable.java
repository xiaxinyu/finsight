package com.finsight.web.restful.common;

import com.finsight.core.AppServiceException;

/**
 * Callback used by {@link ControllerHelper} run helpers (checked exception).
 */
@FunctionalInterface
public interface AppServiceCallable<T> {

    T call() throws AppServiceException;
}
