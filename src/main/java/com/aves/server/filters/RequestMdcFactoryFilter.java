package com.aves.server.filters;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * Filter that sets MDC.
 */
@Provider
public class RequestMdcFactoryFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        // save id generated by the Nginx
        final String nginxRequestId = requestContext.getHeaderString("X-Request-Id");
        if (nginxRequestId != null) {
            MDC.put("infra_request", nginxRequestId);
        }
        // generate unique id for each request in the application
        final String requestId = UUID.randomUUID().toString();
        MDC.put("app_request", requestId);
    }
}
