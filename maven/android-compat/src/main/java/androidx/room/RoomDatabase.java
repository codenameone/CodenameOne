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
package androidx.room;

import android.content.Context;
import android.database.Cursor;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import com.codename1.androidcompat.runtime.MainThread;
import com.codename1.androidcompat.runtime.RoomFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/// The base of an application's `@Database` class. The build generates the
/// subclass (`Name_Impl`) that knows the schema; this class opens the
/// database, creates the tables on the first open, runs the migrations a
/// version change needs and checks that the schema did not change without
/// one, as Room does.
public abstract class RoomDatabase {

    public static final int MAX_BIND_PARAMETER_CNT = 999;
    static final String MASTER_TABLE = "room_master_table";
    private static final int MASTER_ID = 42;

    private SupportSQLiteOpenHelper mOpenHelper;
    private InvalidationTracker mInvalidationTracker;
    private boolean mAllowMainThreadQueries;
    private Executor mQueryExecutor;
    private Executor mTransactionExecutor;
    private final Map<Class<?>, Object> mTypeConverters = new HashMap<Class<?>, Object>();
    private List<Callback> mCallbacks;
    private MigrationContainer mMigrations;
    private boolean mDestructive;
    private boolean mDestructiveOnDowngrade;
    private Set<Integer> mDestructiveFrom;
    private boolean mOpen;
    /// Live queries recompute on the main thread when that is where the
    /// change happened; they are exempt from the main-thread check, which
    /// is there for the application's own calls. Touched on the main thread
    /// only.

    public RoomDatabase() {
    }

    // ---------------------------------------------------------- generated

    /// The `version` of the `@Database` annotation.
    protected abstract int roomSchemaVersion();

    /// The statements creating every table and index, in order.
    protected abstract String[] roomCreateStatements();

    /// The entity tables.
    protected abstract String[] roomTableNames();

    /// Each entity table followed by its columns, which a migration must
    /// leave in place; null skips the check.
    protected String[][] roomTableColumns() {
        return null;
    }

    /// A new instance of a `@TypeConverters` class the application did not
    /// supply, or null for one it must (`@ProvidedTypeConverter`).
    protected Object roomCreateTypeConverter(Class<?> converterClass) {
        return null;
    }

    // ---------------------------------------------------------- lifecycle

    void init(Builder<?> b) {
        mAllowMainThreadQueries = b.mAllowMainThreadQueries;
        mQueryExecutor = b.mQueryExecutor;
        mTransactionExecutor = b.mTransactionExecutor != null ? b.mTransactionExecutor : b.mQueryExecutor;
        mCallbacks = b.mCallbacks;
        mMigrations = b.mMigrationContainer;
        mDestructive = b.mDestructive;
        mDestructiveOnDowngrade = b.mDestructiveOnDowngrade;
        mDestructiveFrom = b.mDestructiveFrom;
        for (Object c : b.mTypeConverters) {
            mTypeConverters.put(c.getClass(), c);
        }
        mInvalidationTracker = new InvalidationTracker(this, roomTableNames());
        SupportSQLiteOpenHelper.Factory factory = b.mFactory != null ? b.mFactory
                : new FrameworkSQLiteOpenHelperFactory();
        mOpenHelper = factory.create(new SupportSQLiteOpenHelper.Configuration(b.mContext, b.mName,
                new OpenCallback(roomSchemaVersion())));
        mOpen = true;
    }

    public SupportSQLiteOpenHelper getOpenHelper() {
        return mOpenHelper;
    }

    public InvalidationTracker getInvalidationTracker() {
        return mInvalidationTracker;
    }

    public Executor getQueryExecutor() {
        return mQueryExecutor;
    }

    public Executor getTransactionExecutor() {
        return mTransactionExecutor;
    }

    public boolean isOpen() {
        return mOpen;
    }

    public void close() {
        if (mOpen) {
            mOpen = false;
            mOpenHelper.close();
        }
    }

    /// The instance of a `@TypeConverters` class: the one the builder was
    /// given, or one created for it.
    @SuppressWarnings("unchecked")
    public <T> T getTypeConverter(Class<T> klass) {
        Object c = mTypeConverters.get(klass);
        if (c == null) {
            c = roomCreateTypeConverter(klass);
            if (c == null) {
                throw new IllegalArgumentException("A required type converter (" + klass.getName()
                        + ") for " + getClass().getName() + " is missing in the database configuration.");
            }
            mTypeConverters.put(klass, c);
        }
        return (T) c;
    }

    // ---------------------------------------------------------- access

