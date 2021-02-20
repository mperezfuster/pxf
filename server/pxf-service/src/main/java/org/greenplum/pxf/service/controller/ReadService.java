package org.greenplum.pxf.service.controller;

import org.greenplum.pxf.api.model.RequestContext;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
    StreamingResponseBody getReadResponse(RequestContext context);
}
