/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Names the cause of a failed build from its log, with the fix.
 *
 * <pre>
 * java tools/BuildLog.java build.log        # a downloaded cloud build log
 * ./mvnw package ... 2&gt;&amp;1 | java tools/BuildLog.java -   # or from stdin
 * </pre>
 *
 * Prints one block per cause found, most specific first: the ID, the first log line
 * that matched (with its line number) and what to change. Exits 0 when it named a
 * cause, 1 when nothing it knows matched (read the log's first "error:" line), and 2
 * on a bad command line.
 *
 * The patterns are the toolchains' own wording (Xcode, codesign, openssl, keytool and
 * apksigner, Gradle, CocoaPods, SwiftPM, clang, javac) and the build service's, so
 * the build server needs to report nothing extra.
 */
public class BuildLog {

    static final class Cause {
        final String id;
        final String what;
        final String fix;
        final Pattern pattern;

        Cause(String id, String what, String fix, String... regexes) {
            this.id = id;
            this.what = what;
            this.fix = fix;
            this.pattern = Pattern.compile(String.join("|", regexes), Pattern.CASE_INSENSITIVE);
        }
    }

    /** Most specific first: an entitlement problem also mentions the profile. */
    static final Cause[] CAUSES = {
        new Cause("CN1-BUILD-QUOTA",
                "The build service refused the build: the account is out of build credits or the upload is "
                        + "over the plan's size limit.",
                "Check the account's remaining builds at https://cloud.codenameone.com, shrink the upload "
                        + "(large resources, unused cn1libs), or upgrade the plan.",
                "Quota size limit reached", "userQuotaExceed", "quotaExceed", "out of build credits",
                "build credits? (are )?(exhausted|used up)", "exceeded (your|the) (monthly )?build quota"),
        new Cause("CN1-BUILD-TIMEOUT",
                "The build ran out of time on the server.",
                "Retry once; if it times out again, look for a step that hangs (a test waiting on the network, "
                        + "a pod install) or reduce what the build does.",
                "Process timed out", "build (has )?timed out", "\\(timed out\\)", "exceeded the maximum build time",
                "Timed out waiting for"),
        new Cause("CN1-SIGN-ENTITLEMENT",
                "The app uses a capability (push, iCloud, App Groups, HomeKit...) its provisioning profile "
                        + "does not grant.",
                "Enable the capability on the App ID in the Apple Developer portal, regenerate the provisioning "
                        + "profile, and set the new one (ios.provision / ios.debug.provision); or drop the "
                        + "entitlement from ios.entitlementsInject.",
                "provisioning profile does(n't| not) (grant|include|support|carry)[^\\n]*(entitlement|capabilit)",
                "doesn't (support|include) the [^\\n]*(capability|entitlement)",
                "Entitlements? [^\\n]*(not (present|included|allowed)|missing) in (the )?(provisioning )?profile",
                "Missing Push Notification Entitlement",
                "entitlement[^\\n]*which the provisioning profile supplied for it does not carry"),
        new Cause("CN1-SIGN-PROFILE",
                "The provisioning profile does not fit the build: wrong bundle ID, wrong certificate, expired, "
                        + "or the wrong kind (development vs. distribution).",
                "Regenerate the profile for this bundle ID (codename1.packageName) with the certificate you sign "
                        + "with, of the kind the build needs (App Store for release, development for debug), and "
                        + "set it in ios.provision or ios.debug.provision. `mvn cn1:ios-certificate-wizard` "
                        + "does all of it.",
                "Provisioning profile [^\\n]*(doesn't|does not) (include (the )?signing certificate|match)",
                "Provisioning profile [^\\n]*(has )?expired", "No (matching )?provisioning profiles? (found|for)",
                "requires a provisioning profile", "doesn't match the bundle identifier",
                "bundle identifier [^\\n]*(does not|doesn't) match", "No profile for team",
                "profile (is|was) not (valid|found)", "provisioning profile [^\\n]*(invalid|corrupt|could not be read)"),
        new Cause("CN1-SIGN-CERT",
                "The iOS signing certificate is missing, expired, revoked, does not match its private key, or "
                        + "its P12 password is wrong.",
                "Re-export the certificate WITH its private key as a .p12, check the password you set in "
                        + "ios.certificatePassword, and make sure it has not expired or been revoked in the "
                        + "Apple Developer portal. `mvn cn1:ios-certificate-wizard` can make a new one.",
                "Mac verify error", "PKCS12[^\\n]*(password|invalid|error)", "No signing certificate",
                "certificate (has )?(expired|been revoked)", "CSSMERR_TP_CERT_(EXPIRED|REVOKED|NOT_VALID_YET)",
                "doesn't match any valid certificate/private key pair", "errSecInternalComponent",
                "no identity found", "The identity used to sign the executable is no longer valid",
                "invalid password\\?"),
        new Cause("CN1-ANDROID-KEYSTORE",
                "Android signing failed: the keystore, its password, the alias or the key password is wrong.",
                "Check android.keystore, android.keystorePassword, android.keystoreAlias and the key password "
                        + "with `keytool -list -v -keystore <file>`; the alias must be one it lists.",
                "Keystore was tampered with, or password was incorrect", "Cannot recover key",
                "No key with alias", "Failed to read key [^\\n]* from store", "Given final block not properly padded",
                "keystore password was incorrect", "Invalid keystore format", "KeyStoreException"),
        new Cause("CN1-BUILD-DEPENDENCY",
                "A dependency did not resolve: a Gradle/Maven artifact, a CocoaPod or a Swift package.",
                "Check the coordinates and version (android.gradleDep, ios.pods, the pom), that the repository "
                        + "serving it is declared, and for pods that ios.pods.platform is high enough.",
                "Could not resolve( all)? (dependencies|files|artifacts|[a-z0-9_.-]+:[a-z0-9_.-]+)",
                "Could not find [a-z0-9_.-]+:[a-z0-9_.-]+:", "Failed to resolve:",
                "Unable to find a specification for", "CocoaPods could not find compatible versions",
                "required a higher minimum deployment target", "Could not resolve package dependencies",
                "Failed to collect dependencies", "Could not transfer artifact", "Duplicate class [^\\n]* found in modules"),
        new Cause("CN1-BUILD-NATIVE",
                "Native code failed to compile or link: a native interface, a cn1lib's natives, or code "
                        + "injected by a build hint.",
                "Open the file the first error names; check the native method names and signatures against "
                        + "the Java interface (`mvn cn1:generate-native-interfaces` regenerates the stubs), and "
                        + "that every framework or library the code uses is declared (ios.add_libs, "
                        + "android.gradleDep).",
                "\\.(m|mm|c|cpp|h|swift):\\d+(:\\d+)?: (fatal )?error:", "Undefined symbols? for architecture",
                "ld: (library|framework) not found", "clang: error:",
                "/(android|native)/[^\\n]*\\.(java|kt):\\d+: error:", "Execution failed for task ':app:compile"),
    };

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: java BuildLog.java <build.log | ->");
            System.exit(2);
        }
        String text;
        if ("-".equals(args[0])) {
            text = readAll(System.in);
        } else if (Files.isRegularFile(Paths.get(args[0]))) {
            text = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        } else {
            System.err.println("No such file: " + args[0]);
            System.exit(2);
            return;
        }
        List<String> found = scan(text);
        if (found.isEmpty()) {
            System.out.println("UNKNOWN: no known cause in the log; read its first \"error:\" line.");
            System.exit(1);
        }
        for (String block : found) {
            System.out.println(block);
        }
    }

    /** One block per cause that matched, in the order of CAUSES. */
    static List<String> scan(String text) {
        String[] lines = text.split("\r?\n");
        List<String> out = new ArrayList<>();
        for (Cause c : CAUSES) {
            for (int i = 0; i < lines.length; i++) {
                if (c.pattern.matcher(lines[i]).find()) {
                    String line = lines[i].trim();
                    if (line.length() > 200) {
                        line = line.substring(0, 200) + "...";
                    }
                    out.add("[" + c.id + "] " + c.what + "\n  line " + (i + 1) + ": " + line + "\n  fix: " + c.fix + "\n");
                    break;
                }
            }
        }
        return out;
    }

    private static String readAll(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
}
