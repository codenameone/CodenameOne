/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.notifications;

import java.util.ArrayList;
import java.util.List;

/// Local notifications are user notifications that are scheduled by the app itself. They
/// are very similar to push notifications, except that they originate locally, rather than
/// remotely.
///
/// They enable an app that isnt running in the foreground to let its users know it
/// has information for them. The information could be a message, an impending calendar
/// event, or new data on a remote server. They can display an alert message or
/// they can badge the app icon. They can also play a sound when the alert or badge
/// number is shown.
///
/// When users are notified that the app has a message, event, or other data for them,
/// they can launch the app and see the details. They can also choose to ignore the notification,
/// in which case the app is not activated.
///
/// This class encapsulates a single notification (though the notification can
/// be repeating).
///
/// Usage
/// ```java
/// // File: GeofenceListenerImpl.java
/// public class GeofenceListenerImpl implements GeofenceListener {
/// @Override
///     public void onExit(String id) {
///     }
/// @Override
///     public void onEntered(String id) {
///         if(!Display.getInstance().isMinimized()) {
///             Display.getInstance().callSerially(() -> {
///                 Dialog.show("Welcome", "Thanks for arriving", "OK", null);
///             });
///         } else {
///             LocalNotification ln = new LocalNotification();
///             ln.setId("LnMessage");
///             ln.setAlertTitle("Welcome");
///             ln.setAlertBody("Thanks for arriving!");
///             Display.getInstance().scheduleLocalNotification(ln, System.currentTimeMillis() + 10, LocalNotification.REPEAT_NONE);
///         }
///     }
/// }
/// ```
///
/// ```java
/// // File: GeofenceSample.java
/// Geofence gf = new Geofence("test", loc, 100, 100000);
/// LocationManager.getLocationManager().addGeoFencing(GeofenceListenerImpl.class, gf);
/// ```
///
/// **Android Note:** The default image that is used on the android top status bar
/// and on the notification itself is the App's icon.  However Android 5 and above will only display
/// this image as a silhouette using alpha pixels.  This will result in many icons appearing to be
/// a blank white square.  In such cases you can provide an alternate image to be displayed instead.
/// Place a 24x24 image named "ic_stat_notify.png" in your project's native/android
/// directory, and this image will be used instead.
/// @author shannah
///
/// #### See also
///
/// - com.codename1.ui.Display#scheduleLocalNotification(LocalNotification n, long firstTime, int repeat)
///
/// - com.codename1.ui.Display#cancelLocalNotification(java.lang.String id)
public class LocalNotification {

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should not be repeated.
    public static final int REPEAT_NONE = 0;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every 1 minute.
    public static final int REPEAT_MINUTE = 1;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every hour.
    public static final int REPEAT_HOUR = 3;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every day.
    public static final int REPEAT_DAY = 4;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every week.
    public static final int REPEAT_WEEK = 5;

    // We don't support month or year right now because it is too complicated
    // to keep track of leap years, and days in month on platforms that only 
    // support repeat by milliseconds etc..

    private String id = "";
    private int badgeNumber = -1;
    private String alertBody = "";
    private String alertTitle = "";
    private String alertSound = "";
    private String alertImage = "";
    private boolean foreground;

    private String channelId;
    private String groupId;
    private boolean groupSummary;
    private boolean fullScreenIntent;
    private boolean timeSensitive;
    private boolean ongoing;
    private int progressMax;
    private int progress;
    private boolean progressIndeterminate;
    private String customViewLayout;
    private final List<Action> actions = new ArrayList<Action>();
    private MessagingStyle messagingStyle;

    /// Gets the badge number to set for this notification.
    ///
    /// #### Returns
    ///
    /// the badgeNumber
    public int getBadgeNumber() {
        return badgeNumber;
    }