    /// Throws on the main thread unless `allowMainThreadQueries()` was set.
    public void assertNotMainThread() {
        if (mAllowMainThreadQueries) {
            return;
        }
        if (MainThread.isMainThread() && com.codename1.ui.Display.isInitialized()) {
            throw new IllegalStateException("Cannot access database on the main thread since"
                    + " it may potentially lock the UI for a long period of time.");
        }
    }

    public Cursor query(String query, Object[] args) {
        return query(new SimpleSQLiteQuery(query, args));
    }

    public Cursor query(SupportSQLiteQuery query) {
        assertNotMainThread();
        return mOpenHelper.getWritableDatabase().query(query);
    }

    public SupportSQLiteStatement compileStatement(String sql) {
        assertNotMainThread();
        return mOpenHelper.getWritableDatabase().compileStatement(sql);
    }

    public void beginTransaction() {
        assertNotMainThread();
        mOpenHelper.getWritableDatabase().beginTransaction();
    }

    public void endTransaction() {
        SupportSQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.endTransaction();
        if (!db.inTransaction()) {
            mInvalidationTracker.flush();
        }
    }

    public void setTransactionSuccessful() {
        mOpenHelper.getWritableDatabase().setTransactionSuccessful();
    }

    public boolean inTransaction() {
        return mOpen && mOpenHelper.getWritableDatabase().inTransaction();
    }

    public void runInTransaction(Runnable body) {
        beginTransaction();
        try {
            body.run();
            setTransactionSuccessful();
        } finally {
            endTransaction();
        }
    }

    public <V> V runInTransaction(Callable<V> body) {
        beginTransaction();
        try {
            V result = body.call();
            setTransactionSuccessful();
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            RoomDatabase.<RuntimeException>sneakyThrow(e);
            return null;
        } finally {
            endTransaction();
        }
    }

    /// Rethrows a checked exception unwrapped, as Room's `SneakyThrow` does.
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    /// Deletes every row of every entity table, foreign keys deferred.
    public void clearAllTables() {
        assertNotMainThread();
        SupportSQLiteDatabase db = mOpenHelper.getWritableDatabase();
        boolean fk = hasForeignKeys();
        try {
            if (fk) {
                db.execSQL("PRAGMA defer_foreign_keys = TRUE");
            }
            db.beginTransaction();
            try {
                for (String t : roomTableNames()) {
                    db.execSQL("DELETE FROM `" + t + "`");
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            if (!db.inTransaction()) {
                db.execSQL("VACUUM");
            }
        }
        mInvalidationTracker.notifyTablesChanged(roomTableNames());
    }

    // ---------------------------------------------------------- schema

    private boolean hasForeignKeys() {
        for (String s : roomCreateStatements()) {
            if (s.indexOf(" REFERENCES ") >= 0) {
                return true;
            }
        }
        return false;
    }

    /// The schema's fingerprint, stored in `room_master_table`. A database
    /// opened at its version with another fingerprint changed its schema
    /// without a version bump.
    String identityHash() {
        StringBuilder all = new StringBuilder();
        for (String s : roomCreateStatements()) {
            all.append(s).append('\n');
        }
        String text = all.toString();
        long h = 1125899906842597L;
        for (int i = 0; i < text.length(); i++) {
            h = 31 * h + text.charAt(i);
        }
        String hex = Long.toHexString(h);
        while (hex.length() < 16) {
            hex = "0" + hex;
        }
        return hex + Integer.toHexString(text.length());
    }

    void createAllTables(SupportSQLiteDatabase db) {
        for (String s : roomCreateStatements()) {
            db.execSQL(s);
        }
        writeIdentity(db);
    }

    private void writeIdentity(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + MASTER_TABLE + " (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO " + MASTER_TABLE + " (id,identity_hash) VALUES(" + MASTER_ID + ", '"
                + identityHash() + "')");
    }

    private void dropAllTables(SupportSQLiteDatabase db) {
        for (String t : roomTableNames()) {
            db.execSQL("DROP TABLE IF EXISTS `" + t + "`");
        }
        db.execSQL("DROP TABLE IF EXISTS " + MASTER_TABLE);
    }

    void migrate(SupportSQLiteDatabase db, int from, int to) {
        List<Migration> path = mMigrations.findMigrationPath(from, to);
        if (path != null) {
            for (Migration m : path) {
                m.migrate(db);
            }
            validateSchema(db);
            writeIdentity(db);
            return;
        }
        boolean downgrade = from > to;
        boolean allowed = mDestructive || (downgrade && mDestructiveOnDowngrade)
                || (mDestructiveFrom != null && mDestructiveFrom.contains(Integer.valueOf(from)));
        if (!allowed) {
            throw new IllegalStateException("A migration from " + from + " to " + to + " was required but not"
                    + " found. Please provide the necessary Migration path via"
                    + " RoomDatabase.Builder.addMigration(Migration ...) or allow for destructive migrations via"
                    + " one of the RoomDatabase.Builder.fallbackToDestructiveMigration* methods.");
        }
        dropAllTables(db);
        if (mCallbacks != null) {
            for (Callback c : mCallbacks) {
                c.onDestructiveMigration(db);
            }
        }
        createAllTables(db);
    }

    /// Room refuses a migration that left a table without the columns its
    /// entity declares, rather than stamping the database valid and failing
    /// at the first query. This compares column names; types, nullability and
    /// indices are not checked.
    private void validateSchema(SupportSQLiteDatabase db) {
        String[][] expected = roomTableColumns();
        if (expected == null) {
            return;
        }
        for (String[] table : expected) {
            java.util.Set<String> want = new java.util.TreeSet<String>();
            for (int i = 1; i < table.length; i++) {
                want.add(InvalidationTracker.asciiLower(table[i]));
            }
            java.util.Set<String> found = new java.util.TreeSet<String>();
            Cursor c = db.query("PRAGMA table_info(`" + table[0] + "`)");
            try {
                int name = c.getColumnIndex("name");
                while (c.moveToNext()) {
                    found.add(InvalidationTracker.asciiLower(c.getString(name)));
                }
            } finally {
                c.close();
            }
            if (!found.equals(want)) {
                throw new IllegalStateException("Migration didn't properly handle: " + table[0]
                        + ".\n Expected columns: " + want + "\n Found columns: " + found);
            }
        }
    }

    void checkIdentity(SupportSQLiteDatabase db) {
        Cursor c = db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name='" + MASTER_TABLE + "'");
        boolean hasMaster;
        try {
            hasMaster = c.moveToFirst();
        } finally {
            c.close();
        }
        if (!hasMaster) {
            writeIdentity(db);
            return;
        }
        String found = null;
        c = db.query("SELECT identity_hash FROM " + MASTER_TABLE + " WHERE id = " + MASTER_ID + " LIMIT 1");
        try {
            if (c.moveToFirst()) {
                found = c.getString(0);
            }
        } finally {
            c.close();
        }
        String expected = identityHash();
        if (!expected.equals(found)) {
            throw new IllegalStateException("Room cannot verify the data integrity. Looks like you've changed"
                    + " schema but forgot to update the version number. You can simply fix this by increasing"
                    + " the version number. Expected identity hash: " + expected + ", found: " + found);
        }
    }

    /// The open hooks: create, migrate, verify.
    private final class OpenCallback extends SupportSQLiteOpenHelper.Callback {
        OpenCallback(int version) {
            super(version);
        }

        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            createAllTables(db);
            if (mCallbacks != null) {
                for (Callback c : mCallbacks) {
                    c.onCreate(db);
                }
            }
        }

        @Override
        public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
            migrate(db, oldVersion, newVersion);
        }

