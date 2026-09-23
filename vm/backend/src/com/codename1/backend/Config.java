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
package com.codename1.backend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Where a server's settings come from, in one resolution order.
 *
 * <p>The shape is the one a Spring Boot developer already knows, because the
 * problem is the same one: the SAME code has to run against a SQLite file on a
 * laptop and a managed PostgreSQL in production, and the difference between those
 * two cannot live in the source. It lives here.
 *
 * <pre>
 *   # application.properties, committed
 *   cn1.datasource.url=${DATABASE_URL}
 *   cn1.server.port=8080
 *
 *   # application-dev.properties, also committed
 *   cn1.datasource.url=:memory:
 * </pre>
 *
 * <pre>
 *   CN1_PROFILE=dev ./server                       # SQLite, nothing installed
 *   DATABASE_URL=postgres://user:pw@db/app ./server # production
 * </pre>
 *
 * A key is looked for in this order, and the first layer that has it wins:
 *
 * <ol>
 *   <li>a system property of exactly that name ({@code -Dcn1.server.port=9000}),
 *       which only the local JVM loop can set;</li>
 *   <li>an environment variable of the name in upper case with dots as
 *       underscores ({@code CN1_SERVER_PORT}), which is how a container sets
 *       one;</li>
 *   <li>the environment variable a platform already sets for it, where one
 *       exists: {@code PORT} and {@code DATABASE_URL} are set for you by every
 *       PaaS worth the name, and a server that ignored them would need a
 *       wrapper script to start at all;</li>
 *   <li>{@code application-<profile>.properties};</li>
 *   <li>{@code application.properties};</li>
 *   <li>the default the caller passed in.</li>
 * </ol>
 *
 * <p>A value may reference an environment variable as {@code ${NAME}} or
 * {@code ${NAME:fallback}}. That resolution happens when the value is READ
 * rather than when the file is loaded, which is what lets a committed
 * application.properties name a variable that only production sets: the dev
 * profile overrides the key, so the unset variable is never looked at. A
 * reference that IS read and cannot be resolved is an error rather than a value
 * with a dollar sign in it -- the alternative is a server that tries to open a
 * SQLite file named "${DATABASE_URL}".
 *
 * <p>Nothing here is required. A binary with no properties file beside it reads
 * its whole configuration from the environment, which is the normal shape for a
 * container built FROM SCRATCH: there is no file next to the binary because there
 * is nothing next to the binary.
 */
public final class Config {
    /** Which profile is active. Defaults to "default". */
    public static final String PROFILE = "cn1.profile";
    /** The directory the properties files are read from. Defaults to ".". */
    public static final String LOCATION = "cn1.config.location";

    /** The port to listen on. Also read from PORT. */
    public static final String SERVER_PORT = "cn1.server.port";
    /** The listen backlog. */
    public static final String SERVER_BACKLOG = "cn1.server.backlog";
    /** The size of the request thread pool. */
    public static final String SERVER_WORKERS = "cn1.server.workers";
    /** How long a stop waits for requests in flight, in milliseconds. */
    public static final String SERVER_SHUTDOWN_MILLIS = "cn1.server.shutdownTimeoutMillis";
    /** A PEM certificate chain to terminate TLS with. */
    public static final String TLS_CERTIFICATE = "cn1.server.tls.certificate";
    /** The private key for {@link #TLS_CERTIFICATE}. */
    public static final String TLS_KEY = "cn1.server.tls.key";
    /** Whether to offer HTTP/2 through ALPN when TLS is terminated here. */
    public static final String TLS_HTTP2 = "cn1.server.tls.http2";

    /** A directory to serve static files from. */
    public static final String STATIC_ROOT = "cn1.static.root";
    /** The path prefix those files are served under. Defaults to /static. */
    public static final String STATIC_PREFIX = "cn1.static.prefix";
    /** The file a directory request is answered with. Defaults to index.html. */
    public static final String STATIC_INDEX = "cn1.static.index";
    /** The Cache-Control header those files carry. */
    public static final String STATIC_CACHE_CONTROL = "cn1.static.cacheControl";

