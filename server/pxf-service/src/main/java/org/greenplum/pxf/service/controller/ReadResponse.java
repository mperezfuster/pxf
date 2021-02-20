package org.greenplum.pxf.service.controller;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface to write response data to an OutputStream. Decouples controllers from Spring MVC.
 */
public interface ReadResponse {

    /**
     * Writes data to provided OutputStream.
     * @param out output stream
     * @throws IOException if an error occurs
     */
    void writeTo(OutputStream out) throws IOException;
}
