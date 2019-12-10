package com.aves.server.filters;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.filter.FilterFactory;

@JsonTypeName("log-filter-factory")
public class LogFilterFactory implements FilterFactory<IAccessEvent> {
    @Override
    public Filter<IAccessEvent> build() {
        return new Filter<IAccessEvent>() {
            @Override
            public FilterReply decide(IAccessEvent event) {
                String requestURI = event.getRequestURI();

                if (requestURI.contains("healthcheck")) {
                    return FilterReply.DENY;
                }

                if (requestURI.contains("swagger")) {
                    return FilterReply.DENY;
                }

                if (requestURI.contains("await")) {
                    return FilterReply.DENY;
                }

                if (requestURI.contains("access")) {
                    return FilterReply.DENY;
                }

//                if (requestURI.contains("notifications")) {
//                    return FilterReply.DENY;
//                }
                return FilterReply.NEUTRAL;
            }
        };
    }
}