/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package android.app;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/// A notification as built by [Notification.Builder] (or AndroidX's
/// `NotificationCompat.Builder`). [NotificationManager] posts it as a Codename
/// One `LocalNotification`, and `Service.startForeground` shows it as the
/// foreground service's notification. Title, text, channel, group, ongoing,
/// progress and the badge number carry over; styles contribute their big text
/// as the body; actions, custom views and large icons are kept but not shown.
public class Notification {

    public static final int FLAG_SHOW_LIGHTS = 0x00000001;
    public static final int FLAG_ONGOING_EVENT = 0x00000002;
    public static final int FLAG_INSISTENT = 0x00000004;
    public static final int FLAG_ONLY_ALERT_ONCE = 0x00000008;
    public static final int FLAG_AUTO_CANCEL = 0x00000010;
    public static final int FLAG_NO_CLEAR = 0x00000020;
    public static final int FLAG_FOREGROUND_SERVICE = 0x00000040;
    public static final int FLAG_GROUP_SUMMARY = 0x00000200;

    public static final int DEFAULT_ALL = ~0;
    public static final int DEFAULT_SOUND = 1;
    public static final int DEFAULT_VIBRATE = 2;
    public static final int DEFAULT_LIGHTS = 4;

    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_LOW = -1;
    public static final int PRIORITY_MIN = -2;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_MAX = 2;

    public static final int VISIBILITY_PUBLIC = 1;
    public static final int VISIBILITY_PRIVATE = 0;
    public static final int VISIBILITY_SECRET = -1;

    public static final int FOREGROUND_SERVICE_DEFAULT = 0;
    public static final int FOREGROUND_SERVICE_IMMEDIATE = 1;
    public static final int FOREGROUND_SERVICE_DEFERRED = 2;

    public static final String CATEGORY_CALL = "call";
    public static final String CATEGORY_NAVIGATION = "navigation";
    public static final String CATEGORY_MESSAGE = "msg";
    public static final String CATEGORY_EMAIL = "email";
    public static final String CATEGORY_EVENT = "event";
    public static final String CATEGORY_PROMO = "promo";
    public static final String CATEGORY_ALARM = "alarm";
    public static final String CATEGORY_PROGRESS = "progress";
    public static final String CATEGORY_SOCIAL = "social";
    public static final String CATEGORY_ERROR = "err";
    public static final String CATEGORY_TRANSPORT = "transport";
    public static final String CATEGORY_SYSTEM = "sys";
    public static final String CATEGORY_SERVICE = "service";
    public static final String CATEGORY_REMINDER = "reminder";
    public static final String CATEGORY_RECOMMENDATION = "recommendation";
    public static final String CATEGORY_STATUS = "status";
    public static final String CATEGORY_WORKOUT = "workout";
    public static final String CATEGORY_LOCATION_SHARING = "location_sharing";

    public static final String EXTRA_TITLE = "android.title";
    public static final String EXTRA_TEXT = "android.text";
    public static final String EXTRA_SUB_TEXT = "android.subText";
    public static final String EXTRA_BIG_TEXT = "android.bigText";
    public static final String EXTRA_PROGRESS = "android.progress";
    public static final String EXTRA_PROGRESS_MAX = "android.progressMax";
    public static final String EXTRA_PROGRESS_INDETERMINATE = "android.progressIndeterminate";

    public long when;
    public int icon;
    public int number;
    public CharSequence tickerText;
    public PendingIntent contentIntent;
    public PendingIntent deleteIntent;
    public int flags;
    public int defaults;
    public int priority;
    public int visibility;
    public int color;
    public String category;
    public Bundle extras = new Bundle();
    public Action[] actions;

    String channelId;
    String group;
    boolean groupSummary;
    int progress;
    int progressMax;
    boolean progressIndeterminate;
    long timeoutAfter;

    public Notification() {
        when = System.currentTimeMillis();
    }

    public String getChannelId() {
        return channelId;
    }

    public String getGroup() {
        return group;
    }

    public long getTimeoutAfter() {
        return timeoutAfter;
    }

    public CharSequence title() {
        return extras.getCharSequence(EXTRA_TITLE);
    }

    /// The body a local notification shows: a style's big text when there is
    /// one, otherwise the content text.
    CharSequence body() {
        CharSequence big = extras.getCharSequence(EXTRA_BIG_TEXT);
        return big != null ? big : extras.getCharSequence(EXTRA_TEXT);
    }

