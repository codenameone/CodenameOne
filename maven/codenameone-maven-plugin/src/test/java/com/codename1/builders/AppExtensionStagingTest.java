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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppExtensionStagingTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void archivesLeaveTheResourcesDirectory() throws Exception {
        File res = tmp.newFolder("res");
        write(new File(res, "WalletUIExtension.ios.appext"));
        write(new File(res, "WalletNonUIExtension.ios.appext"));
        write(new File(res, "theme.res"));

        File staged = IPhoneBuilder.stageAppExtensionArchives(res, new File(tmp.getRoot(), "appext"));

        // resDir is handed to the translator, which copies it into <main>-src and turns every
        // file into an app resource. An archive left here ships inside the .app.
        assertFalse(new File(res, "WalletUIExtension.ios.appext").exists());
        assertFalse(new File(res, "WalletNonUIExtension.ios.appext").exists());
        assertTrue(new File(staged, "WalletUIExtension.ios.appext").isFile());
        assertTrue(new File(staged, "WalletNonUIExtension.ios.appext").isFile());
    }

    @Test
    public void everythingElseStaysWhereItIs() throws Exception {
        File res = tmp.newFolder("res");
        write(new File(res, "WalletUIExtension.ios.appext"));
        write(new File(res, "theme.res"));
        write(new File(res, "notes.appext.txt"));

        IPhoneBuilder.stageAppExtensionArchives(res, new File(tmp.getRoot(), "appext"));

        assertTrue(new File(res, "theme.res").isFile());
        assertTrue(new File(res, "notes.appext.txt").isFile());
        assertEquals(2, res.listFiles().length);
    }

    @Test
    public void noArchiveMeansNoStagingDirectory() throws Exception {
        File res = tmp.newFolder("res");
        write(new File(res, "theme.res"));

        File stagingDir = new File(tmp.getRoot(), "appext");
        assertNull(IPhoneBuilder.stageAppExtensionArchives(res, stagingDir));
        assertFalse(stagingDir.exists());
    }

    @Test
    public void aSymlinkOutOfTheExtensionIsFound() throws Exception {
        File extension = tmp.newFolder("dist", "WalletUIExtension");
        File outside = new File(tmp.getRoot(), "secret.mobileprovision");
        write(outside);
        // Everything under an extension folder is handed to Xcode and copied into the app, so a
        // link out of it would ship a file from the build machine inside the customer's IPA.
        Files.createSymbolicLink(new File(extension, "notes.txt").toPath(), outside.toPath());

        File found = IPhoneBuilder.symlinkEscaping(extension, extension);

        assertTrue(found != null && "notes.txt".equals(found.getName()));
    }

    @Test
    public void aSymlinkFoundDeeperDownIsFoundToo() throws Exception {
        File extension = tmp.newFolder("dist", "WalletUIExtension");
        File nested = new File(extension, "Resources");
        assertTrue(nested.mkdirs());
        File outside = new File(tmp.getRoot(), "secret.mobileprovision");
        write(outside);
        Files.createSymbolicLink(new File(nested, "logo.png").toPath(), outside.toPath());

        assertTrue(IPhoneBuilder.symlinkEscaping(extension, extension) != null);
    }

    @Test
    public void anOrdinaryExtensionPasses() throws Exception {
        File extension = tmp.newFolder("dist", "WalletUIExtension");
        write(new File(extension, "Info.plist"));
        File nested = new File(extension, "Base.lproj");
        assertTrue(nested.mkdirs());
        write(new File(nested, "MainInterface.storyboard"));
        // A link that stays inside the extension is not an escape.
        Files.createSymbolicLink(new File(extension, "alias.plist").toPath(),
                new File(extension, "Info.plist").toPath());

        assertNull(IPhoneBuilder.symlinkEscaping(extension, extension));
    }

    private static void write(File file) throws Exception {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write("PK".getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    @Test
    public void anInTreeDirectoryCycleIsRefused() throws Exception {
        File extension = tmp.newFolder("dist", "WalletUIExtension");
        File sub = new File(extension, "sub");
        assertTrue(sub.mkdirs());
        // sub/loop -> . escapes nothing, and every walk over the folder follows it until the
        // stack ends the build.
        Files.createSymbolicLink(new File(sub, "loop").toPath(), extension.toPath());

        assertTrue(IPhoneBuilder.symlinkEscaping(extension, extension) != null);
    }

    @Test
    public void anInTreeFileLinkIsStillFine() throws Exception {
        File extension = tmp.newFolder("dist2", "WalletUIExtension");
        write(new File(extension, "Info.plist"));
        Files.createSymbolicLink(new File(extension, "alias.plist").toPath(),
                new File(extension, "Info.plist").toPath());
        assertNull(IPhoneBuilder.symlinkEscaping(extension, extension));
    }

    @Test
    public void theFilesystemRootIsNotADeveloperDirectory() throws Exception {
        File fakeRoot = tmp.newFolder("fakeroot");
        assertTrue(new File(fakeRoot, "usr/bin").mkdirs());
        assertTrue(new File(fakeRoot, "usr/bin/xcodebuild").createNewFile());

        // Two levels up from /usr/bin/xcodebuild -- the shim `which xcodebuild` reports -- is the
        // root, which has usr/bin and is not a developer directory. DEVELOPER_DIR=/ makes xcrun
        // fail, the SDK name falls back to the unversioned "iphoneos", and an exact
        // [sdk=iphoneosNN] condition is then decided by map order.
        assertFalse(IPhoneBuilder.isDeveloperDir(fakeRoot));

        File developer = tmp.newFolder("Xcode.app-Contents-Developer");
        assertTrue(new File(developer, "usr/bin").mkdirs());
        assertTrue(new File(developer, "usr/bin/xcodebuild").createNewFile());
        assertTrue(new File(developer, "Platforms").mkdirs());
        assertTrue(IPhoneBuilder.isDeveloperDir(developer));
        assertFalse(IPhoneBuilder.isDeveloperDir(null));
    }
}
