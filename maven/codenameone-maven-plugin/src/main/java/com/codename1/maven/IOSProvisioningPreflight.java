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
package com.codename1.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Reads the {@code .mobileprovision} an iOS device build is about to sign with, and
 * refuses the build locally when signing cannot possibly succeed.
 *
 * <p>Every case here used to be found only by a cloud build server: the profile is
 * uploaded, an ~8GB toolchain runs for minutes, and the failure arrives as an Xcode
 * export error or -- when the file could not be decoded at all -- as a bare XML parser
 * stack trace with no mention of the profile. One real sequence: a build ran four
 * minutes and died with {@code exportArchive Provisioning profile "..." is not an
 * "iOS Ad Hoc" profile}, because the project asked for {@code ad-hoc} while the profile
 * was an App Store one. Both facts were in the file, on disk, before the build was sent.
 *
 * <p>The payload of a {@code .mobileprovision} is a plain-text XML plist inside a CMS
 * envelope, so it can be read here without {@code security}, Xcode, or a Mac.
 *
 * <p>The checks fail only where the outcome is certain -- a missing, empty, unreadable
 * or expired profile, and the unambiguous type mismatches that {@code xcodebuild
 * -exportArchive} rejects. Anything murkier (in-house/enterprise profiles, which Xcode
 * accepts for more than one method) warns instead, because a false refusal here blocks
 * a build that would have worked.
 */
final class IOSProvisioningPreflight {

    /** {@code xcodebuild -exportArchive} distribution methods, as the daemon names them. */
    static final String DEVELOPMENT = "development";
    static final String AD_HOC = "ad-hoc";
    static final String APP_STORE = "app-store";
    static final String ENTERPRISE = "enterprise";

    /** What a profile is, derived from the plist. */
    static class Profile {
        String name;
        String type;
        Date expirationDate;
    }

    /** A problem found before the build was sent: {@code message} is written for the user. */
    static class Problem {
        final String message;
        final boolean fatal;

        Problem(String message, boolean fatal) {
            this.message = message;
            this.fatal = fatal;
        }
    }

    private IOSProvisioningPreflight() {
    }

    /**
     * The distribution method this build will actually export with, resolved exactly as
     * {@code IPhoneBuilder} resolves it on the build server: a per-build-type hint beats
     * the shared hint, which beats the default (development for debug, app-store for
     * release). Reading it any other way would refuse builds the server would have
     * accepted.
     */
    static String effectiveDistributionMethod(Properties settings, boolean release) {
        String method = release ? APP_STORE : DEVELOPMENT;
        method = arg(settings, "ios.distributionMethod", method);
        method = arg(settings, release ? "ios.release.distributionMethod" : "ios.debug.distributionMethod", method);
        return method;
    }

