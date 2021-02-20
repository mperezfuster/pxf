package org.greenplum.pxf.service.controller;

import org.apache.hadoop.conf.Configuration;
import org.greenplum.pxf.api.model.ConfigurationFactory;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.service.bridge.BridgeFactory;
import org.greenplum.pxf.service.security.SecurityService;

/**
 * Base abstract implementation of the Service class, provides means to initialize configuration.
 */
public abstract class BaseServiceImpl {

    protected BridgeFactory bridgeFactory;
    protected SecurityService securityService;

    private final ConfigurationFactory configurationFactory;

    public BaseServiceImpl(ConfigurationFactory configurationFactory) {
        this.configurationFactory = configurationFactory;
    }

    /**
     * Initializes the configuration and stores it in the Request Context
     * @param context request context
     */
    protected void initConfiguration(RequestContext context) {
        // Initialize the configuration for this request
        Configuration configuration = configurationFactory.
                initConfiguration(
                        context.getConfig(),
                        context.getServerName(),
                        context.getUser(),
                        context.getAdditionalConfigProps());

        context.setConfiguration(configuration);
    }
}
