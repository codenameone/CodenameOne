// Mirrors the output of cn1:generate-graphql (nested selection type).
package com.codename1.e2e.graphql;

import com.codename1.annotations.JsonProperty;
import com.codename1.annotations.Mapped;

@Mapped
public class AddReviewData_AddReview {
    @JsonProperty("stars")
    public Integer stars;
    @JsonProperty("commentary")
    public String commentary;

    public AddReviewData_AddReview() {}
}
