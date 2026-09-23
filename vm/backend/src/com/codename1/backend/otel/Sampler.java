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
package com.codename1.backend.otel;

import java.io.IOException;

/**
 * The OpenTelemetry samplers named by {@code OTEL_TRACES_SAMPLER}:
 * {@code always_on}, {@code always_off}, {@code traceidratio} and the three
 * {@code parentbased_} forms, which follow the caller's decision when there is a
 * caller and apply the named rule only to a new trace.
 *
 * <p>Parent-based is the default, and it matters most for a mobile backend: the
 * app decides once, at the edge, and every service the request touches agrees,
 * so a trace is either whole or absent rather than missing its middle.
 */
final class Sampler {
    private final boolean parentBased;
    /** For a new trace: 1 always, 0 never, otherwise a ratio. */
    private final double ratio;
    private final long bound;

    private Sampler(boolean parentBased, double ratio) {
        this.parentBased = parentBased;
        this.ratio = ratio;
        this.bound = ratio >= 1 ? Long.MAX_VALUE : (long)(ratio * (double)Long.MAX_VALUE);
    }

    /** From the sampler's name and its argument, which is the ratio for the ratio forms. */
    static Sampler parse(String name, String argument) throws IOException {
        String n = name == null || name.trim().length() == 0 ? "parentbased_always_on" : name.trim();
        double ratio = 1;
        if(argument != null && argument.trim().length() > 0) {
            try {
                ratio = Double.parseDouble(argument.trim());
            } catch (NumberFormatException err) {
                throw new IOException("The sampler argument must be a number between 0 and 1 "
                        + "and is '" + argument + "'");
            }
            if(!(ratio >= 0 && ratio <= 1)) {
                throw new IOException("The sampler argument must be between 0 and 1 and is "
                        + argument);
            }
        }
        if("always_on".equals(n)) {
            return new Sampler(false, 1);
        }
        if("always_off".equals(n)) {
            return new Sampler(false, 0);
        }
        if("traceidratio".equals(n)) {
            return new Sampler(false, ratio);
        }
        if("parentbased_always_on".equals(n)) {
            return new Sampler(true, 1);
        }
        if("parentbased_always_off".equals(n)) {
            return new Sampler(true, 0);
        }
        if("parentbased_traceidratio".equals(n)) {
            return new Sampler(true, ratio);
        }
        // Refused rather than defaulted: a deployment that asked for a sampler
        // this does not know believes it is getting it.
        throw new IOException("Unknown sampler '" + n + "'. Use always_on, always_off, "
                + "traceidratio, parentbased_always_on, parentbased_always_off or "
                + "parentbased_traceidratio.");
    }

    /**
     * Whether to record a span.
     *
     * <p>The ratio compares the LOW 64 bits of the trace id, which is what the
     * other OpenTelemetry SDKs compare, so every service in a trace that samples
     * at the same ratio makes the same decision about it without being told.
     */
    boolean sample(boolean hasParent, boolean parentSampled, long traceLo) {
        if(parentBased && hasParent) {
            return parentSampled;
        }
        if(ratio >= 1) {
            return true;
        }
        if(ratio <= 0) {
            return false;
        }
        return (traceLo & Long.MAX_VALUE) < bound;
    }
}
