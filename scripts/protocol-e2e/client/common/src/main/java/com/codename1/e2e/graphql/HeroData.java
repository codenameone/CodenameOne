// Mirrors the output of cn1:generate-graphql (query response root).
package com.codename1.e2e.graphql;

import com.codename1.annotations.JsonProperty;
import com.codename1.annotations.Mapped;

@Mapped
public class HeroData {
    @JsonProperty("hero")
    public HeroData_Hero hero;

    public HeroData() {}
}
