package com.aves.server.model.otr;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientMismatch {
    @JsonProperty
    public Missing missing = new Missing();

    @JsonProperty
    public Missing redundant = new Missing();

    @JsonProperty
    public Missing deleted = new Missing();

}
