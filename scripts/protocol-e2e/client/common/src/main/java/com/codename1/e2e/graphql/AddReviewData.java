// Mirrors the output of cn1:generate-graphql (mutation response root).
package com.codename1.e2e.graphql;

import com.codename1.annotations.JsonProperty;
import com.codename1.annotations.Mapped;

@Mapped
public class AddReviewData {
    @JsonProperty("addReview")
    public AddReviewData_AddReview addReview;

    public AddReviewData() {}
}
