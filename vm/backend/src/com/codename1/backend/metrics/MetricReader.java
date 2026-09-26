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
package com.codename1.backend.metrics;

import java.io.IOException;

import com.codename1.backend.Config;

/**
 * Something that reads {@link Metrics} periodically and sends them somewhere --
 * the OTLP exporter. Installed by the generated entry point of a build that
 * enables OpenTelemetry, and referenced nowhere else, so a build that does not
 * leaves the exporter out of the binary.
 */
public interface MetricReader {
    /**
     * Reads the configuration and starts. Answers false when the deployment has
     * metrics turned off, in which case nothing is started.
     */
    boolean open(Config config) throws IOException;

    /** Sends what is left and stops, waiting up to {@code timeoutMillis}. */
    void shutdown(int timeoutMillis);
}
