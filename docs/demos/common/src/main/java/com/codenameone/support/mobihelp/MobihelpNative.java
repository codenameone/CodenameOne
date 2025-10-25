package com.codenameone.support.mobihelp;

import com.codename1.system.NativeInterface;

// tag::mobihelpNative[]
public interface MobihelpNative extends NativeInterface {
    void config_setAppId(String appId);
    void config_setAppSecret(String appSecret);
    void config_setAutoReplyEnabled(boolean enabled);
    void config_setDomain(String domain);
    void config_setEnhancedPrivacyModeEnabled(boolean enabled);
    void config_setFeedbackType(int type);
    void config_setLaunchCountForReviewPrompt(int count);
    void config_setPrefetchSolutions(boolean prefetch);
    void initNative();
    void setUserFullName(String name);
    void showSupportWithTags(String tags, String separator);
    void getUnreadCountAsync(int callbackId);
}
// end::mobihelpNative[]
