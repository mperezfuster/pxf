package org.greenplum.pxf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoggingRequestInterceptor implements HandlerInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingRequestInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String pathInfo = request.getRequestURI();
        String segmentId = request.getHeader("X-GP-SEGMENT-ID");
        LOG.info("ax+bb: preHandle - " + pathInfo + " " + segmentId);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        String pathInfo = request.getRequestURI();
        String segmentId = request.getHeader("X-GP-SEGMENT-ID");
        if (response.getStatus() == 404) {
            LOG.info("ax+bb: returning a 404 - " + pathInfo + " " + segmentId);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String pathInfo = request.getRequestURI();
        String segmentId = request.getHeader("X-GP-SEGMENT-ID");
        LOG.info("ax+bb: afterCompletion - " + pathInfo + " " + segmentId);
    }
}
