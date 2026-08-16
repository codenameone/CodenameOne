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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

    /**
     * The build target {@code cn1:buildIosOnDeviceDebug} selects. Its buildxml target submits
     * {@code codename1.ios.debug.provision} and is signed on the build server like any other
     * debug device build, so a bad profile costs a cloud build slot there too.
     */
    static final String IOS_ON_DEVICE_DEBUG = "ios-on-device-debug";

    /**
     * Whether this build is one the pre-flight has any business judging: a cloud iOS build
     * that the build server signs.
     *
     * <p>Excluded: {@code ios-source} (a local Xcode project, signed later or not at all), the
     * simulator, and the native-Mac targets -- those ride {@code platform=ios} with a different
     * signing identity, so the app's iOS profile settings do not describe them.
     */
    static boolean appliesTo(String platform, String buildTarget) {
        return platform != null && platform.contains("ios") && buildTarget != null
                && (buildTarget.startsWith("ios-device") || IOS_ON_DEVICE_DEBUG.equals(buildTarget));
    }

    /**
     * Whether this target signs with the release (distribution) profile. Everything else --
     * {@code ios-device} and {@code ios-on-device-debug} alike -- signs with the debug one.
     */
    static boolean isReleaseTarget(String buildTarget) {
        return buildTarget != null && buildTarget.contains("release");
    }

    /** The profile setting that applies to this build type. */
    static String provisioningProfileSettingKey(boolean release) {
        return release ? "codename1.ios.release.provision" : "codename1.ios.debug.provision";
    }

    /**
     * The checks that depend only on the profile file itself, safe to run before a CN1Lib's
     * properties have been merged in.
     *
     * <p>A library can supply {@code codename1.arg.ios.*.distributionMethod} through its
     * appended/required properties, which the mojo merges later -- so deciding a type mismatch
     * this early would refuse a build whose method the merge was about to make correct. That
     * decision belongs to {@link #check}, run against the merged settings. What a file is,
     * whether it exists, and whether it has expired cannot change in the merge, so those fail
     * here, before the app is packaged.
     *
     * @return the problems with the file; empty when there is nothing to say, including when
     * no profile is configured yet (the merge may still supply one).
     */
    static List<Problem> checkProfileFile(Properties settings, boolean release, Date now) {
        List<Problem> problems = new ArrayList<Problem>();
        String settingKey = provisioningProfileSettingKey(release);
        String path = settings.getProperty(settingKey);
        if (path == null || path.trim().isEmpty()) {
            return problems;
        }
        collectFileProblems(problems, settings, release, now, path, settingKey, false);
        return problems;
    }

    /**
     * @return the problems with the configured profile, in the order they should be
     * reported; empty when there is nothing to say. A fatal problem means the build
     * cannot succeed as configured. Run this against the settings the build will actually
     * be submitted with -- see {@link #checkProfileFile} for why.
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
        collectFileProblems(problems, settings, release, now, path, settingKey, true);
        return problems;
    }

    /**
     * Expands {@code ${name}} references the way the generated build file's
     * {@code <property file="codenameone_settings.properties">} does: a profile path is
     * routinely written as {@code ${user.home}/certs/dev.mobileprovision}, and Ant resolves
     * that before handing the value to the build task. A name is looked up in the settings
     * themselves first, then in the JVM's system properties, which is where {@code user.home}
     * and friends live.
     *
     * <p>What cannot be resolved is left standing rather than guessed at, and the caller
     * treats a value that still carries a placeholder as one it may not judge. Refusing a
     * build over a path this code merely failed to expand would break configurations that
     * work today -- the opposite of the point.
     *
     * @return the value with every resolvable reference expanded
     */
    static String resolvePlaceholders(String value, Properties settings) {
        if (value == null) {
            return null;
        }
        String current = value;
        // Bounded: a self-referential property would otherwise spin here.
        for (int pass = 0; pass < 10 && current.indexOf("${") >= 0; pass++) {
            StringBuilder out = new StringBuilder();
            boolean expandedAny = false;
            int i = 0;
            while (i < current.length()) {
                int start = current.indexOf("${", i);
                if (start < 0) {
                    out.append(current.substring(i));
                    break;
                }
                int end = current.indexOf('}', start);
                if (end < 0) {
                    out.append(current.substring(i));
                    break;
                }
                out.append(current, i, start);
                String name = current.substring(start + 2, end);
                String replacement = settings == null ? null : settings.getProperty(name);
                if (replacement == null) {
                    replacement = System.getProperty(name);
                }
                if (replacement == null) {
                    out.append(current, start, end + 1);
                } else {
                    out.append(replacement);
                    expandedAny = true;
                }
                i = end + 1;
            }
            current = out.toString();
            if (!expandedAny) {
                break;
            }
        }
        return current;
    }

    /**
     * @param checkMethodMismatch whether to also compare the profile's kind against the
     * distribution method these settings resolve to. Only true once the settings are the ones
     * the build will be submitted with, since a CN1Lib can still change the method.
     */
    private static void collectFileProblems(List<Problem> problems, Properties settings, boolean release,
            Date now, String path, String settingKey, boolean checkMethodMismatch) {
        String resolved = resolvePlaceholders(path.trim(), settings);
        if (resolved.indexOf("${") >= 0) {
            // Ant resolves this when it binds the value to the build task; this check cannot,
            // because it does not have that property context. A path it cannot resolve is a
            // path it cannot judge -- and calling it missing would refuse a build that works.
            return;
        }
        File file = new File(resolved);
        if (!file.exists() || !file.isFile()) {
            problems.add(new Problem("The provisioning profile for this build was not found at "
                    + file.getAbsolutePath() + " (" + settingKey + "). Point that setting at the "
                    + ".mobileprovision file, or re-generate it with the certificate wizard.", true));
            return;
        }

        byte[] raw;
        try {
            raw = readFile(file);
        } catch (IOException ex) {
            problems.add(new Problem("The provisioning profile at " + file.getAbsolutePath()
                    + " could not be read: " + ex.getMessage(), true));
            return;
        }

        if (raw.length == 0) {
            problems.add(new Problem("The provisioning profile at " + file.getAbsolutePath()
                    + " is empty (0 bytes). Re-download it from the Apple Developer portal, or "
                    + "re-generate it with the certificate wizard.", true));
            return;
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
            return;
        }

        String describe = profile.name == null ? file.getName() : "\"" + profile.name + "\"";
        if (profile.expirationDate == null) {
            // The key is there -- isProvisioningPlist insisted on that -- but its value is not
            // a date this can read. Skipping the expiry check on that basis let a corrupted
            // profile through on the strength of its type alone, which is the build slot this
            // check exists to save. Every Apple-issued profile carries a readable expiry.
            problems.add(new Problem("The provisioning profile " + describe + " at "
                    + file.getAbsolutePath() + " has no readable expiry date, so it is not a"
                    + " usable .mobileprovision file. Re-download it from the Apple Developer"
                    + " portal, or re-generate it with the certificate wizard.", true));
            return;
        }
        if (profile.expirationDate.before(now)) {
            problems.add(new Problem("The provisioning profile " + describe + " expired on "
                    + profile.expirationDate + ". Generate a new one in the Apple Developer portal "
                    + "and update " + settingKey + ".", true));
            return;
        }

        if (!checkMethodMismatch) {
            return;
        }
        String method = effectiveDistributionMethod(settings, release);
        Problem mismatch = checkMethod(profile, method, describe, release);
        if (mismatch != null) {
            problems.add(mismatch);
        }
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
        DocumentBuilder db = secureDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(plist));

        if (!isProvisioningPlist(doc)) {
            // An ordinary plist parses perfectly well. Info.plist has no device list, so
            // deriveType would have called it an App Store profile, and a release build --
            // whose default method is app-store -- would have sailed through this check and
            // uploaded a file that cannot provision or sign anything.
            return null;
        }

        Profile profile = new Profile();
        Element name = valueForKey(doc, "Name");
        if (name != null) {
            profile.name = name.getTextContent().trim();
        }
        // Left null when the value is not a <date>, or is one this cannot read; the caller
        // treats that as an unusable profile rather than as "no expiry to check".
        Element expires = valueForKey(doc, "ExpirationDate");
        if (expires != null && "date".equals(expires.getTagName())) {
            profile.expirationDate = parseDate(expires.getTextContent().trim());
        }
        profile.type = deriveType(doc);
        return profile;
    }

    /**
     * A parser that treats the profile as what it is: untrusted input that nothing has
     * verified. A {@code .mobileprovision} is signed, but neither this check nor the build
     * server validates that signature, and a profile can arrive from anyone -- a client
     * sending one for their app, a repository, a support ticket.
     *
     * <p>The DOCTYPE itself has to stay legal, because every real plist declares one. What is
     * refused is entity resolution: without this, an entity declared in a crafted profile's
     * internal subset is resolved by the JDK parser, so a profile could read a local file or
     * make a network request during someone's build -- and, used as the profile {@code Name},
     * have the result printed back in an error message. Turning off external general and
     * parameter entities, entity-reference expansion, XInclude and external DTD access closes
     * that; secure processing additionally caps entity expansion, so a profile cannot hang a
     * build with nested entities.
     *
     * <p>None of these is set "best effort": a parser that cannot be told to stop resolving
     * entities has no business reading an untrusted profile, so the exception propagates and
     * the profile is reported as unreadable rather than parsed unsafely.
     */
    private static DocumentBuilder secureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // the plist DOCTYPE also points at apple.com, so this keeps a local check from
        // depending on Apple's web server answering
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf.newDocumentBuilder();
    }

    /**
     * Whether this plist is a provisioning profile rather than some other plist the setting
     * happens to point at.
     *
     * <p>Keyed on what every Apple-issued profile carries and no ordinary plist does: the
     * profile's own UUID, when it expires, the entitlements it grants, and the certificates it
     * was issued to. Checked against real development, distribution and Xcode-team profiles.
     * Deliberately a small set -- the more that is demanded here, the more likely this refuses
     * a profile that is perfectly good.
     */
    private static boolean isProvisioningPlist(Document doc) {
        return valueForKey(doc, "UUID") != null
                && valueForKey(doc, "ExpirationDate") != null
                && valueForKey(doc, "Entitlements") != null
                && valueForKey(doc, "DeveloperCertificates") != null;
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
