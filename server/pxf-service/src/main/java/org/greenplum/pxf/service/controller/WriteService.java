package org.greenplum.pxf.service.controller;

import org.greenplum.pxf.api.model.RequestContext;

import java.io.InputStream;

/**
 * Service that writes data to external systems.
 */
public interface WriteService {

    /**
     * Returns response for a write request with the given context.
     *
     * @param context     request context
     * @param inputStream input stream to read data from
     * @return read response instance
     */
    String getWriteResponse(RequestContext context, InputStream inputStream) throws Exception;
}
