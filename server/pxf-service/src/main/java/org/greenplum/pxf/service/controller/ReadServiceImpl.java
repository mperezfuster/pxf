package org.greenplum.pxf.service.controller;

import lombok.extern.slf4j.Slf4j;
import org.greenplum.pxf.api.model.ConfigurationFactory;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.service.FragmenterService;
import org.greenplum.pxf.service.bridge.BridgeFactory;
import org.greenplum.pxf.service.security.SecurityService;
import org.springframework.stereotype.Service;

/**
 * Implementation of the ReadService.
 */
@Service
@Slf4j
public class ReadServiceImpl extends BaseServiceImpl implements ReadService {

    private final FragmenterService fragmenterService;

    /**
     * Creates a new instance.
     *
     * @param configurationFactory configuration factory
     * @param bridgeFactory        bridge factory
     * @param securityService      security service
     * @param fragmenterService    fragmenter service
     */
    public ReadServiceImpl(ConfigurationFactory configurationFactory,
                           BridgeFactory bridgeFactory,
                           SecurityService securityService,
                           FragmenterService fragmenterService) {
        super(configurationFactory);
        this.bridgeFactory = bridgeFactory;
        this.securityService = securityService;
        this.fragmenterService = fragmenterService;
    }

    @Override
    public ReadResponse getReadResponse(RequestContext context) {
        initConfiguration(context);
        // TODO: get unified session tracker id, use it in log statement
        log.debug("Returning BridgeResponse");
        return new ReadResponse(bridgeFactory, securityService, fragmenterService, context);
    }
}