    /**
     * The database, as a SQLite path or a PostgreSQL or MySQL URL. Also read from
     * DATABASE_URL.
     */
    public static final String DATASOURCE_URL = "cn1.datasource.url";
    /** How many connections the pool holds. */
    public static final String DATASOURCE_POOL_SIZE = "cn1.datasource.pool.size";
    /** How long a borrow waits for a free connection, in milliseconds. */
    public static final String DATASOURCE_BORROW_MILLIS = "cn1.datasource.pool.borrowTimeoutMillis";
    /** How long SQLite waits on a locked database, in milliseconds. */
    public static final String DATASOURCE_BUSY_MILLIS = "cn1.datasource.busyTimeoutMillis";
    /**
     * Whether the generated daos create their tables at start-up. Defaults to
     * true on a development profile and false everywhere else: a laptop wants a
     * schema without being asked, and production wants its migrations run by
     * whatever runs migrations.
     */
    public static final String ORM_CREATE_TABLES = "cn1.orm.createTables";

    /** The profiles that mean "this is somebody's laptop or a test". */
    private static final String[] DEVELOPMENT_PROFILES = {"dev", "development", "test", "local"};

    /**
     * The environment variables a platform sets whether or not it has heard of
     * Codename One. Pairs of key then variable.
     */
    private static final String[] WELL_KNOWN_ENVIRONMENT = {
        SERVER_PORT, "PORT",
        DATASOURCE_URL, "DATABASE_URL",
        // The OpenTelemetry SDK's own variables, which every collector's
        // documentation, every operator and every deployment template already
        // uses. The keys are com.codename1.backend.otel.OtlpTracer's; they are
        // spelled out here because this table is where the environment is mapped.
        "cn1.otel.disabled", "OTEL_SDK_DISABLED",
        "cn1.otel.endpoint", "OTEL_EXPORTER_OTLP_ENDPOINT",
        "cn1.otel.traces.endpoint", "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT",
        "cn1.otel.headers", "OTEL_EXPORTER_OTLP_HEADERS",
        "cn1.otel.traces.headers", "OTEL_EXPORTER_OTLP_TRACES_HEADERS",
        "cn1.otel.protocol", "OTEL_EXPORTER_OTLP_PROTOCOL",
        "cn1.otel.traces.protocol", "OTEL_EXPORTER_OTLP_TRACES_PROTOCOL",
        "cn1.otel.service.name", "OTEL_SERVICE_NAME",
        "cn1.otel.resource.attributes", "OTEL_RESOURCE_ATTRIBUTES",
        "cn1.otel.sampler", "OTEL_TRACES_SAMPLER",
        "cn1.otel.sampler.arg", "OTEL_TRACES_SAMPLER_ARG",
        "cn1.otel.queue.size", "OTEL_BSP_MAX_QUEUE_SIZE",
        "cn1.otel.batch.size", "OTEL_BSP_MAX_EXPORT_BATCH_SIZE",
        "cn1.otel.export.delayMillis", "OTEL_BSP_SCHEDULE_DELAY",
    };

    private final Properties profileFile;
    private final Properties baseFile;
    private final String profile;
    private final List loadedFrom;

    private Config(Properties baseFile, Properties profileFile, String profile, List loadedFrom) {
        this.baseFile = baseFile;
        this.profileFile = profileFile;
        this.profile = profile;
        this.loadedFrom = loadedFrom;
    }

    /**
     * Reads the configuration for this process: the active profile, then the two
     * properties files, from {@link #LOCATION} or the working directory.
     */
    public static Config load() throws IOException {
        String location = fromProcess(LOCATION);
        return load(location == null ? "." : location);
    }

    /** Reads the configuration from properties files in {@code directory}. */
    public static Config load(String directory) throws IOException {
        List loadedFrom = new ArrayList();
        Properties base = read(directory, "application.properties", loadedFrom);
        // The profile is settled BEFORE the profile file is read, and the base
        // file gets a vote: a project whose default is development says so once,
        // in the file, rather than in every developer's shell.
        String profile = fromProcess(PROFILE);
        if(profile == null) {
            profile = base.getProperty(PROFILE);
            if(profile != null) {
                // EXPANDED, like every other value read from a file. Without
                // this, "cn1.profile=${CN1_DEFAULT_PROFILE:dev}" made the
                // literal text the profile name: the server looked for
                // application-${CN1_DEFAULT_PROFILE:dev}.properties, found
                // nothing, and ran on a profile that is not a development one --
                // so the in-memory datasource and table creation were off and
                // the reason was a filename nobody reads.
                //
                // Against the BASE FILE ALONE, because that is everything which
                // exists at this point: the profile file has not been chosen
                // yet, and a profile referring to a key inside the file it
                // selects would be circular.
                profile = new Config(base, new Properties(), "default", new ArrayList())
                        .expand(profile, PROFILE, 0);
            }
        }
        if(profile == null || profile.length() == 0) {
            profile = "default";
        }
        Properties profileFile = read(directory, "application-" + profile + ".properties",
                loadedFrom);
        return new Config(base, profileFile, profile, loadedFrom);
    }

