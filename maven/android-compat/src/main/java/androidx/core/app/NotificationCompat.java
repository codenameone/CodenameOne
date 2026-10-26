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
package androidx.core.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;

/// AndroidX's notification builder, over the platform one: every setter
/// passes straight through, so what [Notification] carries over to a Codename
/// One local notification is the same either way.
public class NotificationCompat {

    public static final int PRIORITY_DEFAULT = Notification.PRIORITY_DEFAULT;
    public static final int PRIORITY_LOW = Notification.PRIORITY_LOW;
    public static final int PRIORITY_MIN = Notification.PRIORITY_MIN;
    public static final int PRIORITY_HIGH = Notification.PRIORITY_HIGH;
    public static final int PRIORITY_MAX = Notification.PRIORITY_MAX;
    public static final int VISIBILITY_PUBLIC = Notification.VISIBILITY_PUBLIC;
    public static final int VISIBILITY_PRIVATE = Notification.VISIBILITY_PRIVATE;
    public static final int VISIBILITY_SECRET = Notification.VISIBILITY_SECRET;
    public static final int DEFAULT_ALL = Notification.DEFAULT_ALL;
    public static final int DEFAULT_SOUND = Notification.DEFAULT_SOUND;
    public static final int DEFAULT_VIBRATE = Notification.DEFAULT_VIBRATE;
    public static final int DEFAULT_LIGHTS = Notification.DEFAULT_LIGHTS;
    public static final int FLAG_ONGOING_EVENT = Notification.FLAG_ONGOING_EVENT;
    public static final int FLAG_AUTO_CANCEL = Notification.FLAG_AUTO_CANCEL;
    public static final int FLAG_FOREGROUND_SERVICE = Notification.FLAG_FOREGROUND_SERVICE;
    public static final int FOREGROUND_SERVICE_DEFAULT = Notification.FOREGROUND_SERVICE_DEFAULT;
    public static final int FOREGROUND_SERVICE_IMMEDIATE = Notification.FOREGROUND_SERVICE_IMMEDIATE;
    public static final int FOREGROUND_SERVICE_DEFERRED = Notification.FOREGROUND_SERVICE_DEFERRED;
    public static final String CATEGORY_SERVICE = Notification.CATEGORY_SERVICE;
    public static final String CATEGORY_PROGRESS = Notification.CATEGORY_PROGRESS;
    public static final String CATEGORY_NAVIGATION = Notification.CATEGORY_NAVIGATION;
    public static final String CATEGORY_MESSAGE = Notification.CATEGORY_MESSAGE;
    public static final String CATEGORY_ALARM = Notification.CATEGORY_ALARM;
    public static final String CATEGORY_REMINDER = Notification.CATEGORY_REMINDER;
    public static final String CATEGORY_STATUS = Notification.CATEGORY_STATUS;
    public static final String CATEGORY_ERROR = Notification.CATEGORY_ERROR;
    public static final String CATEGORY_LOCATION_SHARING = Notification.CATEGORY_LOCATION_SHARING;
    public static final String EXTRA_TITLE = Notification.EXTRA_TITLE;
    public static final String EXTRA_TEXT = Notification.EXTRA_TEXT;

    public static Bundle getExtras(Notification notification) {
        return notification.extras;
    }

    public static class Action extends Notification.Action {
        public Action(int icon, CharSequence title, PendingIntent intent) {
            super(icon, title, intent);
        }
    }

    public abstract static class Style {
        Notification.Style platform;

        public void setBuilder(Builder builder) {
            if (builder != null) {
                builder.setStyle(this);
            }
        }
    }

    public static class BigTextStyle extends Style {
        private final Notification.BigTextStyle style = new Notification.BigTextStyle();

        public BigTextStyle() {
            platform = style;
        }

        public BigTextStyle(Builder builder) {
            this();
            setBuilder(builder);
        }

        public BigTextStyle bigText(CharSequence cs) {
            style.bigText(cs);
            return this;
        }

        public BigTextStyle setBigContentTitle(CharSequence title) {
            style.setBigContentTitle(title);
            return this;
        }

        public BigTextStyle setSummaryText(CharSequence cs) {
            return this;
        }
    }

    public static class InboxStyle extends Style {
        private final Notification.InboxStyle style = new Notification.InboxStyle();

        public InboxStyle() {
            platform = style;
        }

        public InboxStyle(Builder builder) {
            this();
            setBuilder(builder);
        }

        public InboxStyle addLine(CharSequence cs) {
            style.addLine(cs);
            return this;
        }

        public InboxStyle setBigContentTitle(CharSequence title) {
            return this;
        }

