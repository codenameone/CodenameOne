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
package com.codename1.builders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppExtensionResourcesTest {

    @TempDir
    Path tmp;

    @Test
    void assetCatalogIsAddedAsOneResource() throws Exception {
        File extension = new File(tmp.toFile(), "WalletUIExtension");
        assertTrue(extension.mkdir());
        write(new File(extension, "WalletView.swift"));
        write(new File(extension, "config.json"));

        File catalog = new File(extension, "Assets.xcassets");
        File logo = new File(catalog, "Logo.imageset");
        File icon = new File(catalog, "Icon.imageset");
        assertTrue(logo.mkdirs());
        assertTrue(icon.mkdirs());
        write(new File(catalog, "Contents.json"));
        write(new File(logo, "Contents.json"));
        write(new File(logo, "logo.png"));
        write(new File(icon, "Contents.json"));
        write(new File(icon, "icon.png"));

        StringBuilder script = new StringBuilder();
        IPhoneBuilder.appendFilesToXcodeProjGroup(script, extension,
                "service_group", "service_target", extension.getParentFile());
        String ruby = script.toString();

        assertTrue(ruby.contains("service_group.new_file('WalletUIExtension/Assets.xcassets')\n"
                + "service_target.add_resources([fileref])"));
        assertTrue(ruby.contains("service_group.new_file('WalletUIExtension/WalletView.swift')"));
        assertTrue(ruby.contains("service_group.new_file('WalletUIExtension/config.json')"));
        assertFalse(ruby.contains("Contents.json"));
        assertFalse(ruby.contains("logo.png"));
        assertFalse(ruby.contains("icon.png"));
    }

    private static void write(File file) throws Exception {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write("{}".getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }
}
