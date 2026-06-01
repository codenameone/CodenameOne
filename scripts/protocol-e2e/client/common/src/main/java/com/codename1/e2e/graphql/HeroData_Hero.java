// Mirrors the output of cn1:generate-graphql (nested selection type).
package com.codename1.e2e.graphql;

import com.codename1.annotations.JsonProperty;
import com.codename1.annotations.Mapped;

@Mapped
public class HeroData_Hero {
    @JsonProperty("name")
    public String name;
    @JsonProperty("greeting")
    public String greeting;

    public HeroData_Hero() {}
}
