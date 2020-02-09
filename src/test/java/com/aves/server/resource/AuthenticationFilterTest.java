package com.aves.server.resource;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import static com.aves.server.resource.Const.USER_ID;

@Provider
public class AuthenticationFilterTest implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty("zuid", USER_ID);
    }
}