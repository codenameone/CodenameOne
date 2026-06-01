// Mirrors the output of cn1:generate-openapi for the e2e openapi.json.
package com.codename1.e2e.rest;

import com.codename1.annotations.JsonProperty;
import com.codename1.annotations.Mapped;

@Mapped
public class Greeting {
    @JsonProperty("name")
    public String name;
    @JsonProperty("message")
    public String message;
    @JsonProperty("transport")
    public String transport;

    public Greeting() {}
}
