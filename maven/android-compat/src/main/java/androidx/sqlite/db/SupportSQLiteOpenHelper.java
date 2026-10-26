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
package androidx.sqlite.db;

import android.content.Context;

/// Opens, creates and upgrades a database, as `SQLiteOpenHelper` does.
public interface SupportSQLiteOpenHelper {

    String getDatabaseName();

    void setWriteAheadLoggingEnabled(boolean enabled);

    SupportSQLiteDatabase getWritableDatabase();

    SupportSQLiteDatabase getReadableDatabase();

    void close();

    /// The schema hooks the helper calls while it opens a database.
    abstract class Callback {

        public final int version;

        public Callback(int version) {
            this.version = version;
        }

        public void onConfigure(SupportSQLiteDatabase db) {
        }

        public abstract void onCreate(SupportSQLiteDatabase db);

        public abstract void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion);

        public void onDowngrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
            throw new android.database.sqlite.SQLiteException("Can't downgrade database from version "
                    + oldVersion + " to " + newVersion);
        }

        public void onOpen(SupportSQLiteDatabase db) {
        }

        public void onCorruption(SupportSQLiteDatabase db) {
        }
    }

    /// What a factory needs to create a helper.
    final class Configuration {

        public final Context context;
        public final String name;
        public final Callback callback;
        public final boolean useNoBackupDirectory;
        public final boolean allowDataLossOnRecovery;

        public Configuration(Context context, String name, Callback callback) {
            this(context, name, callback, false, false);
        }

        public Configuration(Context context, String name, Callback callback, boolean useNoBackupDirectory,
                             boolean allowDataLossOnRecovery) {
            this.context = context;
            this.name = name;
            this.callback = callback;
            this.useNoBackupDirectory = useNoBackupDirectory;
            this.allowDataLossOnRecovery = allowDataLossOnRecovery;
        }

        public static Builder builder(Context context) {
            return new Builder(context);
        }

        /// Builds a [Configuration].
        public static final class Builder {
            private final Context context;
            private String name;
            private Callback callback;
            private boolean noBackup;
            private boolean allowDataLoss;

            Builder(Context context) {
                this.context = context;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder callback(Callback callback) {
                this.callback = callback;
                return this;
            }

            public Builder noBackupDirectory(boolean noBackup) {
                this.noBackup = noBackup;
                return this;
            }

            public Builder allowDataLossOnRecovery(boolean allow) {
                this.allowDataLoss = allow;
                return this;
            }

            public Configuration build() {
                if (callback == null) {
                    throw new IllegalArgumentException("Must set a callback to create the configuration.");
                }
                if (context == null) {
                    throw new IllegalArgumentException("Must set a non-null context to create the configuration.");
                }
                return new Configuration(context, name, callback, noBackup, allowDataLoss);
            }
        }
    }

    /// Creates helpers.
    interface Factory {
        SupportSQLiteOpenHelper create(Configuration configuration);
    }
}
