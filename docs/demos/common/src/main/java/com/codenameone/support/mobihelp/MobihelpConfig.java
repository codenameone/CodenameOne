package com.codenameone.support.mobihelp;

// tag::mobihelpConfig[]
public class MobihelpConfig {
    private String appSecret;
    private String appId;
    private String domain;
    private boolean autoReplyEnabled;
    private boolean enhancedPrivacyModeEnabled;
    private FeedbackType feedbackType;
    private int launchCountForReviewPrompt;
    private boolean prefetchSolutions;

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public String getDomain() {
        return domain;
    }

    public void setAutoReplyEnabled(boolean autoReplyEnabled) {
        this.autoReplyEnabled = autoReplyEnabled;
    }

    public boolean isAutoReplyEnabled() {
        return autoReplyEnabled;
    }

    public void setEnhancedPrivacyModeEnabled(boolean enhancedPrivacyModeEnabled) {
        this.enhancedPrivacyModeEnabled = enhancedPrivacyModeEnabled;
    }

    public boolean isEnhancedPrivacyModeEnabled() {
        return enhancedPrivacyModeEnabled;
    }

    public void setFeedbackType(FeedbackType feedbackType) {
        this.feedbackType = feedbackType;
    }

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setLaunchCountForReviewPrompt(int launchCountForReviewPrompt) {
        this.launchCountForReviewPrompt = launchCountForReviewPrompt;
    }

    public int getLaunchCountForReviewPrompt() {
        return launchCountForReviewPrompt;
    }

    public void setPrefetchSolutions(boolean prefetchSolutions) {
        this.prefetchSolutions = prefetchSolutions;
    }

    public boolean isPrefetchSolutions() {
        return prefetchSolutions;
    }

    public enum FeedbackType {
        POSITIVE,
        NEGATIVE
    }
}
// end::mobihelpConfig[]