    /// This notification as a Codename One local notification with `id`.
    public com.codename1.notifications.LocalNotification toLocal(String id) {
        com.codename1.notifications.LocalNotification ln = new com.codename1.notifications.LocalNotification();
        ln.setId(id);
        CharSequence t = title();
        CharSequence b = body();
        ln.setAlertTitle(t == null ? "" : t.toString());
        ln.setAlertBody(b == null ? "" : b.toString());
        if (channelId != null) {
            ln.setChannelId(channelId);
        }
        if (group != null) {
            ln.setGroup(group);
            ln.setGroupSummary(groupSummary);
        }
        if (number > 0) {
            ln.setBadgeNumber(number);
        }
        ln.setOngoing((flags & FLAG_ONGOING_EVENT) != 0);
        // Codename One shows a determinate bar only; an indeterminate one is
        // left off rather than drawn as empty.
        if (progressMax > 0 && !progressIndeterminate) {
            ln.setProgress(progressMax, progress);
        }
        return ln;
    }

    /// A button on the notification. Kept so code that adds one compiles;
    /// Codename One local notifications have no buttons to show it on.
    public static class Action {
        public int icon;
        public CharSequence title;
        public PendingIntent actionIntent;

        public Action(int icon, CharSequence title, PendingIntent intent) {
            this.icon = icon;
            this.title = title;
            this.actionIntent = intent;
        }

        public static final class Builder {
            private final Action action;

            public Builder(int icon, CharSequence title, PendingIntent intent) {
                action = new Action(icon, title, intent);
            }

            public Builder addExtras(Bundle extras) {
                return this;
            }

            public Builder setAllowGeneratedReplies(boolean allow) {
                return this;
            }

            public Action build() {
                return action;
            }
        }
    }

    /// Base of the expanded layouts. Each contributes to the extras a local
    /// notification reads.
    public abstract static class Style {
        protected Builder mBuilder;

        public void setBuilder(Builder builder) {
            if (mBuilder != builder) {
                mBuilder = builder;
                if (builder != null) {
                    builder.setStyle(this);
                }
            }
        }

        void apply(Notification n) {
        }

        public Notification build() {
            return mBuilder == null ? null : mBuilder.build();
        }
    }

    public static class BigTextStyle extends Style {
        private CharSequence bigText;
        private CharSequence bigTitle;

        public BigTextStyle() {
        }

        public BigTextStyle(Builder builder) {
            setBuilder(builder);
        }

        public BigTextStyle bigText(CharSequence cs) {
            bigText = cs;
            return this;
        }

        public BigTextStyle setBigContentTitle(CharSequence title) {
            bigTitle = title;
            return this;
        }

        public BigTextStyle setSummaryText(CharSequence cs) {
            return this;
        }

        @Override
        void apply(Notification n) {
            if (bigText != null) {
                n.extras.putCharSequence(EXTRA_BIG_TEXT, bigText);
            }
            if (bigTitle != null) {
                n.extras.putCharSequence(EXTRA_TITLE, bigTitle);
            }
        }
    }

    public static class InboxStyle extends Style {
        private final List<CharSequence> lines = new ArrayList<CharSequence>();

        public InboxStyle() {
        }

        public InboxStyle(Builder builder) {
            setBuilder(builder);
        }

        public InboxStyle addLine(CharSequence cs) {
            lines.add(cs);
            return this;
        }

        public InboxStyle setBigContentTitle(CharSequence title) {
            return this;
        }

        public InboxStyle setSummaryText(CharSequence cs) {
            return this;
        }

