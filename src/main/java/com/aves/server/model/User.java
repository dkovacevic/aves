package com.aves.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @NotNull
    public UUID id;

    @NotNull
    @NotEmpty
    public String email;

    @NotNull
    @NotEmpty
    public String name;

    @NotNull
    @NotEmpty
    public String phone;

    public String firstname;

    public String lastname;

    public String country;

    @JsonProperty("accent_id")
    public int accent;

    public String locale = "en-US";

    @JsonProperty("managed_by")
    public String managed = "scim";

    @JsonProperty
    public String getHandle() {
        return name.toLowerCase().replace(" ", "");
    }

    @JsonProperty
    public void setHandle() {
    }

    public ArrayList<UserAsset> assets = new ArrayList<>();

    @JsonProperty("picture")
    public ArrayList<String> dummy = new ArrayList<>();

    public static class UserAsset {
        public String size;
        public String key;
        public String type = "image";
    }
}