        @Override
        public void onDowngrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
            migrate(db, oldVersion, newVersion);
        }

        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            checkIdentity(db);
            if (hasForeignKeys()) {
                db.execSQL("PRAGMA foreign_keys = ON");
            }
            if (mCallbacks != null) {
                for (Callback c : mCallbacks) {
                    c.onOpen(db);
                }
            }
        }
    }

    // ---------------------------------------------------------- nested

    /// Hooks an application adds with `Builder.addCallback`.
    public abstract static class Callback {
        public void onCreate(SupportSQLiteDatabase db) {
        }

        public void onOpen(SupportSQLiteDatabase db) {
        }

        public void onDestructiveMigration(SupportSQLiteDatabase db) {
        }
    }

    /// The journal mode; accepted, and left to the platform.
    public enum JournalMode {
        AUTOMATIC, TRUNCATE, WRITE_AHEAD_LOGGING
    }

    /// The migrations a builder was given, by start and end version.
    public static class MigrationContainer {
        private final Map<Integer, Map<Integer, Migration>> mMigrations =
                new HashMap<Integer, Map<Integer, Migration>>();

        public void addMigrations(Migration... migrations) {
            for (Migration m : migrations) {
                addMigration(m);
            }
        }

        void addMigration(Migration m) {
            Integer start = Integer.valueOf(m.startVersion);
            Map<Integer, Migration> ends = mMigrations.get(start);
            if (ends == null) {
                ends = new HashMap<Integer, Migration>();
                mMigrations.put(start, ends);
            }
            ends.put(Integer.valueOf(m.endVersion), m);
        }

        /// Room's walk: from each version, the migration that goes furthest
        /// toward `end` without passing it.
        public List<Migration> findMigrationPath(int start, int end) {
            if (start == end) {
                return new ArrayList<Migration>();
            }
            boolean upgrade = end > start;
            List<Migration> result = new ArrayList<Migration>();
            int current = start;
            while (upgrade ? current < end : current > end) {
                Map<Integer, Migration> targets = mMigrations.get(Integer.valueOf(current));
                if (targets == null) {
                    return null;
                }
                Migration best = null;
                for (Map.Entry<Integer, Migration> e : targets.entrySet()) {
                    int to = e.getKey().intValue();
                    boolean usable = upgrade ? to <= end && to > current : to >= end && to < current;
                    if (usable && (best == null || (upgrade ? to > best.endVersion : to < best.endVersion))) {
                        best = e.getValue();
                    }
                }
                if (best == null) {
                    return null;
                }
                result.add(best);
                current = best.endVersion;
            }
            return result;
        }
    }

    /// Configures and creates a database.
    public static class Builder<T extends RoomDatabase> {
        final Class<T> mDatabaseClass;
        final String mName;
        final Context mContext;
        final List<Callback> mCallbacks = new ArrayList<Callback>();
        final List<Object> mTypeConverters = new ArrayList<Object>();
        final MigrationContainer mMigrationContainer = new MigrationContainer();
        boolean mAllowMainThreadQueries;
        boolean mDestructive;
        boolean mDestructiveOnDowngrade;
        Set<Integer> mDestructiveFrom;
        Executor mQueryExecutor;
        Executor mTransactionExecutor;
        SupportSQLiteOpenHelper.Factory mFactory;
        String mUnsupported;

        Builder(Context context, Class<T> klass, String name) {
            mContext = context;
            mDatabaseClass = klass;
            mName = name;
        }

        public Builder<T> addMigrations(Migration... migrations) {
            mMigrationContainer.addMigrations(migrations);
            return this;
        }

        public Builder<T> addCallback(Callback callback) {
            mCallbacks.add(callback);
            return this;
        }

        public Builder<T> addTypeConverter(Object typeConverter) {
            mTypeConverters.add(typeConverter);
            return this;
        }

        public Builder<T> allowMainThreadQueries() {
            mAllowMainThreadQueries = true;
            return this;
        }

        public Builder<T> fallbackToDestructiveMigration() {
            mDestructive = true;
            return this;
        }

        public Builder<T> fallbackToDestructiveMigration(boolean dropAllTables) {
            mDestructive = true;
            return this;
        }

        public Builder<T> fallbackToDestructiveMigrationOnDowngrade() {
            mDestructiveOnDowngrade = true;
            return this;
        }

        /// Destructive only when the database is at one of these versions.
        public Builder<T> fallbackToDestructiveMigrationFrom(int... startVersions) {
            if (mDestructiveFrom == null) {
                mDestructiveFrom = new HashSet<Integer>();
            }
            for (int v : startVersions) {
                mDestructiveFrom.add(Integer.valueOf(v));
            }
            return this;
        }

        public Builder<T> setQueryExecutor(Executor executor) {
            mQueryExecutor = executor;
            return this;
        }

        public Builder<T> setTransactionExecutor(Executor executor) {
            mTransactionExecutor = executor;
            return this;
        }

        public Builder<T> setJournalMode(JournalMode journalMode) {
            return this;
        }

        public Builder<T> openHelperFactory(SupportSQLiteOpenHelper.Factory factory) {
            mFactory = factory;
            return this;
        }

        public Builder<T> enableMultiInstanceInvalidation() {
            return this;
        }

        public Builder<T> createFromAsset(String databaseFilePath) {
            mUnsupported = "createFromAsset(\"" + databaseFilePath + "\")";
            return this;
        }

        public Builder<T> createFromFile(java.io.File databaseFile) {
            mUnsupported = "createFromFile(" + databaseFile + ")";
            return this;
        }

        public T build() {
            Object impl = RoomFactory.create(mDatabaseClass.getName());
            if (impl == null) {
                throw new RuntimeException("Cannot find implementation for " + mDatabaseClass.getName() + ". "
                        + mDatabaseClass.getName() + "_Impl does not exist");
            }
            @SuppressWarnings("unchecked")
            T db = (T) impl;
            return buildWith(db);
        }

        /// Configures an implementation the caller created.
        T buildWith(T db) {
            if (mUnsupported != null) {
                throw new UnsupportedOperationException("Room." + mUnsupported
                        + " is not supported by the Codename One Room runtime");
            }
            if (mContext == null) {
                throw new IllegalArgumentException("Cannot provide null context for the database.");
            }
            if (mQueryExecutor == null) {
                mQueryExecutor = new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        new Thread(command, "room-query").start();
                    }
                };
            }
            db.init(this);
            return db;
        }
    }
}
