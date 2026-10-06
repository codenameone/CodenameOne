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
package com.codename1.initializr;

import com.codename1.system.NativeInterface;

public interface WebsiteThemeNative extends NativeInterface {
    boolean isDarkMode();
    void notifyUiReady();
    /// Saves the generated project through the browser and, once the save was
    /// handed to the browser, tells the embedding website page. packageName and
    /// template ride along in that message only so the page can send an
    /// anonymous download beacon (a SHA-256 of the package name, never the name
    /// itself) that BuildCloud can later join to the first cloud build of the
    /// same package. They do not influence the download.
    boolean downloadProject(String fileName, String dataUrl, String packageName, String template);

    /// Horizontal clearance, in CSS pixels, that the host page's chat launcher
    /// (Crisp) currently needs at the bottom-right, or 0 when it is hidden. The
    /// generate button is shifted left by this amount so the launcher does not
    /// cover it.
    int chatLauncherClearance();

    /// Whether this Initializr is embedded in the Codename One website itself,
    /// the only page allowed to receive an email address from it. A third-party
    /// page can frame the public /initializr-app/ and would otherwise collect
    /// every address typed into the panel, so the email field is shown only when
    /// this answers true.
    boolean canRequestSteps();

    /// Asks the embedding website page to email the next steps to `email`, once.
    /// The page forwards it to BuildCloud (docs/website/assets/js/
    /// cn1-initializr-beacon.js), which may follow up if that project never
    /// reaches a first build. ide and build (see GeneratorModel#buildKind) pick
    /// the steps BuildCloud sends -- the same ones the panel shows; the page
    /// hashes the package name before anything leaves the browser. Returns true
    /// only when the page confirmed it sent the request.
    boolean requestSteps(String email, String packageName, String template, String ide, String build);
}