        @Override
        void apply(Notification n) {
            if (!lines.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.size(); i++) {
                    if (i > 0) {
                        sb.append('\n');
                    }
                    sb.append(lines.get(i));
                }
                n.extras.putCharSequence(EXTRA_BIG_TEXT, sb.toString());
            }
        }
    }

    public static class Builder {
        private final Notification n = new Notification();
        private final List<Action> actions = new ArrayList<Action>();
        private Style style;

        public Builder(Context context, String channelId) {
            n.channelId = channelId;
        }

        /// The pre-Oreo constructor: no channel.
        public Builder(Context context) {
            this(context, null);
        }

        public Builder setContentTitle(CharSequence title) {
            n.extras.putCharSequence(EXTRA_TITLE, title);
            return this;
        }

        public Builder setContentText(CharSequence text) {
            n.extras.putCharSequence(EXTRA_TEXT, text);
            return this;
        }

        public Builder setSubText(CharSequence text) {
            n.extras.putCharSequence(EXTRA_SUB_TEXT, text);
            return this;
        }

        public Builder setSmallIcon(int icon) {
            n.icon = icon;
            return this;
        }

        public Builder setLargeIcon(android.graphics.Bitmap b) {
            return this;
        }

        public Builder setTicker(CharSequence tickerText) {
            n.tickerText = tickerText;
            return this;
        }

        public Builder setWhen(long when) {
            n.when = when;
            return this;
        }

        public Builder setShowWhen(boolean show) {
            return this;
        }

        public Builder setUsesChronometer(boolean b) {
            return this;
        }

        public Builder setNumber(int number) {
            n.number = number;
            return this;
        }

        public Builder setContentIntent(PendingIntent intent) {
            n.contentIntent = intent;
            return this;
        }

        public Builder setDeleteIntent(PendingIntent intent) {
            n.deleteIntent = intent;
            return this;
        }

        public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) {
            return this;
        }

        private Builder flag(int mask, boolean value) {
            if (value) {
                n.flags |= mask;
            } else {
                n.flags &= ~mask;
            }
            return this;
        }

        public Builder setOngoing(boolean ongoing) {
            return flag(FLAG_ONGOING_EVENT, ongoing);
        }

        public Builder setAutoCancel(boolean autoCancel) {
            return flag(FLAG_AUTO_CANCEL, autoCancel);
        }

        public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
            return flag(FLAG_ONLY_ALERT_ONCE, onlyAlertOnce);
        }

        public Builder setPriority(int pri) {
            n.priority = pri;
            return this;
        }

        public Builder setCategory(String category) {
            n.category = category;
            return this;
        }

        public Builder setVisibility(int visibility) {
            n.visibility = visibility;
            return this;
        }

        public Builder setColor(int argb) {
            n.color = argb;
            return this;
        }

        public Builder setColorized(boolean colorize) {
            return this;
        }

        public Builder setDefaults(int defaults) {
            n.defaults = defaults;
            return this;
        }

        public Builder setSound(android.net.Uri sound) {
            return this;
        }

        public Builder setVibrate(long[] pattern) {
            return this;
        }

        public Builder setLights(int argb, int onMs, int offMs) {
            return this;
        }

        public Builder setGroup(String groupKey) {
            n.group = groupKey;
            return this;
        }

        public Builder setGroupSummary(boolean isGroupSummary) {
            n.groupSummary = isGroupSummary;
            return flag(FLAG_GROUP_SUMMARY, isGroupSummary);
        }

        public Builder setSortKey(String sortKey) {
            return this;
        }

        public Builder setChannelId(String channelId) {
            n.channelId = channelId;
            return this;
        }

        public Builder setTimeoutAfter(long durationMs) {
            n.timeoutAfter = durationMs;
            return this;
        }

        public Builder setProgress(int max, int progress, boolean indeterminate) {
            n.progressMax = max;
            n.progress = progress;
            n.progressIndeterminate = indeterminate;
            n.extras.putInt(EXTRA_PROGRESS, progress);
            n.extras.putInt(EXTRA_PROGRESS_MAX, max);
            n.extras.putBoolean(EXTRA_PROGRESS_INDETERMINATE, indeterminate);
            return this;
        }

        public Builder setForegroundServiceBehavior(int behavior) {
            return this;
        }

        public Builder setLocalOnly(boolean localOnly) {
            return this;
        }

        public Builder setStyle(Style s) {
            if (style != s) {
                style = s;
                if (s != null) {
                    s.setBuilder(this);
                }
            }
            return this;
        }

        public Builder addAction(int icon, CharSequence title, PendingIntent intent) {
            actions.add(new Action(icon, title, intent));
            return this;
        }

        public Builder addAction(Action action) {
            actions.add(action);
            return this;
        }

        public Builder setExtras(Bundle extras) {
            if (extras != null) {
                n.extras.putAll(extras);
            }
            return this;
        }

        public Bundle getExtras() {
            return n.extras;
        }

        public Notification build() {
            Notification out = copy(n);
            out.actions = actions.toArray(new Action[actions.size()]);
            if (style != null) {
                style.apply(out);
            }
            return out;
        }

        /// Deprecated alias of [#build()].
        public Notification getNotification() {
            return build();
        }

        private static Notification copy(Notification src) {
            Notification out = new Notification();
            out.when = src.when;
            out.icon = src.icon;
            out.number = src.number;
            out.tickerText = src.tickerText;
            out.contentIntent = src.contentIntent;
            out.deleteIntent = src.deleteIntent;
            out.flags = src.flags;
            out.defaults = src.defaults;
            out.priority = src.priority;
            out.visibility = src.visibility;
            out.color = src.color;
            out.category = src.category;
            out.extras = new Bundle(src.extras);
            out.channelId = src.channelId;
            out.group = src.group;
            out.groupSummary = src.groupSummary;
            out.progress = src.progress;
            out.progressMax = src.progressMax;
            out.progressIndeterminate = src.progressIndeterminate;
            out.timeoutAfter = src.timeoutAfter;
            return out;
        }
    }
}