        public InboxStyle setSummaryText(CharSequence cs) {
            return this;
        }
    }

    public static class Builder {
        private final Notification.Builder b;

        public Builder(Context context, String channelId) {
            b = new Notification.Builder(context, channelId);
        }

        /// Deprecated in AndroidX: no channel.
        public Builder(Context context) {
            b = new Notification.Builder(context);
        }

        public Builder setContentTitle(CharSequence title) {
            b.setContentTitle(title);
            return this;
        }

        public Builder setContentText(CharSequence text) {
            b.setContentText(text);
            return this;
        }

        public Builder setSubText(CharSequence text) {
            b.setSubText(text);
            return this;
        }

        public Builder setSmallIcon(int icon) {
            b.setSmallIcon(icon);
            return this;
        }

        public Builder setLargeIcon(android.graphics.Bitmap icon) {
            b.setLargeIcon(icon);
            return this;
        }

        public Builder setTicker(CharSequence text) {
            b.setTicker(text);
            return this;
        }

        public Builder setWhen(long when) {
            b.setWhen(when);
            return this;
        }

        public Builder setShowWhen(boolean show) {
            b.setShowWhen(show);
            return this;
        }

        public Builder setUsesChronometer(boolean u) {
            b.setUsesChronometer(u);
            return this;
        }

        public Builder setNumber(int number) {
            b.setNumber(number);
            return this;
        }

        public Builder setContentIntent(PendingIntent intent) {
            b.setContentIntent(intent);
            return this;
        }

        public Builder setDeleteIntent(PendingIntent intent) {
            b.setDeleteIntent(intent);
            return this;
        }

        public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) {
            b.setFullScreenIntent(intent, highPriority);
            return this;
        }

        public Builder setOngoing(boolean ongoing) {
            b.setOngoing(ongoing);
            return this;
        }

        public Builder setAutoCancel(boolean autoCancel) {
            b.setAutoCancel(autoCancel);
            return this;
        }

        public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
            b.setOnlyAlertOnce(onlyAlertOnce);
            return this;
        }

        public Builder setSilent(boolean silent) {
            return this;
        }

        public Builder setPriority(int pri) {
            b.setPriority(pri);
            return this;
        }

        public Builder setCategory(String category) {
            b.setCategory(category);
            return this;
        }

        public Builder setVisibility(int visibility) {
            b.setVisibility(visibility);
            return this;
        }

        public Builder setColor(int argb) {
            b.setColor(argb);
            return this;
        }

        public Builder setColorized(boolean colorize) {
            b.setColorized(colorize);
            return this;
        }

        public Builder setDefaults(int defaults) {
            b.setDefaults(defaults);
            return this;
        }

        public Builder setSound(android.net.Uri sound) {
            b.setSound(sound);
            return this;
        }

        public Builder setVibrate(long[] pattern) {
            b.setVibrate(pattern);
            return this;
        }

        public Builder setLights(int argb, int onMs, int offMs) {
            b.setLights(argb, onMs, offMs);
            return this;
        }

        public Builder setGroup(String groupKey) {
            b.setGroup(groupKey);
            return this;
        }

        public Builder setGroupSummary(boolean isGroupSummary) {
            b.setGroupSummary(isGroupSummary);
            return this;
        }

        public Builder setSortKey(String sortKey) {
            b.setSortKey(sortKey);
            return this;
        }

        public Builder setChannelId(String channelId) {
            b.setChannelId(channelId);
            return this;
        }

        public Builder setTimeoutAfter(long durationMs) {
            b.setTimeoutAfter(durationMs);
            return this;
        }

        public Builder setProgress(int max, int progress, boolean indeterminate) {
            b.setProgress(max, progress, indeterminate);
            return this;
        }

        public Builder setForegroundServiceBehavior(int behavior) {
            b.setForegroundServiceBehavior(behavior);
            return this;
        }

        public Builder setLocalOnly(boolean localOnly) {
            b.setLocalOnly(localOnly);
            return this;
        }

        public Builder setStyle(Style style) {
            b.setStyle(style == null ? null : style.platform);
            return this;
        }

        public Builder addAction(int icon, CharSequence title, PendingIntent intent) {
            b.addAction(icon, title, intent);
            return this;
        }

        public Builder addAction(Action action) {
            b.addAction(action);
            return this;
        }

        public Builder setExtras(Bundle extras) {
            b.setExtras(extras);
            return this;
        }

        public Bundle getExtras() {
            return b.getExtras();
        }

        public Notification build() {
            return b.build();
        }
    }
}