    /**
     * A configuration with no files behind it, holding exactly what it is given.
     * The process environment still wins over it, for the same reason it wins
     * over a file: the deployment has the last word.
     */
    public static Config of(Properties values, String profile) {
        Properties empty = new Properties();
        return new Config(values == null ? empty : values, empty,
                profile == null || profile.length() == 0 ? "default" : profile,
                new ArrayList());
    }

    /** The active profile: "default" unless something named another. */
    public String getProfile() {
        return profile;
    }

    /**
     * Whether the active profile is a development one -- dev, development, test
     * or local.
     *
     * <p>This decides two defaults and nothing else: an unconfigured database
     * becomes an in-memory SQLite one rather than a refusal, and the ORM creates
     * its tables. Both are wrong in production and right on a laptop, and both
     * are overridable by naming the key.
     */
    public boolean isDevelopmentProfile() {
        for(int iter = 0 ; iter < DEVELOPMENT_PROFILES.length ; iter++) {
            if(DEVELOPMENT_PROFILES[iter].equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /** The value for {@code key}, or null when no layer has one. */
    public String get(String key) throws IOException {
        return get(key, null);
    }

    /** The value for {@code key}, or {@code fallback} when no layer has one. */
    public String get(String key, String fallback) throws IOException {
        String raw = raw(key);
        if(raw == null) {
            return fallback;
        }
        return expand(raw, key, 0);
    }

    /** The value for {@code key} as a number, or {@code fallback}. */
    public int getInt(String key, int fallback) throws IOException {
        String value = get(key);
        if(value == null || value.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException err) {
            throw new IOException(key + " must be a number and is '" + value + "'");
        }
    }

    /**
     * The value for {@code key} as a flag, or {@code fallback}.
     *
     * <p>"true", "yes", "on" and "1" are true; "false", "no", "off" and "0" are
     * false; anything else is an error rather than false. A setting the operator
     * spelled "ture" is a setting they believe is on.
     */
    public boolean getBoolean(String key, boolean fallback) throws IOException {
        String value = get(key);
        if(value == null || value.length() == 0) {
            return fallback;
        }
        String trimmed = value.trim();
        if(trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("yes")
                || trimmed.equalsIgnoreCase("on") || "1".equals(trimmed)) {
            return true;
        }
        if(trimmed.equalsIgnoreCase("false") || trimmed.equalsIgnoreCase("no")
                || trimmed.equalsIgnoreCase("off") || "0".equals(trimmed)) {
            return false;
        }
        throw new IOException(key + " must be true or false and is '" + value + "'");
    }

    /**
     * What was read, for a start-up line. NEVER any value: the datasource URL
     * holds a password, and a configuration dump is how it reaches a log.
     */
    public String describe() {
        StringBuilder out = new StringBuilder("profile=");
        out.append(profile);
        if(loadedFrom.isEmpty()) {
            out.append(", no properties file, configured from the environment");
            return out.toString();
        }
        for(int iter = 0 ; iter < loadedFrom.size() ; iter++) {
            out.append(iter == 0 ? ", read " : ", ").append(loadedFrom.get(iter));
        }
        return out.toString();
    }

    /** The value as written, before any ${} in it is resolved. */
    private String raw(String key) {
        String value = fromProcess(key);
        if(value != null) {
            return value;
        }
        for(int iter = 0 ; iter < WELL_KNOWN_ENVIRONMENT.length ; iter += 2) {
            if(WELL_KNOWN_ENVIRONMENT[iter].equals(key)) {
                value = environment(WELL_KNOWN_ENVIRONMENT[iter + 1]);
                if(value != null) {
                    return value;
                }
            }
        }
        value = profileFile.getProperty(key);
        if(value != null) {
            return value;
        }
        return baseFile.getProperty(key);
    }

    /**
     * A system property of that name, then the environment variable it maps to.
     * The mapping is the conventional one -- upper case, dots and dashes to
     * underscores -- so cn1.datasource.url is CN1_DATASOURCE_URL.
     */
    private static String fromProcess(String key) {
        String value = System.getProperty(key);
        if(value != null && value.length() > 0) {
            return value;
        }
        return environment(environmentName(key));
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        return value == null || value.length() == 0 ? null : value;
    }

    /**
     * The environment variable name for a key.
     *
     * <p>Hand-folded rather than String.toUpperCase, which is locale sensitive:
     * on a Turkish device the i of "cn1" folds to a dotted capital I and the
     * variable the deployment set is never found. The runtime has no Locale to
     * ask for the root one, so the fold is written out.
     */
    static String environmentName(String key) {
        StringBuilder out = new StringBuilder(key.length());
        for(int iter = 0 ; iter < key.length() ; iter++) {
            char c = key.charAt(iter);
            if(c == '.' || c == '-') {
                out.append('_');
            } else if(c >= 'a' && c <= 'z') {
                out.append((char)(c - 'a' + 'A'));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * {@code value} with every ${NAME} and ${NAME:fallback} resolved against the
     * process environment, then against system properties, then against this
     * configuration's own keys.
     *
     * <p>Depth is bounded because a value may reference a key that references it
     * back, and the failure of an unbounded expansion is a stack overflow at
     * start-up rather than a message naming the two keys.
     */
    private String expand(String value, String key, int depth) throws IOException {
        int at = value.indexOf("${");
        if(at < 0) {
            return value;
        }
        if(depth > 8) {
            throw new IOException(key + " expands through more than eight references, "
                    + "so two of them refer to each other");
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        int from = 0;
        while(at >= 0) {
            int end = value.indexOf('}', at + 2);
            if(end < 0) {
                // THE KEY AND THE POSITION, NEVER THE VALUE. This one is reached
                // with cn1.datasource.url in hand more often than with anything
                // else, and that value carries a password: an uncaught start-up
                // failure prints its message straight into a deployment log,
                // which undoes the care describe() takes for the same reason.
                //
                // The two messages below name a key and a reference and no value
                // at all, so they are left as they are; getInt and getBoolean do
                // quote what they were given, and that is deliberate -- the value
                // of a key declared to be a number or a flag is what the operator
                // needs to see, and is not a credential.
                throw new IOException(key + " holds a '${' at index " + at
                        + " that is never closed");
            }
            out.append(value, from, at);
            String reference = value.substring(at + 2, end);
            String fallback = null;
            int colon = reference.indexOf(':');
            if(colon >= 0) {
                fallback = reference.substring(colon + 1);
                reference = reference.substring(0, colon);
            }
            String resolved = environment(reference);
            if(resolved == null) {
                resolved = System.getProperty(reference);
            }
            if(resolved == null || resolved.length() == 0) {
                String nested = raw(reference);
                resolved = nested == null ? null : expand(nested, reference, depth + 1);
            }
            if(resolved == null) {
                if(fallback == null) {
                    // LOUDLY. Left alone, the caller opens a database named
                    // "${DATABASE_URL}" -- or, worse, a SQLite file by that name,
                    // which succeeds and is empty.
                    throw new IOException(key + " refers to ${" + reference + "}, which is "
                            + "not set in the environment or the configuration. Set it, or "
                            + "give it a fallback as ${" + reference + ":value}.");
                }
                resolved = fallback;
            }
            out.append(resolved);
            from = end + 1;
            at = value.indexOf("${", from);
        }
        out.append(value, from, value.length());
        return out.toString();
    }

    /**
     * One properties file, or an empty set when it is not there. Read through
     * {@link FileIo} rather than java.io, because that is the file reader this
     * runtime implements on both arms.
     */
    private static Properties read(String directory, String name, List loadedFrom)
            throws IOException {
        Properties out = new Properties();
        String path = directory == null || directory.length() == 0 || ".".equals(directory)
                ? name : directory + "/" + name;
        byte[] content = readFile(path);
        if(content == null) {
            return out;
        }
        // Through a Reader with the encoding named, on both arms. The two
        // java.util.Properties implementations disagree about load(InputStream):
        // the JDK's reads ISO-8859-1 and this runtime's reads UTF-8, so a
        // password with an accent in it would be a different password in the
        // development loop than in the binary that ships.
        out.load(new InputStreamReader(new ByteArrayInputStream(content), "UTF-8"));
        loadedFrom.add(path);
        return out;
    }

    /** A whole file as bytes, or null when it cannot be opened. */
    private static byte[] readFile(String path) throws IOException {
        int fd = FileIo.openRead(path);
        if(fd == FileIo.OPEN_FAILED) {
            // NOT THE SAME AS ABSENT. Every setting this file carries would fall
            // back to an environment variable or a default, and the ones that
            // have no default are the ones that matter: a deployment naming its
            // TLS certificate and key here came up in PLAINTEXT when the file's
            // permissions were wrong, reporting nothing. An optional file that
            // is not there is a choice; one that is there and cannot be read is
            // a broken deployment, and it says so before serving anything.
            throw new IOException(path + " exists and could not be opened. A configuration "
                    + "file that is present must be readable: every setting in it would "
                    + "otherwise fall back to a default, silently.");
        }
        if(fd < 0) {
            return null;
        }
        try {
            // FOUR, not three: the fourth is the file's identity, which the
            // post-read check compares so an atomic replace is caught as well as
            // an in-place rewrite. A three-long array here would leave the
            // before-identity at 0 against a real after-identity and refuse every
            // configuration file there is.
            long[] info = new long[4];
            if(info.length >= 3 && FileIo.stat(fd, info) >= 0 && info[2] == 1) {
                // A DIRECTORY IS NOT AN ABSENT FILE. openRead gives a descriptor
                // for one -- StaticFiles needs that, to stat it and retry at the
                // index -- so a bad ConfigMap or volume mount that put a
                // directory where application.properties belongs read as "no
                // such file" and every file-based setting silently became an
                // environment default, plaintext included when both TLS paths
                // lived in that file.
                throw new IOException(path + " is a directory, not a properties file. "
                        + "A configuration file that is present must be a regular file: "
                        + "every setting in it would otherwise fall back to a default, "
                        + "silently.");
            }
            if(FileIo.stat(fd, info) < 0) {
                // NOT ABSENT EITHER. The descriptor is already OPEN, so the file
                // exists and this is the filesystem failing to describe it -- an
                // I/O error, or a stale handle on a network or container mount
                // that went away underneath us. Reading that as "no configuration"
                // is the same silent fallback the open failure above refuses:
                // every setting in the file becomes an environment default, and a
                // deployment naming its TLS certificate and key there comes up in
                // plaintext because the mount blinked.
                throw new IOException(path + " is open but its size could not be read. "
                        + "A configuration file that is present must be readable: every "
                        + "setting in it would otherwise fall back to a default, "
                        + "silently.");
            }
            long size = info[0];
            // Kept for the check after the read: a rewrite that replaces the file
            // with one of the SAME LENGTH passes both guards below -- the buffer
            // fills exactly and nothing is beyond it -- while what is in the
            // buffer can be part of the old file and part of the new. See the
            // re-stat below.
            long modifiedBefore = info[1];
            // AND WHICH FILE IT WAS. mtime alone misses an atomic replace: a new
            // application.properties moved over the path can carry any timestamp,
            // the old one included, and then the comparison below sees no change
            // in a file that is not even the same file. info is four long, so the
            // identity is already there.
            long identityBefore = info[3];
            // A configuration file is kilobytes. The ceiling is here because the
            // size comes from the filesystem and this allocates it: a device node
            // or a truncated-then-growing file should fail with a message rather
            // than an OutOfMemoryError.
            if(size > 1024L * 1024L) {
                throw new IOException(path + " is " + size + " bytes, which is far larger "
                        + "than a properties file; refusing to read it");
            }
            byte[] out = new byte[(int)size];
            int filled = 0;
            while(filled < out.length) {
                int read = FileIo.read(fd, out, filled, out.length - filled);
                if(read <= 0) {
                    break;
                }
                filled += read;
            }
            if(filled == out.length) {
                // AND NOTHING BEYOND IT. The size came from the stat, and a file
                // rewritten in place while this runs is longer by the time the
                // read reaches the end -- so filling the buffer proves only that
                // the FIRST `size` bytes arrived, not that they are the whole
                // file. A rewrite that truncates and repopulates would hand back
                // a valid-looking prefix, or an empty one, with everything past
                // the old length missing: the same silent loss the short read
                // below produces, arrived at from the other side.
                //
                // One byte is enough to tell. At end of file the read answers
                // zero or less, which is the ordinary case and costs a syscall.
                //
                // The growth itself has no test and cannot have one from here:
                // FileIo captures the size when the descriptor opens, so a test
                // would have to append between that open and this read. What
                // IS covered is that an ordinary file still loads -- every
                // ConfigTest case goes through this line -- so the guard cannot
                // be refusing everything.
                byte[] beyond = new byte[1];
                if(FileIo.read(fd, beyond, 0, 1) > 0) {
                    throw new IOException(path + " grew while it was being read, so what "
                            + "was loaded is the first " + out.length + " bytes of a file "
                            + "that is now longer. It is being rewritten underneath this "
                            + "process; start again once it has settled.");
                }
                // AND THE FILE IS STILL THE ONE THAT WAS MEASURED. The two guards
                // above both key off LENGTH, so the rewrite they cannot see is the
                // one that keeps it: a deployment tool writing a new
                // application.properties over the old one in place, where the
                // buffer ends up part old and part new. It parses -- every
                // complete line in it is a setting -- so the result is a
                // configuration that never existed, and the pairing that matters
                // is an old TLS certificate path with a new key.
                //
                // The modification time is what tells, and it is checked on the
                // SAME DESCRIPTOR, so this asks about the file that was read
                // rather than about whatever the path names now.
                //
                // NOT AIRTIGHT, and worth saying so: a filesystem that stamps
                // mtime to the second cannot distinguish a rewrite that lands
                // inside the same second as the open. It closes the window that
                // is actually open -- a tool writing a file after this process
                // started reading it -- rather than every window.
                // A FAILED stat HERE IS A FAILED CHECK, not a passed one. The
                // pre-read stat two branches up is already fatal for the same
                // reason -- the descriptor is open, so the file exists and the
                // filesystem is failing to describe it -- and letting the
                // post-read one fall through to success would leave the stability
                // check unperformed on exactly the mounts that need it, which is
                // where a rewrite underneath a reader actually happens.
                // statFresh, NOT stat. On the Java SE runtime stat() answers
                // from the snapshot taken when the descriptor opened -- asset
                // serving needs that, so a response describes the bytes it will
                // stream -- which means a pre-read and a post-read stat return
                // the IDENTICAL captured values and this comparison could never
                // be unequal. The check read as passed on a runtime where it had
                // never run, while the native runtime's live fstat did perform
                // it. statFresh re-reads on both.
                //
                // Measured on Java SE with the descriptor held open across an
                // in-place rewrite of the same length: stat() answered the same
                // mtime before and after (changed=false), statFresh() answered
                // the new one (changed=true), and after a replace-by-rename the
                // identity moved too.
                long[] after = new long[4];
                if(FileIo.statFresh(fd, after) < 0) {
                    throw new IOException(path + " was read but could not be checked for "
                            + "a rewrite: its size and modification time are no longer "
                            + "readable through the open descriptor. What was loaded "
                            + "cannot be shown to be one version of the file, and every "
                            + "setting in it would otherwise fall back to a default, "
                            + "silently.");
                }
                // WHAT THIS CATCHES DEPENDS ON THE RUNTIME, and saying so beats
                // implying it is universal. statFresh re-reads the PATH on Java
                // SE, so a replace-by-rename shows up as a different identity
                // there. On the native runtime it is an fstat of the open
                // DESCRIPTOR, which pins the inode, so the value cannot change
                // and this comparison is inert. That is the tolerable half: a
                // replaced file leaves the descriptor holding a whole, consistent
                // version of the old one, which is stale rather than corrupt. The
                // mtime check below is the one that catches the corrupting case
                // -- an in-place rewrite of the same length -- and it works on
                // both.
                if(after[3] != identityBefore) {
                    throw new IOException(path + " was REPLACED while it was being "
                            + "read: the name now refers to a different file than the "
                            + "one these bytes came from. What was loaded is a whole "
                            + "version of the old file, but it is not the configuration "
                            + "this process was told to use; start again once the "
                            + "deployment has settled.");
                }
                if(after[1] != modifiedBefore) {
                    throw new IOException(path + " was rewritten while it was being "
                            + "read, so what was loaded may be part of the old file and "
                            + "part of the new. It is the same length either way, which "
                            + "is why nothing else here noticed; start again once it has "
                            + "settled.");
                }
                return out;
            }
            // A PREFIX IS NOT A SHORTER FILE. It parses perfectly well -- every
            // complete line in it is a setting -- so the ones past the cut point
            // simply vanish, and a deployment whose TLS certificate and key sit
            // near the end of application.properties came up in PLAINTEXT with
            // nothing to read about it. The file can genuinely shrink between
            // the stat and the read, which is a file being rewritten underneath
            // the server; refusing is the right answer to that too.
            throw new IOException(path + " is " + out.length + " bytes and only " + filled
                    + " could be read. A configuration file read in part is not a shorter "
                    + "configuration file: the settings past the cut would silently fall "
                    + "back to their defaults.");
        } finally {
            FileIo.close(fd);
        }
    }
}
