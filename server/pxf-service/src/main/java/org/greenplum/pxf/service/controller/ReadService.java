package org.greenplum.pxf.service.controller;

import org.greenplum.pxf.api.model.RequestContext;

/**
 * Service that reads data from external systems.
 */
public interface ReadService {

    /**
     * Returns response for a read request with the given context.
     *
     * @param context request context
     * @return read response instance
     */
    ReadResponse getReadResponse(RequestContext context);
}