    /// Gets the badge number to set for this notification.
    ///
    /// #### Parameters
    ///
    /// - `badgeNumber`: the badgeNumber to set
    public void setBadgeNumber(int badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    /// Gets the alert body to be displayed for this notification.
    ///
    /// #### Returns
    ///
    /// the alertBody
    public String getAlertBody() {
        return alertBody;
    }

    /// Sets the alert body to be displayed for this notification.
    ///
    /// #### Parameters
    ///
    /// - `alertBody`: the alertBody to set
    public void setAlertBody(String alertBody) {
        this.alertBody = alertBody;
    }

    /// Gets the alert title to be displayed for this notification.
    ///
    /// #### Returns
    ///
    /// the alertTitle
    public String getAlertTitle() {
        return alertTitle;
    }

    /// Sets the alert title to be displayed for this notification.
    ///
    /// #### Parameters
    ///
    /// - `alertTitle`: the alertTitle to set
    public void setAlertTitle(String alertTitle) {
        this.alertTitle = alertTitle;
    }

    /// Gets the alert sound to be sounded when the notification arrives.  This
    /// should refer to a sound file that is bundled in the default package of your
    /// app.
    ///
    /// #### Returns
    ///
    /// the alertSound
    public String getAlertSound() {
        return alertSound;
    }

    /// Sets the alert sound to be sounded when the notification arrives.  This
    /// should refer to a sound file that is bundled in the default package of your
    /// app.
    /// The name of the file must start with the "notification_sound" prefix.
    ///
    /// `````java LocalNotification n = new LocalNotification(); n.setAlertSound("/notification_sound_bells.mp3"); `````
    ///
    /// #### Parameters
    ///
    /// - `alertSound`: the alertSound to set
    public void setAlertSound(String alertSound) {
        this.alertSound = alertSound;
    }

    /// Gets the ID of the notification.  The ID is the only information that is
    /// passed to `LocalNotificationCallback#localNotificationReceived(java.lang.String)`
    /// so you can use it as a lookup key to retrieve the rest of the information as required
    /// from storage or some other mechanism.
    ///
    /// The ID can also be used to cancel the notification later using `com.codename1.ui.Display#cancelLocalNotification(java.lang.String)`
    ///
    /// #### Returns
    ///
    /// the id
    public String getId() {
        return id;
    }

    /// Sets the ID of the notification.  The ID is the only information that is
    /// passed to `LocalNotificationCallback#localNotificationReceived(java.lang.String)`
    /// so you can use it as a lookup key to retrieve the rest of the information as required
    /// from storage or some other mechanism.
    ///
    /// The ID can also be used to cancel the notification later using `com.codename1.ui.Display#cancelLocalNotification(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `id`: the id to set
    public void setId(String id) {
        this.id = id;
    }

    /// Gets the notification image
    ///
    /// #### Returns
    ///
    /// image path
    public String getAlertImage() {
        return alertImage;
    }

    /// Sets an image to be displayed on the platform notifications bar, if the underlying platform
    /// supports image displaying otherwise the image will be ignored.
    ///
    /// #### Parameters
    ///
    /// - `image`: a path to the image, the image needs to be placed in the app root.
    public void setAlertImage(String image) {
        this.alertImage = image;
    }

    /// Checks whether this notification will be displayed in the device's notification center even when the app is in the foreground.
    ///
    /// #### Returns
    ///
    /// True if the notification will display in the device's notification center even when the app is in the foreground.
    ///
    /// #### Since
    ///
    /// 7.0
    public boolean isForeground() {
        return foreground;
    }

    /// Set whether this notification should be displayed in the device's notification center even when the app is
    /// in the foreground.
    ///
    /// #### Parameters
    ///
    /// - `foreground`: True to display this notification in the notification center even when the app is in the foreground.
    ///
    /// #### Since
    ///
    /// 7.0
    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }

    /// Gets the notification channel id this notification is posted to. Channels are an
    /// Android concept; see `NotificationChannelBuilder`. On platforms without channels
    /// this value is ignored.
    ///
    /// #### Returns
    ///
    /// the channel id, or null
    public String getChannelId() {
        return channelId;
    }

    /// Sets the notification channel id this notification is posted to.
    ///
    /// #### Parameters
    ///
    /// - `channelId`: the channel id
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    /// Convenience alias for `#setAlertSound(String)` that returns this notification for
    /// chaining.
    ///
    /// #### Parameters
    ///
    /// - `sound`: the alert sound file path
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setSound(String sound) {
        setAlertSound(sound);
        return this;
    }

    /// Gets the group id used to bundle related notifications together in the shade.
    ///
    /// #### Returns
    ///
    /// the group id, or null
    public String getGroupId() {
        return groupId;
    }

