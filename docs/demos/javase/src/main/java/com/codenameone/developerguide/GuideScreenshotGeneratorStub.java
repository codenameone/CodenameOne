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

package com.codenameone.developerguide;

import com.codenameone.developerguide.screenshots.FigureDevice;
import com.codenameone.developerguide.screenshots.GuideFigureRenderer;
import com.codenameone.developerguide.screenshots.GuideFigures;
import com.codenameone.developerguide.screenshots.PreAdvancedThemingScreenshots;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * JavaSE entry point used by docs automation to regenerate guide screenshots.
 */
public final class GuideScreenshotGeneratorStub {
    private GuideScreenshotGeneratorStub() {
    }

    public static void main(String[] args) throws Exception {
        generateInto(new File(args.length > 0 ? args[0] : "target/generated-guide-screenshots"),
                args.length > 1 ? args[1] : "schematic");
    }

    public static void generateInto(File outputDirectory, String mode) throws IOException {
        System.out.println("Generating guide screenshots (" + mode + ") into: "
                + outputDirectory.getAbsolutePath());
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IOException("Unable to create screenshot output directory: " + outputDirectory);
        }
        final File target = outputDirectory;
        if ("schematic".equals(mode)) {
            PreAdvancedThemingScreenshots.generate(new PreAdvancedThemingScreenshots.ScreenshotSink() {
                @Override
                public OutputStream open(String fileName) throws IOException {
                    System.out.println("Generating guide screenshot: " + fileName);
                    return new FileOutputStream(new File(target, fileName));
                }
            });
            return;
        }
        GuideFigureRenderer.render(new GuideFigureRenderer.ScreenshotSink() {
            @Override
            public OutputStream open(String fileName) throws IOException {
                System.out.println("Generating guide figure: " + fileName);
                return new FileOutputStream(new File(target, fileName));
            }
        }, FigureDevice.fromKey(mode), GuideFigures.variants());
    }
}
