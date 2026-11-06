/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.async;

/// dart:async's ZoneSpecification: the handlers a forked zone overrides. Each is a
/// Dart function value (a Funcs lambda) taking (self, parent delegate, zone, ...), or
/// null to defer to the parent zone. Modelled: handleUncaughtError, print, run and
/// registerCallback; the timer and microtask interceptors are not.
public final class ZoneSpecification {
    Object handleUncaughtError;
    Object print;
    Object run;
    Object registerCallback;

    public ZoneSpecification() {
        this(null, null, null, null);
    }

    public ZoneSpecification(Object handleUncaughtError, Object print, Object run, Object registerCallback) {
        this.handleUncaughtError = handleUncaughtError;
        this.print = print;
        this.run = run;
        this.registerCallback = registerCallback;
    }

    // The transpiler builds an external class's named constructor arguments as a
    // no-argument construction followed by one call per argument named after it.
    public ZoneSpecification handleUncaughtError(Object handler) {
        this.handleUncaughtError = handler;
        return this;
    }

    public ZoneSpecification print(Object handler) {
        this.print = handler;
        return this;
    }

    public ZoneSpecification run(Object handler) {
        this.run = handler;
        return this;
    }

    public ZoneSpecification registerCallback(Object handler) {
        this.registerCallback = handler;
        return this;
    }

    /// {@code ZoneSpecification.from(other, handleUncaughtError: ...)}: other with the
    /// given handlers replacing its own.
    public static ZoneSpecification from(ZoneSpecification other, Object handleUncaughtError, Object print,
            Object run, Object registerCallback) {
        if (other == null) {
            return new ZoneSpecification(handleUncaughtError, print, run, registerCallback);
        }
        return new ZoneSpecification(
                handleUncaughtError != null ? handleUncaughtError : other.handleUncaughtError,
                print != null ? print : other.print,
                run != null ? run : other.run,
                registerCallback != null ? registerCallback : other.registerCallback);
    }
}