    private static String arg(Properties settings, String hint, String defaultValue) {
        String value = settings.getProperty("codename1.arg." + hint);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    /** The profile setting that applies to this build type. */
    static String provisioningProfileSettingKey(boolean release) {
        return release ? "codename1.ios.release.provision" : "codename1.ios.debug.provision";
    }

    /**
     * @return the problems with the configured profile, in the order they should be
     * reported; empty when there is nothing to say. A fatal problem means the build
     * cannot succeed as configured.
     */
    static List<Problem> check(Properties settings, boolean release, Date now) {
        List<Problem> problems = new ArrayList<Problem>();
        String settingKey = provisioningProfileSettingKey(release);
        String path = settings.getProperty(settingKey);
        if (path == null || path.trim().isEmpty()) {
            // Not fatal: a profile can still reach the server another way, and refusing here
            // would invent a failure the build might not have.
            problems.add(new Problem("No provisioning profile is configured for this build ("
                    + settingKey + " is not set). An iOS device build has to be signed, so the "
                    + "build server will reject it unless the profile is supplied another way.", false));
            return problems;
        }

        File file = new File(path.trim());
        if (!file.exists() || !file.isFile()) {
            problems.add(new Problem("The provisioning profile for this build was not found at "
                    + file.getAbsolutePath() + " (" + settingKey + "). Point that setting at the "
                    + ".mobileprovision file, or re-generate it with the certificate wizard.", true));
            return problems;
        }

        byte[] raw;
        try {
            raw = readFile(file);
        } catch (IOException ex) {
            problems.add(new Problem("The provisioning profile at " + file.getAbsolutePath()
                    + " could not be read: " + ex.getMessage(), true));
            return problems;
        }

        if (raw.length == 0) {
            problems.add(new Problem("The provisioning profile at " + file.getAbsolutePath()
                    + " is empty (0 bytes). Re-download it from the Apple Developer portal, or "
                    + "re-generate it with the certificate wizard.", true));
            return problems;
        }

        Profile profile;
        try {
            profile = parse(raw);
        } catch (Exception ex) {
            profile = null;
        }
        if (profile == null) {
            problems.add(new Problem("The file at " + file.getAbsolutePath() + " (" + raw.length
                    + " bytes) is not a valid .mobileprovision file -- it carries no readable "
                    + "provisioning plist. Re-download it from the Apple Developer portal, or "
                    + "re-generate it with the certificate wizard.", true));
            return problems;
        }

        String describe = profile.name == null ? file.getName() : "\"" + profile.name + "\"";
        if (profile.expirationDate != null && profile.expirationDate.before(now)) {
            problems.add(new Problem("The provisioning profile " + describe + " expired on "
                    + profile.expirationDate + ". Generate a new one in the Apple Developer portal "
                    + "and update " + settingKey + ".", true));
            return problems;
        }

        String method = effectiveDistributionMethod(settings, release);
        Problem mismatch = checkMethod(profile, method, describe, release);
        if (mismatch != null) {
            problems.add(mismatch);
        }
        return problems;
    }

    /**
     * The type mismatch {@code xcodebuild -exportArchive} would reject minutes into the
     * cloud build. Enterprise profiles only warn: Xcode accepts an in-house profile for
     * more than one method, so refusing one risks blocking a build that works.
     */
    private static Problem checkMethod(Profile profile, String method, String describe, boolean release) {
        if (profile.type == null || method == null || method.equals(profile.type)) {
            return null;
        }
        String hint = release ? "ios.release.distributionMethod" : "ios.debug.distributionMethod";
        String message = "The provisioning profile " + describe + " is " + article(profile.type)
                + " profile, but this build is configured to export with method \"" + method
                + "\" (" + hint + "). Xcode will refuse to export with a mismatched profile."
                + " Either set " + hint + "=" + profile.type + ", or use " + article(method)
                + " provisioning profile.";
        if (ENTERPRISE.equals(profile.type) || ENTERPRISE.equals(method)) {
            return new Problem(message, false);
        }
        return new Problem(message, true);
    }

    private static String article(String type) {
        if (AD_HOC.equals(type) || APP_STORE.equals(type) || ENTERPRISE.equals(type)) {
            return "an " + describeType(type);
        }
        return "a " + describeType(type);
    }

    private static String describeType(String type) {
        if (AD_HOC.equals(type)) {
            return "Ad Hoc";
        }
        if (APP_STORE.equals(type)) {
            return "App Store distribution";
        }
        if (ENTERPRISE.equals(type)) {
            return "in-house (enterprise)";
        }
        return "Development";
    }

    /**
     * Reads the plist payload of a {@code .mobileprovision}.
     *
     * @return what the profile is, or null when the bytes carry no provisioning plist.
     */
    static Profile parse(byte[] raw) throws Exception {
        byte[] plist = extractEmbeddedPlist(raw);
        if (plist == null) {
            return null;
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        disableExternalDtdLoading(dbf);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(plist));

        Profile profile = new Profile();
        Element name = valueForKey(doc, "Name");
        if (name != null) {
            profile.name = name.getTextContent().trim();
        }
        Element expires = valueForKey(doc, "ExpirationDate");
        if (expires != null && "date".equals(expires.getTagName())) {
            profile.expirationDate = parseDate(expires.getTextContent().trim());
        }
        profile.type = deriveType(doc);
        return profile;
    }

    /**
     * The plist DOCTYPE points at apple.com. Resolving it would make a local check depend on
     * Apple's web server answering, so it is turned off where the parser supports it.
     *
     * @return whether the parser honoured the request
     */
    private static boolean disableExternalDtdLoading(DocumentBuilderFactory dbf) {
        try {
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * The four profile kinds, told apart the way Apple's own tooling does: an in-house
     * profile provisions all devices, a profile that lists devices is Development when it
     * allows the debugger to attach ({@code get-task-allow}) and Ad Hoc when it does not,
     * and a profile that lists no devices at all is an App Store one.
     */
    private static String deriveType(Document doc) {
        Element provisionsAllDevices = valueForKey(doc, "ProvisionsAllDevices");
        if (provisionsAllDevices != null && "true".equals(provisionsAllDevices.getTagName())) {
            return ENTERPRISE;
        }
        Element devices = valueForKey(doc, "ProvisionedDevices");
        if (devices != null && "array".equals(devices.getTagName())) {
            Element getTaskAllow = valueForKey(doc, "get-task-allow");
            if (getTaskAllow != null && "true".equals(getTaskAllow.getTagName())) {
                return DEVELOPMENT;
            }
            return AD_HOC;
        }
        return APP_STORE;
    }

    /** The element that follows the named {@code <key>} -- a plist's value for that key. */
    private static Element valueForKey(Document doc, String key) {
        NodeList keys = doc.getElementsByTagName("key");
        for (int i = 0; i < keys.getLength(); i++) {
            Element k = (Element) keys.item(i);
            if (!key.equals(k.getTextContent().trim())) {
                continue;
            }
            Node n = k.getNextSibling();
            while (n != null && !(n instanceof Element)) {
                n = n.getNextSibling();
            }
            if (n != null) {
                return (Element) n;
            }
        }
        return null;
    }

    private static Date parseDate(String value) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(value);
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * Lifts the {@code <?xml ... </plist>} payload out of the CMS envelope. The payload is
     * not encrypted, which is what lets this run without {@code security} or a Mac.
     */
    static byte[] extractEmbeddedPlist(byte[] raw) {
        if (raw == null) {
            return null;
        }
        int start = indexOf(raw, "<?xml".getBytes(StandardCharsets.UTF_8), 0);
        if (start < 0) {
            return null;
        }
        byte[] end = "</plist>".getBytes(StandardCharsets.UTF_8);
        int endPos = indexOf(raw, end, start);
        if (endPos < 0) {
            return null;
        }
        int len = endPos + end.length - start;
        byte[] plist = new byte[len];
        System.arraycopy(raw, start, plist, 0, len);
        return plist;
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] readFile(File f) throws IOException {
        try (InputStream is = new FileInputStream(f)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int amount;
            while ((amount = is.read(buffer)) > -1) {
                baos.write(buffer, 0, amount);
            }
            return baos.toByteArray();
        }
    }
}