    /// Assigns this notification to a group. Notifications sharing a group id are
    /// visually bundled. On iOS the group id maps to the notification thread identifier.
    ///
    /// #### Parameters
    ///
    /// - `groupId`: the group id
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setGroup(String groupId) {
        this.groupId = groupId;
        return this;
    }

    /// Returns true if this notification is the summary for its group.
    ///
    /// #### Returns
    ///
    /// true if this is a group summary
    public boolean isGroupSummary() {
        return groupSummary;
    }

    /// Marks this notification as the summary of its group (Android). The summary is the
    /// single entry shown when the group is collapsed.
    ///
    /// #### Parameters
    ///
    /// - `groupSummary`: true to make this notification the group summary
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setGroupSummary(boolean groupSummary) {
        this.groupSummary = groupSummary;
        return this;
    }

    /// Returns true if this notification should launch a full screen intent.
    ///
    /// #### Returns
    ///
    /// true if a full screen intent is requested
    public boolean isFullScreenIntent() {
        return fullScreenIntent;
    }

    /// Requests that this notification launch a full screen intent (Android), used for
    /// high priority interruptions such as incoming calls or alarms. Ignored on platforms
    /// that do not support it.
    ///
    /// #### Parameters
    ///
    /// - `fullScreenIntent`: true to request a full screen intent
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setFullScreenIntent(boolean fullScreenIntent) {
        this.fullScreenIntent = fullScreenIntent;
        return this;
    }

    /// Returns true if this notification is marked time sensitive.
    ///
    /// #### Returns
    ///
    /// true if time sensitive
    public boolean isTimeSensitive() {
        return timeSensitive;
    }

    /// Marks this notification as time sensitive so it can break through Focus modes
    /// (iOS) or be treated with elevated importance (Android). Requires the corresponding
    /// permission to have been requested.
    ///
    /// #### Parameters
    ///
    /// - `timeSensitive`: true to mark the notification time sensitive
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setTimeSensitive(boolean timeSensitive) {
        this.timeSensitive = timeSensitive;
        return this;
    }

    /// Returns true if this notification is ongoing.
    ///
    /// #### Returns
    ///
    /// true if ongoing
    public boolean isOngoing() {
        return ongoing;
    }

    /// Marks this notification as ongoing (Android), meaning it cannot be dismissed by
    /// the user and represents background activity in progress. Ignored on platforms that
    /// do not support it.
    ///
    /// #### Parameters
    ///
    /// - `ongoing`: true to make the notification ongoing
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setOngoing(boolean ongoing) {
        this.ongoing = ongoing;
        return this;
    }

    /// Returns the maximum value of the progress bar, or 0 if no progress bar is shown.
    ///
    /// #### Returns
    ///
    /// the progress maximum
    public int getProgressMax() {
        return progressMax;
    }

    /// Returns the current progress value.
    ///
    /// #### Returns
    ///
    /// the current progress
    public int getProgress() {
        return progress;
    }

    /// Shows a determinate progress bar on this notification (Android).
    ///
    /// #### Parameters
    ///
    /// - `max`: the maximum progress value
    ///
    /// - `current`: the current progress value
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setProgress(int max, int current) {
        this.progressMax = max;
        this.progress = current;
        this.progressIndeterminate = false;
        return this;
    }

    /// Returns true if the progress bar is indeterminate.
    ///
    /// #### Returns
    ///
    /// true if the progress bar is indeterminate
    public boolean isProgressIndeterminate() {
        return progressIndeterminate;
    }

    /// Shows an indeterminate (spinning) progress bar on this notification (Android).
    ///
    /// #### Parameters
    ///
    /// - `indeterminate`: true to show an indeterminate progress bar
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setIndeterminateProgress(boolean indeterminate) {
        this.progressIndeterminate = indeterminate;
        return this;
    }

    /// Gets the custom view layout name used to render this notification.
    ///
    /// #### Returns
    ///
    /// the custom view layout name, or null
    public String getCustomView() {
        return customViewLayout;
    }

