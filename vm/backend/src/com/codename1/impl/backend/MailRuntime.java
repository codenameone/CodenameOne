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
package com.codename1.impl.backend;

import com.codename1.backend.mvc.Model;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/// What the generated wiring gives the mail support, the compiled templates,
/// and what the mail support gives the management endpoint, its state.
///
/// Both go through here so that neither side names the other: a server that
/// sends no mail links no mail code for the sake of its health report.
///
/// The templates of an application compile into a class the build generates,
/// which the framework cannot name. The wiring installs a renderer over it
/// for an application that has mail templates; one without them installs
/// nothing and links no template code for mail.
public final class MailRuntime {
    private static final AtomicReference<TemplateRenderer> RENDERER =
            new AtomicReference<TemplateRenderer>();

    private static final AtomicReference<Probe> PROBE = new AtomicReference<Probe>();

    private MailRuntime() {
    }

    /// The state of an open mail support, read by the health report and the
    /// outbox gauge.
    public interface Probe {
        /// The transport's name, such as `smtp`.
        String transport();

        /// Whether the transport reached its server the last time it tried.
        boolean isHealthy();

        /// Mails staged and not yet handed to the transport.
        int pending();
    }

    /// Called by the mail support when it opens, never by application code.
    public static void opened(Probe probe) {
        PROBE.set(probe);
    }

    /// Called by the mail support when it closes.
    public static void closed(Probe probe) {
        PROBE.compareAndSet(probe, null);
    }

    /// The open mail support, or null when this server has sent no mail.
    public static Probe probe() {
        return PROBE.get();
    }

    /// Renders a mail's HTML from the application's compiled templates.
    public interface TemplateRenderer {
        /// The HTML of `templates/<view>.html` as UTF-8.
        byte[] render(String view, Model model) throws IOException;
    }

    /// Called by the generated wiring, never by application code.
    public static void install(TemplateRenderer renderer) {
        RENDERER.set(renderer);
    }

    /// The installed renderer, or null.
    public static TemplateRenderer renderer() {
        return RENDERER.get();
    }
}
