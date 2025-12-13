package com.codenameone.support.mobihelp;

import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import java.util.ArrayList;

// tag::mobihelpClassOverview[]
public class Mobihelp {

    private static char[] separators = new char[]{',','|','/','@','#','%','!','^','&','*','=','+','*','<'};
    private static MobihelpNative peer;

    public static boolean isSupported() {
        return peer != null;
    }

    public static void setPeer(MobihelpNative peer) {
        Mobihelp.peer = peer;
    }

    //Attach the given custom data (key-value pair) to the conversations/tickets.
    public final static void addCustomData(String key, String value) {
        // ...
    }
    //Attach the given custom data (key-value pair) to the conversations/tickets with the ability to flag sensitive data.
    public final static void addCustomData(String key, String value, boolean isSensitive) {
        // ...
    }
    //Clear all breadcrumb data.
    public final static void clearBreadCrumbs() {
        // ...
    }
    //Clear all custom data.
    public final static void clearCustomData() {
        // ...
    }
    //Clears User information.
    public final static void clearUserData() {
        // ...
    }
    //Retrieve the number of unread items across all the conversations for the user synchronously i.e.
    public final static int getUnreadCount() {
        return 0;
    }

    //Retrieve the number of unread items across all the conversations for the user asynchronously
    public final static void getUnreadCountAsync(UnreadUpdatesCallback callback) {
        // ...
    }
    //Initialize the Mobihelp support section with necessary app configuration.
    public final static void initAndroid(MobihelpConfig config) {
        // ...
    }

    public final static void initIOS(MobihelpConfig config) {
        // ...
    }

    public void showSupport() {
        // ...
    }
}
// end::mobihelpClassOverview[]

// tag::mobihelpInitMethods[]
class MobihelpInitMethods {
    //Initialize the Mobihelp support section with necessary app configuration.
    // tag::mobihelpInitAndroid[]
    public final static void initAndroid(MobihelpConfig config) {
        if ("and".equals(Display.getInstance().getPlatformName())) {
            init(config);
        }
    }
    // end::mobihelpInitAndroid[]

    // tag::mobihelpInitIOS[]
    public final static void initIOS(MobihelpConfig config) {
        if ("ios".equals(Display.getInstance().getPlatformName())) {
            init(config);
        }
    }
    // end::mobihelpInitIOS[]

    private static void init(MobihelpConfig config) {
        MobihelpNative peer = (MobihelpNative) NativeLookup.create(MobihelpNative.class);
        peer.config_setAppId(config.getAppId());
        peer.config_setAppSecret(config.getAppSecret());
        peer.config_setAutoReplyEnabled(config.isAutoReplyEnabled());
        peer.config_setDomain(config.getDomain());
        peer.config_setEnhancedPrivacyModeEnabled(config.isEnhancedPrivacyModeEnabled());
        if (config.getFeedbackType() != null) {
            peer.config_setFeedbackType(config.getFeedbackType().ordinal());
        }
        peer.config_setLaunchCountForReviewPrompt(config.getLaunchCountForReviewPrompt());
        peer.config_setPrefetchSolutions(config.isPrefetchSolutions());
        peer.initNative();
    }
}
// end::mobihelpInitMethods[]

// tag::mobihelpSetUserFullName[]
class MobihelpUserSupport {
    private static MobihelpNative peer;

    public static void setPeer(MobihelpNative nativePeer) {
        peer = nativePeer;
    }

    // tag::mobihelpSetUserFullNameMethod[]
    public final static void setUserFullName(String name) {
        peer.setUserFullName(name);
    }
    // end::mobihelpSetUserFullNameMethod[]

    // tag::mobihelpShowSupportWithTags[]
    public final static void showSupport(ArrayList<String> tags) {
        String separator = findUnusedSeparator(tags);
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            sb.append(tag).append(separator);
        }
        peer.showSupportWithTags(sb.toString().substring(0, sb.length() - separator.length()), separator);
    }
    // end::mobihelpShowSupportWithTags[]

    private static String findUnusedSeparator(ArrayList<String> tags) {
        return ",";
    }

    // tag::mobihelpGetUnreadCountAsync[]
    public final static void getUnreadCountAsync(UnreadUpdatesCallback callback) {
        int callbackId = MobihelpNativeCallback.registerUnreadUpdatesCallback(callback);
        peer.getUnreadCountAsync(callbackId);
    }
    // end::mobihelpGetUnreadCountAsync[]
}
// end::mobihelpSetUserFullName[]