    /// Sets a custom view layout name for this notification. On Android this maps to a
    /// RemoteViews layout bundled in the native resources. On iOS a custom view is
    /// rendered by a notification content extension keyed by the notification category.
    /// Ignored on platforms that do not support custom notification views.
    ///
    /// #### Parameters
    ///
    /// - `customViewLayout`: the layout name
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification setCustomView(String customViewLayout) {
        this.customViewLayout = customViewLayout;
        return this;
    }

    /// Adds an action button to this notification.
    ///
    /// #### Parameters
    ///
    /// - `action`: the action to add
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification addAction(Action action) {
        actions.add(action);
        return this;
    }

    /// Adds a simple action button to this notification.
    ///
    /// #### Parameters
    ///
    /// - `id`: the action id reported back when the user taps the action
    ///
    /// - `title`: the button label
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification addAction(String id, String title) {
        return addAction(new Action(id, title));
    }

    /// Adds a quick reply action with an inline text input field. When the user submits
    /// a reply the entered text is reported back via `com.codename1.push.PushContent#getTextResponse()`
    /// alongside the action id.
    ///
    /// #### Parameters
    ///
    /// - `id`: the action id reported back when the user submits the reply
    ///
    /// - `title`: the button label
    ///
    /// - `placeholder`: placeholder text shown in the text input field
    ///
    /// - `replyButtonText`: the label for the send button
    ///
    /// #### Returns
    ///
    /// this notification for chaining
    public LocalNotification addInputAction(String id, String title, String placeholder, String replyButtonText) {
        Action a = new Action(id, title);
        a.textInputPlaceholder = placeholder;
        a.textInputButtonText = replyButtonText;
        return addAction(a);
    }

    /// Returns the list of action buttons configured on this notification.
    ///
    /// #### Returns
    ///
    /// the actions, never null
    public List<Action> getActions() {
        return actions;
    }

    /// Configures this notification to render as a conversation (messaging style)
    /// notification. Returns the `MessagingStyle` so messages can be added fluently.
    ///
    /// #### Parameters
    ///
    /// - `selfDisplayName`: the name representing the device user in the conversation
    ///
    /// #### Returns
    ///
    /// the messaging style for further configuration
    public MessagingStyle asMessagingStyle(String selfDisplayName) {
        this.messagingStyle = new MessagingStyle(selfDisplayName);
        return this.messagingStyle;
    }

    /// Returns the messaging style configured on this notification, or null if this is
    /// not a messaging style notification.
    ///
    /// #### Returns
    ///
    /// the messaging style, or null
    public MessagingStyle getMessagingStyle() {
        return messagingStyle;
    }

    /// A single action button attached to a local notification. An action may optionally
    /// include an inline text input (quick reply) by setting a placeholder and reply
    /// button text via `LocalNotification#addInputAction(String, String, String, String)`.
    public static class Action {
        private final String id;
        private final String title;
        private String icon;
        private String textInputPlaceholder;
        private String textInputButtonText;

        /// Creates an action.
        ///
        /// #### Parameters
        ///
        /// - `id`: the action id reported back when the user taps the action
        ///
        /// - `title`: the button label
        public Action(String id, String title) {
            this.id = id;
            this.title = title;
        }

        /// Creates an action with an icon.
        ///
        /// The icon is a platform resource NAME, not a numeric id (Codename One has no
        /// numeric resource ids). On Android it is the name of a drawable bundled in the
        /// `native/android` folder (the build copies it into `res/drawable`), resolved at
        /// runtime by name; the file extension is optional and ignored. iOS notification
        /// action buttons do not display icons, so the value is ignored there.
        ///
        /// #### Parameters
        ///
        /// - `id`: the action id reported back when the user taps the action
        ///
        /// - `title`: the button label
        ///
        /// - `icon`: the drawable resource name for the action (Android only)
        public Action(String id, String title, String icon) {
            this.id = id;
            this.title = title;
            this.icon = icon;
        }

        /// Returns the action id.
        ///
        /// #### Returns
        ///
        /// the action id
        public String getId() {
            return id;
        }

        /// Returns the button label.
        ///
        /// #### Returns
        ///
        /// the title
        public String getTitle() {
            return title;
        }

        /// Returns the drawable resource name used for the action icon on Android, or null.
        /// See `Action#Action(String, String, String)` for how it is resolved.
        ///
        /// #### Returns
        ///
        /// the drawable resource name, or null
        public String getIcon() {
            return icon;
        }

