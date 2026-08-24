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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The file half of the stamping: which plists get stamped, and in what encoding. */
public class AppExtensionPlistFileTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final Map<String, String> NO_SETTINGS = new HashMap<String, String>();

    private static String plist(String declaration, String extra) {
        return declaration
                + "<plist version=\"1.0\">\n"
                + "<dict>\n"
                + "\t<key>CFBundleName</key>\n"
                + "\t<string>" + extra + "</string>\n"
                + "</dict>\n"
                + "</plist>\n";
    }

    @Test
    public void aUtf16PlistIsStampedAndStaysUtf16() throws Exception {
        File extension = extension();
        File infoPlist = new File(extension, "Info.plist");
        String source = plist("<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n", "WalletUIExtension");
        // With a BOM, as a UTF-16 file is written. Read as UTF-8 this is noise, so the plist would
        // not parse and the extension would ship with no identifier at all.
        writeBytes(infoPlist, concat(new byte[]{(byte) 0xFF, (byte) 0xFE},
                source.getBytes(StandardCharsets.UTF_16LE)));

        List<String> changes = IPhoneBuilder.stampPlistFile(infoPlist, "5.4", "5.4", NO_SETTINGS);

        assertFalse(changes.isEmpty());
        byte[] written = readBytes(infoPlist);
        assertEquals((byte) 0xFF, written[0]);
        assertEquals((byte) 0xFE, written[1]);
        String out = new String(written, 2, written.length - 2, StandardCharsets.UTF_16LE);
        assertTrue(out.contains("<key>CFBundleIdentifier</key>"));
        assertTrue(out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }

    @Test
    public void aLatin1PlistKeepsItsAccentsAndItsDeclaration() throws Exception {
        File extension = extension();
        File infoPlist = new File(extension, "Info.plist");
        Charset latin1 = Charset.forName("ISO-8859-1");
        String source = plist("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n", "Café Wallet");
        writeBytes(infoPlist, source.getBytes(latin1));

        IPhoneBuilder.stampPlistFile(infoPlist, "5.4", "5.4", NO_SETTINGS);

        // Decoding with the wrong charset and writing the result back would turn the display name
        // into replacement characters -- corrupting a name in order to fix an identifier.
        String out = new String(readBytes(infoPlist), latin1);
        assertTrue(out, out.contains("Café Wallet"));
        assertTrue(out.contains("encoding=\"ISO-8859-1\""));
        assertTrue(out.contains("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>"));
    }

    @Test
    public void aQualifiedSettingNamesAPlistToStampToo() throws Exception {
        File extension = extension();
        writeText(new File(extension, "Info.plist"),
                plist("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "Base"));
        writeText(new File(extension, "Device-Info.plist"),
                plist("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "Device"));
        // Escaped, which is how a conditional key survives Properties -- and Xcode then honours it
        // over the base value for device builds, so the archive would ship the unstamped one.
        writeText(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = WalletUIExtension/Info.plist\n"
                + "INFOPLIST_FILE[sdk\\=iphoneos*] = WalletUIExtension/Device-Info.plist\n");

        Map<String, File> plists = IPhoneBuilder.appExtensionInfoPlists(extension);

        assertEquals(2, plists.size());
        assertTrue(plists.values().contains(new File(extension, "Info.plist")));
        assertTrue(plists.values().contains(new File(extension, "Device-Info.plist")));
    }

    @Test
    public void aQualifiedSettingBesideNoBaseValueKeepsTheDefault() throws Exception {
        File extension = extension();
        writeText(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE[sdk\\=iphoneos*] = WalletUIExtension/Device-Info.plist\n");

        Map<String, File> plists = IPhoneBuilder.appExtensionInfoPlists(extension);

        // The base value still decides for every build the condition does not match.
        assertEquals(2, plists.size());
        assertTrue(plists.values().contains(new File(extension, "Info.plist")));
        assertTrue(plists.values().contains(new File(extension, "Device-Info.plist")));
    }

    @Test
    public void anUnescapedConditionIsNotASettingAtAll() throws Exception {
        File extension = extension();
        // Properties splits on the = inside the brackets, so the key becomes INFOPLIST_FILE[sdk,
        // which Xcode does not recognise: the base value decides and there is nothing else to
        // stamp.
        writeText(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE[sdk=iphoneos*] = WalletUIExtension/Device-Info.plist\n");

        Map<String, File> plists = IPhoneBuilder.appExtensionInfoPlists(extension);

        assertEquals(1, plists.size());
        assertTrue(plists.values().contains(new File(extension, "Info.plist")));
    }

    private File extension() throws Exception {
        File dist = tmp.newFolder("dist");
        File extension = new File(dist, "WalletUIExtension");
        extension.mkdirs();
        return extension;
    }

    private static byte[] concat(byte[] head, byte[] tail) {
        byte[] out = new byte[head.length + tail.length];
        System.arraycopy(head, 0, out, 0, head.length);
        System.arraycopy(tail, 0, out, head.length, tail.length);
        return out;
    }

    private static void writeText(File file, String contents) throws Exception {
        writeBytes(file, contents.getBytes("UTF-8"));
    }

    private static void writeBytes(File file, byte[] contents) throws Exception {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(contents);
        } finally {
            out.close();
        }
    }

    private static byte[] readBytes(File file) throws Exception {
        byte[] data = new byte[(int) file.length()];
        java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream(file));
        try {
            in.readFully(data);
        } finally {
            in.close();
        }
        return data;
    }
}
