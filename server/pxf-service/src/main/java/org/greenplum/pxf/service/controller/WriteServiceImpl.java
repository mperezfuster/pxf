package org.greenplum.pxf.service.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.greenplum.pxf.api.model.ConfigurationFactory;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.api.utilities.Utilities;
import org.greenplum.pxf.service.bridge.Bridge;
import org.greenplum.pxf.service.bridge.BridgeFactory;
import org.greenplum.pxf.service.security.SecurityService;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.InputStream;
import java.security.PrivilegedExceptionAction;

/**
 * Implementation of the WriteService.
 */
@Service
@Slf4j
public class WriteServiceImpl extends BaseServiceImpl implements WriteService {

    /**
     * Creates a new instance.
     *
     * @param configurationFactory configuration factory
     * @param bridgeFactory        bridge factory
     * @param securityService      security service
     */
    public WriteServiceImpl(ConfigurationFactory configurationFactory,
                            BridgeFactory bridgeFactory,
                            SecurityService securityService) {
        super(configurationFactory);
        this.bridgeFactory = bridgeFactory;
        this.securityService = securityService;
    }

    @Override
    public String getWriteResponse(RequestContext context, InputStream inputStream) throws Exception {
        initConfiguration(context);

        PrivilegedExceptionAction<Long> action = () -> readStream(context, inputStream);
        Long totalWritten = securityService.doAs(context, action);

        String censuredPath = Utilities.maskNonPrintables(context.getDataSource());
        String returnMsg = String.format("wrote %d bulks to %s", totalWritten, censuredPath);
        // TODO: get unified session tracker id, use it in log statement
        log.debug(returnMsg);

        return returnMsg;
    }

    /**
     * Reads the input stream, iteratively submits submits data from the stream to created bridge.
     *
     * @param context     request context
     * @param inputStream input stream
     * @return number of data bulks written
     * @throws Exception if error occurs when writing data
     */
    private Long readStream(RequestContext context, InputStream inputStream) throws Exception {
        Bridge bridge = bridgeFactory.getBridge(context);

        // Open the output file
        bridge.beginIteration();
        long totalWritten = 0;
        Exception ex = null;

        // dataStream will close automatically in the end of the try.
        // inputStream is closed by dataStream.close().
        try (DataInputStream dataStream = new DataInputStream(inputStream)) {
            while (bridge.setNext(dataStream)) {
                ++totalWritten;
            }
        } catch (ClientAbortException cae) {
            // Occurs whenever client (GPDB) decides to end the connection
            if (log.isDebugEnabled()) {
                // Stacktrace in debug
                log.warn(String.format("Remote connection closed by GPDB (segment %s)", context.getSegmentId()), cae);
            } else {
                log.warn("Remote connection closed by GPDB (segment {}) (Enable debug for stacktrace)", context.getSegmentId());
            }
            ex = cae;
            // Re-throw the exception so Spring MVC is aware that an IO error has occurred
            throw cae;
        } catch (Exception e) {
            log.error(String.format("Exception: totalWritten so far %d to %s", totalWritten, context.getDataSource()), e);
            ex = e;
            throw ex;
        } finally {
            try {
                bridge.endIteration();
            } catch (Exception e) {
                ex = (ex == null) ? e : ex;
            }
        }

        // Report any errors we might have encountered
        if (ex != null) throw ex;

        return totalWritten;
    }

}