        /// Returns the placeholder text for the inline text input, or null when the
        /// action has no text input.
        ///
        /// #### Returns
        ///
        /// the text input placeholder, or null
        public String getTextInputPlaceholder() {
            return textInputPlaceholder;
        }

        /// Returns the label of the reply button for the inline text input, or null when
        /// the action has no text input.
        ///
        /// #### Returns
        ///
        /// the reply button text, or null
        public String getTextInputButtonText() {
            return textInputButtonText;
        }

        /// Returns true if this action has an inline text input (quick reply).
        ///
        /// #### Returns
        ///
        /// true if this is a text input action
        public boolean isTextInput() {
            return textInputPlaceholder != null || textInputButtonText != null;
        }
    }

    /// Describes a conversation (messaging style) notification. A messaging style
    /// notification renders a sequence of chat messages, each attributed to a sender,
    /// and is the recommended presentation for chat and messaging apps.
    public static class MessagingStyle {
        private final String selfDisplayName;
        private String conversationTitle;
        private boolean groupConversation;
        private final List<Message> messages = new ArrayList<Message>();

        /// Creates a messaging style.
        ///
        /// #### Parameters
        ///
        /// - `selfDisplayName`: the name representing the device user
        public MessagingStyle(String selfDisplayName) {
            this.selfDisplayName = selfDisplayName;
        }

        /// Sets the conversation title shown above the messages.
        ///
        /// #### Parameters
        ///
        /// - `t`: the conversation title
        ///
        /// #### Returns
        ///
        /// this messaging style for chaining
        public MessagingStyle conversationTitle(String t) {
            this.conversationTitle = t;
            return this;
        }

        /// Marks the conversation as a group conversation (more than two participants).
        ///
        /// #### Parameters
        ///
        /// - `b`: true if this is a group conversation
        ///
        /// #### Returns
        ///
        /// this messaging style for chaining
        public MessagingStyle groupConversation(boolean b) {
            this.groupConversation = b;
            return this;
        }

        /// Adds a message to the conversation.
        ///
        /// #### Parameters
        ///
        /// - `text`: the message text
        ///
        /// - `timestamp`: the message timestamp in milliseconds since the epoch
        ///
        /// - `senderName`: the display name of the sender, or null for the device user
        ///
        /// #### Returns
        ///
        /// this messaging style for chaining
        public MessagingStyle addMessage(String text, long timestamp, String senderName) {
            messages.add(new Message(text, timestamp, senderName));
            return this;
        }

        /// Returns the name representing the device user.
        ///
        /// #### Returns
        ///
        /// the self display name
        public String getSelfDisplayName() {
            return selfDisplayName;
        }

        /// Returns the conversation title.
        ///
        /// #### Returns
        ///
        /// the conversation title, or null
        public String getConversationTitle() {
            return conversationTitle;
        }

        /// Returns true if this is a group conversation.
        ///
        /// #### Returns
        ///
        /// true if a group conversation
        public boolean isGroupConversation() {
            return groupConversation;
        }

        /// Returns the messages in the conversation.
        ///
        /// #### Returns
        ///
        /// the messages, never null
        public List<Message> getMessages() {
            return messages;
        }

        /// A single message within a messaging style notification.
        public static class Message {
            private final String text;
            private final long timestamp;
            private final String senderName;

            /// Creates a message.
            ///
            /// #### Parameters
            ///
            /// - `text`: the message text
            ///
            /// - `timestamp`: the timestamp in milliseconds since the epoch
            ///
            /// - `senderName`: the sender display name, or null for the device user
            public Message(String text, long timestamp, String senderName) {
                this.text = text;
                this.timestamp = timestamp;
                this.senderName = senderName;
            }

            /// Returns the message text.
            ///
            /// #### Returns
            ///
            /// the text
            public String getText() {
                return text;
            }

            /// Returns the message timestamp.
            ///
            /// #### Returns
            ///
            /// the timestamp in milliseconds since the epoch
            public long getTimestamp() {
                return timestamp;
            }

            /// Returns the sender display name.
            ///
            /// #### Returns
            ///
            /// the sender name, or null for the device user
            public String getSenderName() {
                return senderName;
            }
        }
    }

}
