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
package com.codename1.compat.jdk;

import com.codename1.io.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/// `java.util.logging.Logger` for the Codename One runtime, writing through
/// `com.codename1.io.Log`.
///
/// `Log` has four levels where the JDK has seven, so they fold: `SEVERE` is
/// an error, `WARNING` a warning, `INFO` and `CONFIG` are information, and
/// the three `FINE` levels are debug output. A record carries its logger's
/// name in front of the message, and an attached exception goes to
/// `Log.e`, which is what prints a stack trace on a device.
///
/// A logger with no level of its own logs `INFO` and above, the JDK's
/// default; `Log`'s own level then filters once more. There are no
/// handlers, filters, formatters or parents to configure: a call that would
/// install one has no method here, and the build reports it.
public class Logger {

    public static final String GLOBAL_LOGGER_NAME = "global";

    private static final Map<String, Logger> LOGGERS = new HashMap<String, Logger>();

    private final String name;
    private Level level;

    protected Logger(String name, String resourceBundleName) {
        this.name = name;
    }

    public static Logger getLogger(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        Logger logger = LOGGERS.get(name);
        if (logger == null) {
            logger = new Logger(name, null);
            LOGGERS.put(name, logger);
        }
        return logger;
    }

    /// The bundle is not consulted: messages are logged as they are given.
    public static Logger getLogger(String name, String resourceBundleName) {
        return getLogger(name);
    }

    public static Logger getGlobal() {
        return getLogger(GLOBAL_LOGGER_NAME);
    }

    public static Logger getAnonymousLogger() {
        return new Logger(null, null);
    }

    public String getName() {
        return name;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level newLevel) {
        this.level = newLevel;
    }

    public Logger getParent() {
        return null;
    }

    public void setUseParentHandlers(boolean useParentHandlers) {
        // There are no handlers; see the class description.
    }

    public boolean getUseParentHandlers() {
        return false;
    }

    public boolean isLoggable(Level l) {
        int floor = (level == null ? Level.INFO : level).intValue();
        return l.intValue() >= floor && floor != Level.OFF.intValue();
    }

    private static int logLevel(Level l) {
        int value = l.intValue();
        if (value >= Level.SEVERE.intValue()) {
            return Log.ERROR;
        }
        if (value >= Level.WARNING.intValue()) {
            return Log.WARNING;
        }
        if (value >= Level.CONFIG.intValue()) {
            return Log.INFO;
        }
        return Log.DEBUG;
    }

    public void log(Level l, String msg) {
        if (isLoggable(l)) {
            Log.p(name == null ? String.valueOf(msg) : name + ": " + msg, logLevel(l));
        }
    }

    public void log(Level l, Supplier<String> msgSupplier) {
        if (isLoggable(l)) {
            log(l, msgSupplier.get());
        }
    }

    public void log(Level l, String msg, Object param1) {
        log(l, msg, new Object[] {param1});
    }

    /// Logs `msg` with each `{n}` replaced by parameter `n`, as the JDK's
    /// formatter does.
    public void log(Level l, String msg, Object[] params) {
        if (!isLoggable(l)) {
            return;
        }
        String text = msg;
        if (text != null && params != null && params.length > 0 && text.indexOf('{') >= 0) {
            try {
                text = MessageFormat.format(text, params);
            } catch (IllegalArgumentException e) {
                text = msg;
            }
        }
        log(l, text);
    }

    public void log(Level l, String msg, Throwable thrown) {
        if (isLoggable(l)) {
            log(l, msg);
            if (thrown != null) {
                Log.e(thrown);
            }
        }
    }

    public void log(Level l, Throwable thrown, Supplier<String> msgSupplier) {
        if (isLoggable(l)) {
            log(l, msgSupplier.get(), thrown);
        }
    }

    public void logp(Level l, String sourceClass, String sourceMethod, String msg) {
        log(l, msg);
    }

    public void logp(Level l, String sourceClass, String sourceMethod, String msg, Object param1) {
        log(l, msg, param1);
    }

    public void logp(Level l, String sourceClass, String sourceMethod, String msg, Object[] params) {
        log(l, msg, params);
    }

    public void logp(Level l, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
        log(l, msg, thrown);
    }

    public void entering(String sourceClass, String sourceMethod) {
        log(Level.FINER, "ENTRY " + sourceClass + "." + sourceMethod);
    }

    public void entering(String sourceClass, String sourceMethod, Object param1) {
        log(Level.FINER, "ENTRY " + sourceClass + "." + sourceMethod + " " + param1);
    }

    public void exiting(String sourceClass, String sourceMethod) {
        log(Level.FINER, "RETURN " + sourceClass + "." + sourceMethod);
    }

    public void exiting(String sourceClass, String sourceMethod, Object result) {
        log(Level.FINER, "RETURN " + sourceClass + "." + sourceMethod + " " + result);
    }

    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        log(Level.FINER, "THROW " + sourceClass + "." + sourceMethod, thrown);
    }

    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    public void warning(String msg) {
        log(Level.WARNING, msg);
    }

    public void info(String msg) {
        log(Level.INFO, msg);
    }

    public void config(String msg) {
        log(Level.CONFIG, msg);
    }

    public void fine(String msg) {
        log(Level.FINE, msg);
    }

    public void finer(String msg) {
        log(Level.FINER, msg);
    }

    public void finest(String msg) {
        log(Level.FINEST, msg);
    }

    public void severe(Supplier<String> msgSupplier) {
        log(Level.SEVERE, msgSupplier);
    }

    public void warning(Supplier<String> msgSupplier) {
        log(Level.WARNING, msgSupplier);
    }

    public void info(Supplier<String> msgSupplier) {
        log(Level.INFO, msgSupplier);
    }

    public void config(Supplier<String> msgSupplier) {
        log(Level.CONFIG, msgSupplier);
    }

    public void fine(Supplier<String> msgSupplier) {
        log(Level.FINE, msgSupplier);
    }

    public void finer(Supplier<String> msgSupplier) {
        log(Level.FINER, msgSupplier);
    }

    public void finest(Supplier<String> msgSupplier) {
        log(Level.FINEST, msgSupplier);
    }
}
