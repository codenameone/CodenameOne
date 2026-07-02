// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::app-review-java-001[]
AppReview.getInstance().requestReview();
// end::app-review-java-001[]

// tag::app-review-java-002[]
AppReview.getInstance()
        .setMinimumLaunches(5)          // at least 5 launches
        .setMinimumDaysInstalled(3)     // and installed at least 3 days
        .setDaysBetweenPrompts(30)      // never nag more than monthly
        .setStoreUrl("https://apps.apple.com/app/id0000000000")
        .setSupportEmail("support@example.com")
        .registerSession();
// end::app-review-java-002[]

// tag::app-review-java-003[]
AppReview.getInstance().setFeedbackListener(new FeedbackListener() {
    public boolean lowRating(int rating) {
        // return true to present your own feedback UI and suppress the
        // built-in e-mail composer
        return false;
    }

    public void feedback(int rating, String text) {
        // called with the free text the user typed in the built-in composer
        myBackend.submitFeedback(rating, text);
    }
});
// end::app-review-java-003[]
