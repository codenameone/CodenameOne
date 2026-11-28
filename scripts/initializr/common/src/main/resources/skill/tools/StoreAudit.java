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
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Pre-submission audit of a Codename One project against the App Store and
/// Google Play review rules in `references/store-review.md`.
///
/// ```
/// java tools/StoreAudit.java                       # audit the project in .
/// java tools/StoreAudit.java path/to/project
/// java tools/StoreAudit.java --checklist submission-checklist.md
/// java tools/StoreAudit.java --network --url https://example.com/privacy \
///                            --url https://example.com/support
/// ```
///
/// It reads the build hints in `codenameone_settings.properties` and scans the
/// app's sources (Java, Kotlin, l10n `.properties`), then prints every finding
/// as BLOCKER (the store rejects it), HIGH (often rejected) or NOTE, with the
/// file and line and the fix. What code cannot prove -- console answers, the
/// demo account, the listing -- is printed as a MANUAL list that depends on
/// what the app uses (accounts, purchases, AI, user content, ...).
/// `--checklist <file>` writes both as a Markdown checklist to walk with the
/// developer before uploading.
///
/// `--network` also fetches every `--url` (privacy policy, support, marketing:
/// a broken one is a rejection) and resolves each backend host the sources
/// name, reporting hosts with no IPv6 address as a NOTE. Without it the tool
/// never touches the network.
///
/// A scan is a heuristic: re-check each BLOCKER against the code before
/// reporting it, as the reference says. `--strict` also fails on HIGH.
///
/// Exit status: 0 no BLOCKER (no HIGH with `--strict`), 1 findings that fail,
/// 2 usage error or no project found.
public class StoreAudit {

    enum Sev { BLOCKER, HIGH, NOTE }

    record Finding(Sev sev, String id, String where, String msg, String fix) { }

    record Hit(Path file, int line, String text) { }

    static final List<Finding> findings = new ArrayList<>();
    static final Set<String> features = new LinkedHashSet<>();
    static final List<Path> sources = new ArrayList<>();
    static final Map<Path, List<String>> lines = new LinkedHashMap<>();
    static Properties hints = new Properties();
    static Path root;

    public static void main(String[] args) throws Exception {
        Path project = Paths.get(".");
        String checklist = null;
        boolean network = false;
        boolean strict = false;
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--checklist" -> checklist = need(args, ++i);
                case "--network" -> network = true;
                case "--strict" -> strict = true;
                case "--url" -> urls.add(need(args, ++i));
                case "-h", "--help" -> { usage(); System.exit(0); }
                default -> {
                    if (args[i].startsWith("-")) { usage(); System.exit(2); }
                    project = Paths.get(args[i]);
                }
            }
        }
        root = project.toAbsolutePath().normalize();
        Path settings = firstExisting(root.resolve("common/codenameone_settings.properties"),
                root.resolve("codenameone_settings.properties"));
        Path src = firstExisting(root.resolve("common/src/main"), root.resolve("src/main"));
        if (settings == null || src == null) {
            System.err.println("No Codename One project at " + root
                    + " (looked for codenameone_settings.properties and src/main)");
            System.exit(2);
        }
        try (var in = Files.newBufferedReader(settings, StandardCharsets.UTF_8)) {
            hints.load(in);
        }
        try (Stream<Path> walk = Files.walk(src)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> { String n = p.toString(); return n.endsWith(".java") || n.endsWith(".kt")
                        || n.endsWith(".properties"); })
                .forEach(sources::add);
        }
        for (Path p : sources) {
            try {
                lines.put(p, Files.readAllLines(p, StandardCharsets.UTF_8));
            } catch (IOException e) {
                lines.put(p, Files.readAllLines(p, StandardCharsets.ISO_8859_1));
            }
        }

        checkPurposeStrings();
        checkPayments();
        checkAccounts();
        checkAi();
        checkUserContent();
        checkCompleteness();
        checkNetworking();
        checkSecurity();
        checkAndroid();
        checkIos();
        checkMetadataWords();
        if (network) {
            checkNetwork(urls);
        } else if (!urls.isEmpty()) {
            System.err.println("--url given without --network; the URLs were not fetched.");
        }

        List<String> manual = manualItems();
        report(manual);
        if (checklist != null) {
            writeChecklist(Paths.get(checklist), manual);
            System.out.println("Checklist written to " + checklist);
        }
        boolean fail = count(Sev.BLOCKER) > 0 || (strict && count(Sev.HIGH) > 0);
        System.exit(fail ? 1 : 0);
    }

    // ---------------------------------------------------------------- checks

    /// API used -> iOS purpose string hint it needs.
    static final String[][] PURPOSES = {
        {"camera", "\\bCapture\\.capturePhoto|\\bcapturePhoto\\(|CameraKit|\\bCodeScanner\\b|\\bBarcodeScanner\\b|\\bscanBarCode\\(|\\bscanQRCode\\(", "ios.NSCameraUsageDescription"},
        {"microphone", "\\bcaptureAudio\\(|\\bcaptureVideo\\(|\\bcreateMediaRecorder\\(|\\bMediaRecorder\\b|\\bSpeechRecognizer\\b", "ios.NSMicrophoneUsageDescription"},
        {"location", "\\bLocationManager\\b", "ios.NSLocationWhenInUseUsageDescription"},
        {"contacts", "\\bContactsManager\\b|\\bgetAllContacts\\(|\\bgetContactById\\(", "ios.NSContactsUsageDescription"},
        {"biometrics", "\\bBiometrics\\b|\\bFingerprintScanner\\b", "ios.NSFaceIDUsageDescription"},
        {"bluetooth", "\\bBluetooth[A-Z]?\\w*\\b|\\bBluetoothLE\\b", "ios.NSBluetoothAlwaysUsageDescription"},
        {"calendar", "\\bDeviceCalendar\\b|\\bCalendarManager\\b", "ios.NSCalendarsFullAccessUsageDescription"},
    };

    static final Pattern VAGUE = Pattern.compile(
            "(?i)^\\s*$|^(camera|photos?|location|microphone|contacts|bluetooth|calendar|face ?id)( access)?\\.?$"
            + "|\\b(this|the) app (needs|requires|wants|uses|would like)\\b"
            + "|\\b(needs|requires|need|require) (access|permission)\\b"
            + "|^\\s*(required|needed|for (app )?functionality|access)\\.?\\s*$");

    static void checkPurposeStrings() {
        for (String[] p : PURPOSES) {
            List<Hit> uses = grep(p[1]);
            String hint = hints.getProperty("codename1.arg." + p[2]);
            if (!uses.isEmpty()) {
                features.add(p[0]);
                if (p[0].equals("location")) {
                    String always = hints.getProperty("codename1.arg.ios.NSLocationAlwaysUsageDescription",
                            hints.getProperty("codename1.arg.ios.NSLocationAlwaysAndWhenInUseUsageDescription"));
                    if (always != null) {
                        add(Sev.HIGH, "LOCATION-ALWAYS", "codenameone_settings.properties",
                                "An \"Always\" location purpose string is set. Reviewers ask for the background feature that needs it.",
                                "Use \"when in use\" unless the feature truly runs in the background; then say why in the string.");
                    }
                }
                if (hint == null) {
                    add(Sev.BLOCKER, "PURPOSE-MISSING", at(uses.get(0)),
                            "The app uses the " + p[0] + " but sets no " + p[2] + " build hint.",
                            "Add codename1.arg." + p[2] + "=<what this app does with it, e.g. \"Scan a receipt to add an expense\">.");
                }
            }
            if (hint != null) {
                if (VAGUE.matcher(hint).find()) {
                    add(Sev.HIGH, "PURPOSE-VAGUE", "codenameone_settings.properties",
                            p[2] + " is generic: \"" + hint + "\" (Apple 5.1.1).",
                            "Say what the app does with it, in this app's terms.");
                }
                if (uses.isEmpty()) {
                    add(Sev.NOTE, "PURPOSE-UNUSED", "codenameone_settings.properties",
                            p[2] + " is set but no " + p[0] + " API was found in the sources.",
                            "Remove it unless a cn1lib uses the " + p[0] + "; reviewers ask about permissions with no feature.");
                }
            }
        }
        // Any other usage description hint, checked for vagueness only.
        for (String k : hints.stringPropertyNames()) {
            if (k.matches("codename1\\.arg\\.ios\\.NS\\w+UsageDescription")
                    && Stream.of(PURPOSES).noneMatch(p -> k.endsWith(p[2]))
                    && VAGUE.matcher(hints.getProperty(k)).find()) {
                add(Sev.HIGH, "PURPOSE-VAGUE", "codenameone_settings.properties",
                        k.substring("codename1.arg.".length()) + " is generic: \"" + hints.getProperty(k) + "\".",
                        "Say what the app does with it, in this app's terms.");
            }
        }
        // A pre-permission screen whose button reads "Allow" (5.1.1(iv)).
        for (Hit h : grep("\"(Allow|Allow Access|Allow access)\"")) {
            add(Sev.HIGH, "PREPROMPT-ALLOW", at(h),
                    "A button labelled \"Allow\" -- on a screen shown before a system permission prompt this is rejected under 5.1.1(iv).",
                    "Label the explanation screen's button \"Continue\" or \"Next\"; only the system prompt says Allow.");
        }
    }

    static void checkPayments() {
        List<Hit> purchase = grep("\\bPurchase\\.getInAppPurchase\\(|\\bpurchase\\(|\\bsubscribe\\(|\\bPurchaseCallback\\b");
        boolean iap = !grep("\\bPurchase\\.getInAppPurchase\\(|\\bPurchaseCallback\\b").isEmpty();
        if (!iap) {
            purchase = List.of();
        }
        if (iap) {
            features.add("iap");
            if (grep("\\.restore\\(\\)").isEmpty()) {
                add(Sev.BLOCKER, "IAP-NO-RESTORE", at(purchase.get(0)),
                        "In-app purchases with no call to restore() (Apple 3.1.1).",
                        "Add a visible \"Restore purchases\" button that calls Purchase.getInAppPurchase().restore().");
            }
            boolean subs = !grep("\\bsubscribe\\(|\\bisSubscriptionSupported\\(|\\bReceipt\\b").isEmpty();
            if (subs) {
                features.add("subscriptions");
                if (grep("\\bmanageSubscriptions\\(").isEmpty()) {
                    add(Sev.HIGH, "SUB-NO-MANAGE", at(purchase.get(0)),
                            "Subscriptions with no manageSubscriptions() entry point. Cancelling must be as easy as subscribing (Apple 3.1.2, FTC, EU).",
                            "In settings, when isManageSubscriptionsSupported(), offer manageSubscriptions(sku).");
                }
                if (grep("(?i)auto(matically)?[- ]renew").isEmpty()) {
                    add(Sev.HIGH, "SUB-NO-DISCLOSURE", at(purchase.get(0)),
                            "No text says the subscription renews automatically (Apple 3.1.2).",
                            "The paywall shows price, period, what is included, that it auto-renews and how to cancel, with privacy and terms links.");
                }
            }
        }
        List<Hit> webPay = grep("(?i)(checkout\\.stripe\\.com|paypal\\.com/(checkout|donate)|buy\\.stripe\\.com|patreon\\.com|ko-fi\\.com|buymeacoffee)");
        if (!webPay.isEmpty()) {
            add(iap ? Sev.HIGH : Sev.NOTE, "EXTERNAL-PAYMENT", at(webPay.get(0)),
                    "A link to an external checkout or donation page. Digital goods and in-app donations to the developer go through store billing (Apple 3.1.1, Google Payments).",
                    "Keep it only for physical goods/services, or in the storefronts that allow link-outs; otherwise use Purchase.");
        }
        List<Hit> loot = grep("(?i)\\b(loot ?box|gacha|mystery box|random reward)");
        if (!loot.isEmpty()) {
            features.add("lootbox");
            add(Sev.HIGH, "LOOTBOX-ODDS", at(loot.get(0)),
                    "Randomised paid rewards: both stores require the odds to be shown before purchase.",
                    "Show the odds of each item on the purchase screen.");
        }
    }

    static void checkAccounts() {
        List<Hit> social = grep("\\b(GoogleConnect|FacebookConnect|MicrosoftConnect|Auth0Connect|FirebaseAuth)\\b");
        List<Hit> apple = grep("\\bAppleSignIn\\b");
        List<Hit> login = grep("(?i)\\b(Login\\b|signIn\\(|signUp\\(|register\\(|createAccount|\"(sign in|log in|sign up|create account)\")");
        if (!social.isEmpty() || !apple.isEmpty() || !login.isEmpty()) {
            features.add("accounts");
        }
        if (!social.isEmpty() && apple.isEmpty()) {
            add(Sev.BLOCKER, "SIWA-MISSING", at(social.get(0)),
                    "Third-party sign-in without Sign in with Apple (Apple 4.8).",
                    "Offer com.codename1.social.AppleSignIn next to the other providers, with Apple's button style.");
        }
        if (features.contains("accounts")
                && grep("(?i)delete (my |your |the )?account|deleteAccount|removeAccount|account deletion").isEmpty()) {
            add(Sev.BLOCKER, "NO-ACCOUNT-DELETION", at(!login.isEmpty() ? login.get(0) : social.isEmpty() ? apple.get(0) : social.get(0)),
                    "Accounts with no in-app account deletion (Apple 5.1.1(v), Google account deletion policy).",
                    "Add \"Delete account\" in settings that deletes the account and its data on the server, and publish a web deletion URL for Play.");
        }
    }

    static void checkAi() {
        List<Hit> ai = grep("(?i)\\bcom\\.codename1\\.ai\\b|\\bLlmClient\\b|\\bChatView\\b|api\\.openai\\.com|api\\.anthropic\\.com|generativelanguage\\.googleapis\\.com|api\\.mistral\\.ai|api\\.groq\\.com|openrouter\\.ai");
        if (ai.isEmpty()) {
            return;
        }
        features.add("ai");
        if (grep("(?i)(consent|opt[- ]?in|agree|I understand).{0,80}(AI|OpenAI|Anthropic|Gemini|model|assistant)|(AI|OpenAI|Anthropic|Gemini).{0,80}(consent|opt[- ]?in|agree)").isEmpty()) {
            add(Sev.BLOCKER, "AI-NO-CONSENT", at(ai.get(0)),
                    "Data is sent to an AI service with no consent text found (Apple 5.1.2(i), November 2025).",
                    "Before the first request, show which provider receives what and get an explicit opt-in; name the provider in the privacy policy.");
        }
        if (grep("(?i)\\b(report|flag)\\b.{0,40}\\b(response|answer|message|output|image)").isEmpty()) {
            add(Sev.HIGH, "AI-NO-REPORT", at(ai.get(0)),
                    "Generative AI output with no way to report it (Apple 1.2, Google AI-Generated Content).",
                    "Let the user report a response; filter prompts and output for objectionable content. Image/video generators also need intimate-imagery safeguards on Play.");
        }
        List<Hit> keys = grep("\"(sk-[A-Za-z0-9_-]{16,}|sk-ant-[A-Za-z0-9_-]{16,}|AIza[0-9A-Za-z_-]{30,})\"");
        for (Hit h : keys) {
            add(Sev.BLOCKER, "SECRET-IN-CLIENT", at(h),
                    "A paid-service API key is compiled into the app.",
                    "Call the service from your backend; never ship the key in the client.");
        }
    }

    static void checkUserContent() {
        List<Hit> ugc = grep("(?i)\\b(postComment|uploadPhoto|sendMessage|sendText|sendMedia|createPost|publishPost|ChatView|comments?Endpoint)\\b|/(posts|comments|messages|reviews)\\b");
        if (ugc.isEmpty()) {
            return;
        }
        features.add("ugc");
        boolean report = !grep("(?i)\\breport (user|post|content|message|abuse)|\\breportUser|\\breportContent|\"Report\"").isEmpty();
        boolean block = !grep("(?i)\\bblock (user|this user)|\\bblockUser|\"Block\"").isEmpty();
        if (!report || !block) {
            add(Sev.HIGH, "UGC-MODERATION", at(ugc.get(0)),
                    "Content users post for others appears to lack " + (!report ? "a report action" : "")
                    + (!report && !block ? " and " : "") + (!block ? "a block action" : "") + " (Apple 1.2).",
                    "Add report and block, filter objectionable content, act on reports within 24 hours, and publish contact information. Answer App Store Connect's social-media question if it is asked.");
        }
    }

    static void checkCompleteness() {
        for (Hit h : grep("(?i)lorem ipsum|\"coming soon|\"under construction|\"todo\\b|\"test button|\"placeholder")) {
            add(Sev.BLOCKER, "PLACEHOLDER", at(h),
                    "Placeholder text in the UI (Apple 2.1, the most common rejection).",
                    "Replace it with real content or remove the screen.");
        }
        for (Hit h : grep("(?i)https?://(localhost|127\\.0\\.0\\.1|10\\.0\\.2\\.2|0\\.0\\.0\\.0)\\b")) {
            if (h.text.matches("(?i).*(redirect|callback).*")) {
                continue; // an OAuth redirect the app intercepts, never fetched
            }
            add(Sev.BLOCKER, "LOCAL-BACKEND", at(h),
                    "The app talks to a local address the reviewer cannot reach. Every full-stack template ships this way, for development.",
                    "Deploy the backend and point the release build at its public https URL (the template's AppConfig.DEFAULT_SERVER).");
        }
        for (Hit h : grep("(?i)https?://[^\"\\s]*\\b(staging|stage|dev|test|sandbox|ngrok|trycloudflare)\\b[^\"\\s]*")) {
            if (h.text.contains("sandbox.itunes") || h.text.contains("sandbox.paypal")) {
                continue;
            }
            add(Sev.HIGH, "STAGING-BACKEND", at(h),
                    "A staging/tunnel URL is compiled in; reviewers must hit production (Apple 2.1).",
                    "Use the production URL in the release build, or select it by build type, and confirm which one ships.");
        }
        if (hints.getProperty("codename1.version") == null) {
            add(Sev.NOTE, "NO-VERSION", "codenameone_settings.properties",
                    "codename1.version is not set.",
                    "Set it and raise it (or ios.bundleVersion / android.versionCode) with every upload.");
        }
        List<Hit> hidden = grep("(?i)\\b(remoteConfig|featureFlag|isFeatureEnabled|killSwitch|enableAfterReview|hiddenFeature)\\b");
        if (!hidden.isEmpty()) {
            add(Sev.NOTE, "REMOTE-FLAGS", at(hidden.get(0)),
                    "Remotely switched features: anything turned on after review that changes what the app does is rejected (Apple 2.5.19).",
                    "Make sure the reviewed build shows every feature that will ship; mention flags in the review notes.");
        }
    }

    static void checkNetworking() {
        for (Hit h : grep("https?://(\\d{1,3}\\.){3}\\d{1,3}")) {
            if (h.text.matches(".*https?://(127\\.0\\.0\\.1|10\\.0\\.2\\.2|0\\.0\\.0\\.0).*")) {
                continue;
            }
            add(Sev.HIGH, "IPV4-LITERAL", at(h),
                    "An IPv4 address literal. Apple reviews on an IPv6-only network, where it fails (Apple 2.5.5).",
                    "Use a host name; the review network's NAT64 resolves names, not literals.");
        }
        for (Hit h : grep("\"http://(?!localhost|127\\.|10\\.0\\.2\\.2|schemas\\.|www\\.w3\\.org)[^\"]+\"")) {
            add(Sev.NOTE, "CLEARTEXT", at(h),
                    "A cleartext http:// URL. iOS ATS and Android block it unless exempted, and the Data safety form asks about encryption in transit.",
                    "Use https.");
        }
    }

    static void checkSecurity() {
        for (Hit h : grep("(?i)(Preferences\\.set|Storage\\.getInstance\\(\\)\\.writeObject)\\(\\s*\"[^\"]*(token|password|secret|session|jwt|apikey|api_key|credential)")) {
            add(Sev.HIGH, "INSECURE-TOKEN", at(h),
                    "A credential is stored in Preferences/Storage, which is plain and is backed up.",
                    "Keep tokens in SecureStorage (com.codename1.security.SecureStorage).");
        }
        for (Hit h : grep("(?i)Log\\.p\\(.*(password|token|email|ssn|creditcard)")) {
            add(Sev.NOTE, "LOGS-PII", at(h),
                    "A log line may print personal data or credentials.",
                    "Remove it from the release build.");
        }
        List<Hit> deep = grep("\\bgetAppArg\\(\\)");
        boolean sensitive = deep.stream().anyMatch(h -> lines.get(h.file).stream()
                .anyMatch(l -> l.matches("(?i).*\\b(delete\\w*|transfer\\w*|pay(ment)?|purchase\\w*|resetPassword|logout|signIn)\\(.*")));
        if (sensitive) {
            add(Sev.NOTE, "DEEPLINK-VALIDATE", at(deep.get(0)),
                    "The app reads deep-link arguments. A link must never perform a sensitive action (pay, delete, sign in) without validating it and asking the user.",
                    "Parse the link, validate it, and confirm sensitive actions on screen.");
        }
    }

    static final String[][] ANDROID_SENSITIVE = {
        {"READ_SMS|RECEIVE_SMS|SEND_SMS|READ_CALL_LOG|WRITE_CALL_LOG|PROCESS_OUTGOING_CALLS", "SMS/call log permissions need a Play declaration and are refused unless the app is a default handler."},
        {"ACCESS_BACKGROUND_LOCATION", "Background location needs a Play declaration, a video and a core feature that needs it."},
        {"MANAGE_EXTERNAL_STORAGE", "All-files access is refused unless the app is a file manager or similar."},
        {"QUERY_ALL_PACKAGES", "QUERY_ALL_PACKAGES needs a Play declaration; declare <queries> for the packages you need instead."},
        {"READ_MEDIA_IMAGES|READ_MEDIA_VIDEO", "Broad photo/video access is allowed only when that is the app's core purpose; use the system photo picker (Display.openGallery) instead."},
        {"READ_CONTACTS", "On API 37 Play expects the contact picker when the app needs only some contacts."},
        {"SCHEDULE_EXACT_ALARM|USE_EXACT_ALARM", "Exact alarms need a declaration; only alarm/calendar apps get USE_EXACT_ALARM."},
        {"BIND_ACCESSIBILITY_SERVICE", "Accessibility services may only be used to help users with disabilities."},
        {"SYSTEM_ALERT_WINDOW", "Overlays draw over other apps; Play restricts them."},
        {"REQUEST_INSTALL_PACKAGES", "Installing packages is restricted on Play."},
    };

    static void checkAndroid() {
        StringBuilder perms = new StringBuilder();
        for (String k : hints.stringPropertyNames()) {
            if (k.startsWith("codename1.arg.android.")) {
                perms.append(hints.getProperty(k)).append('\n');
            }
        }
        String all = perms.toString();
        for (String[] s : ANDROID_SENSITIVE) {
            Matcher m = Pattern.compile("android\\.permission\\.(" + s[0] + ")\\b").matcher(all);
            if (m.find()) {
                add(Sev.HIGH, "PLAY-SENSITIVE-PERMISSION", "codenameone_settings.properties",
                        m.group(1) + ": " + s[1], "Remove it, or fill the Play Console permission declaration before uploading.");
            }
        }
        if ("true".equals(hints.getProperty("codename1.arg.android.onDeviceDebug"))
                || hints.getProperty("codename1.arg.android.xapplication_attr", "")
                        .matches("(?s).*android\\\\?:debuggable\\\\?=\\\\?\"true.*")) {
            add(Sev.BLOCKER, "ANDROID-DEBUGGABLE", "codenameone_settings.properties",
                    "The build is debuggable (android.onDeviceDebug, or android:debuggable in android.xapplication_attr) and is not a release.",
                    "Remove the hint before the store build.");
        }
        if (!"false".equals(hints.getProperty("codename1.arg.android.allowBackup")) && grep("\\bSecureStorage\\b").isEmpty()
                && features.contains("accounts")) {
            add(Sev.NOTE, "ANDROID-BACKUP", "codenameone_settings.properties",
                    "android:allowBackup is on (the default) in an app with accounts; anything in Storage is copied to the user's cloud backup.",
                    "Keep credentials in SecureStorage, or set android.allowBackup=false.");
        }
        String target = hints.getProperty("codename1.arg.android.targetSDKVersion");
        if (target != null) {
            add(Sev.HIGH, "PLAY-TARGET-PINNED", "codenameone_settings.properties",
                    "android.targetSDKVersion is pinned to " + target + "; Play refuses uploads below its current required level.",
                    "Remove the hint so the build targets the level Play requires.");
        }
        // Android has no orientation hint of its own; a lock there comes from
        // code (Display.lockOrientation), so only the iOS hint is read.
        String orient = hints.getProperty("codename1.arg.ios.interface_orientation", "");
        if (!grep("\\blockOrientation\\(").isEmpty()) {
            orient += " portrait";
        }
        if (orient.matches("(?i).*(portrait|landscape).*") && !orient.matches("(?i).*(sensor|unspecified|full).*")) {
            add(Sev.NOTE, "ORIENTATION-LOCK", "codenameone_settings.properties",
                    "Orientation is locked. Android 16+ ignores the lock on large screens and Play's large-screen quality tiers expect a resizable layout.",
                    "Check the layout on a tablet/foldable skin in both orientations.");
        }
    }

    static void checkIos() {
        if (hints.getProperty("codename1.arg.ios.appUsesNonExemptEncryption") == null
                && grep("\\b(Cipher|AESGCM|ChaCha|Rsa|com\\.codename1\\.security)\\b").isEmpty()) {
            add(Sev.NOTE, "EXPORT-COMPLIANCE", "codenameone_settings.properties",
                    "ios.appUsesNonExemptEncryption is not set, so each upload may wait on the encryption question.",
                    "Set codename1.arg.ios.appUsesNonExemptEncryption=false when the app uses only HTTPS and OS crypto.");
        }
        boolean push = !grep("\\bPushCallback\\b|\\bregisterPush\\(").isEmpty();
        if (push) {
            features.add("push");
        }
        String bg = hints.getProperty("codename1.arg.ios.background_modes", "");
        if (bg.contains("remote-notification") && !push) {
            add(Sev.HIGH, "UNUSED-ENTITLEMENT", "codenameone_settings.properties",
                    "Push background mode declared but the app never registers for push (Apple 2.4.5).",
                    "Remove the capability, or wire push up.");
        }
        if (bg.contains("location") && !features.contains("location")) {
            add(Sev.HIGH, "UNUSED-ENTITLEMENT", "codenameone_settings.properties",
                    "Background location mode declared without location use (Apple 2.5.4).",
                    "Remove it.");
        }
        List<Hit> track = grep("(?i)\\b(AppTrackingTransparency|requestTrackingAuthorization|advertisingIdentifier|AdMob|FacebookAds|AppsFlyer|Adjust\\b|Branch\\b)");
        if (!track.isEmpty()) {
            features.add("tracking");
            if (hints.getProperty("codename1.arg.ios.NSUserTrackingUsageDescription") == null) {
                add(Sev.BLOCKER, "ATT-MISSING", at(track.get(0)),
                        "An ads/attribution SDK without ios.NSUserTrackingUsageDescription (Apple 5.1.2).",
                        "Show the ATT prompt before the SDK starts, set the purpose string, and list the data in the privacy labels.");
            }
        }
        List<Hit> web = grep("\\bBrowserComponent\\b");
        if (!web.isEmpty()) {
            features.add("web");
            long forms = grep("\\bextends Form\\b|\\bnew Form\\(").size();
            if (forms <= 2) {
                add(Sev.HIGH, "WEB-WRAPPER", at(web.get(0)),
                        "Few native screens and a BrowserComponent: an app that is mostly a website is rejected (Apple 4.2).",
                        "Build the core flows as native screens; keep the browser for links and documents.");
            }
        }
        if (!grep("(?i)\"[^\"]*\\b(heart rate|step count|calories|sleep tracking|blood|glucose|workout|diagnos\\w*)\\b[^\"]*\"").isEmpty()) {
            features.add("health");
        }
        if (!grep("(?i)\"[^\"]*\\b(place a bet|wager|casino|poker|slot machine|sweepstakes?)\\b[^\"]*\"").isEmpty()) {
            features.add("gambling");
        }
    }

    static void checkMetadataWords() {
        for (Hit h : grep("(?i)\"[^\"]*\\b(google play|play store|android version|get it on google)\\b[^\"]*\"")) {
            add(Sev.HIGH, "OTHER-PLATFORM", at(h),
                    "UI text names another platform; the iOS build shows it to Apple's reviewer (Apple 2.3.10).",
                    "Show it only on Android (Display.getInstance().getPlatformName()), or remove it.");
        }
        for (Hit h : grep("(?i)\"[^\"]*\\b(on the app store|download on the|available on the app store)\\b[^\"]*\"")) {
            add(Sev.HIGH, "OTHER-PLATFORM", at(h),
                    "UI text points to the App Store; Play rejects references to another store.",
                    "Show it only on iOS, or remove it.");
        }
        for (Hit h : grep("(?i)\"[^\"]*\\b(apple sucks|unlike apple|apple won't let|blame apple)\\b")) {
            add(Sev.HIGH, "NEGATIVE-APPLE", at(h), "Text criticising Apple is rejected (Apple 2.3).", "Remove it.");
        }
    }

    static void checkNetwork(List<String> urls) {
        for (String u : urls) {
            int code = fetch(u);
            if (code < 200 || code >= 400) {
                add(Sev.BLOCKER, "URL-UNREACHABLE", u,
                        "Listing URL answers " + (code < 0 ? "nothing" : "HTTP " + code) + " (Apple 1.5/5.1.1, Google User Data).",
                        "Publish the page and make it reachable without sign-in.");
            }
        }
        Set<String> hosts = new LinkedHashSet<>();
        Pattern host = Pattern.compile("https://([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
        for (List<String> ls : lines.values()) {
            for (String l : ls) {
                Matcher m = host.matcher(l);
                while (m.find()) {
                    hosts.add(m.group(1).toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
        for (String h : hosts) {
            if (h.endsWith("example.com") || h.endsWith("w3.org") || h.endsWith("codenameone.com")) {
                continue;
            }
            try {
                InetAddress[] all = InetAddress.getAllByName(h);
                if (Stream.of(all).noneMatch(a -> a instanceof Inet6Address)) {
                    add(Sev.NOTE, "NO-IPV6", h,
                            "No AAAA record. Apple's NAT64 review network still reaches it by name, but check the server does not reject the synthesized address.",
                            "Optional: publish an IPv6 address for the backend.");
                }
            } catch (IOException e) {
                add(Sev.HIGH, "HOST-UNRESOLVED", h, "The host does not resolve; the reviewer will see a broken app.",
                        "Fix the host name or deploy the server.");
            }
        }
    }

    // ------------------------------------------------------------- manual list

    static List<String> manualItems() {
        List<String> m = new ArrayList<>();
        m.add("Privacy policy URL in both listings and reachable inside the app; it names every SDK/AI provider that receives data.");
        m.add("Apple privacy labels and Google Data safety form answered from the same inventory of collected data.");
        m.add("Apple age-rating questionnaire (the current one, with 13+/16+/18+) and Google content rating completed; an unrated app is removed from Play.");
        m.add("Age assurance: if the app is offered in US states with App Store Accountability Acts, read the store's age signal (Declared Age Range / Play Age Signals) only for age-gating, and handle consent being revoked.");
        m.add("Screenshots and preview video show the real app, every button in them works, no device frames in the video, no Apple hardware images.");
        m.add("Support and marketing URLs load (run with --network --url ...).");
        m.add("Every screen of the release build works against the production backend; no hidden or remotely enabled features.");
        m.add("Version/build number higher than the last upload.");
        m.add("iPad layout checked if the iPhone app runs on iPad; tablet/foldable layout checked for Play.");
        m.add("Accessibility: controls labelled for VoiceOver/TalkBack, text scales, contrast passes (run Sim.java verify); answer Apple's Accessibility Nutrition Labels truthfully.");
        m.add("Google Play: package name registered for developer verification; organization account if the app is finance, health, gambling or another regulated category.");
        m.add("Google Play, new personal account: closed test with 12 testers for 14 days before production.");
        m.add("EU distribution: trader status and contact details in App Store Connect / Play Console (DSA); product-safety (GPSR) contact for physical-goods apps.");
        if (features.contains("accounts")) {
            m.add("Demo account with sample data in App Review notes and Play's App access section, plus what to test and where.");
            m.add("Account deletion deletes server data; web deletion URL entered in Play Console; Apple token revoked on deletion when Sign in with Apple is used.");
            m.add("Sign in with Apple uses Apple's button style, does not re-ask for the name/email Apple returned, and the private-relay email domain is registered so mail reaches relay addresses.");
            m.add("Signed-in state survives a move to a new Android device (Restore Credentials), or sign-in is quick.");
        }
        if (features.contains("iap")) {
            m.add("IAP products are Ready to Submit in App Store Connect and active in Play Console before the build is submitted.");
            m.add("Paywall: price, period, contents, auto-renewal, how to cancel, privacy and terms links on the buy screen; free trial says what is charged when it ends.");
            m.add("A paid feature works if the user denies an optional permission.");
        }
        if (features.contains("subscriptions")) {
            m.add("Cancelling is as easy as subscribing (no phone call or email); EU users get a withdrawal option.");
        }
        if (features.contains("ai")) {
            m.add("AI: consent screen names the provider and the data; output can be reported; Play image/video generators have intimate-imagery safeguards and a full-access test account.");
        }
        if (features.contains("ugc")) {
            m.add("User content: report, block, filtering, action on reports within 24 hours, published contact; social-media question in App Store Connect answered if asked.");
        }
        if (features.contains("tracking")) {
            m.add("ATT prompt shown before any tracking SDK starts; features not gated on Allow; labels say Data Used to Track You.");
        }
        if (features.contains("health")) {
            m.add("No medical claims in app or listing; health data out of analytics, ads and iCloud backup.");
        }
        if (features.contains("gambling")) {
            m.add("Real-money gambling: licence for every country offered (Brazil requires its own), organization account on Play, geo-restriction.");
        }
        if (features.contains("lootbox")) {
            m.add("Loot box odds shown before purchase.");
        }
        if (features.contains("push")) {
            m.add("Push is never required to use the app and carries no ads without explicit opt-in.");
        }
        if (features.contains("location") || features.contains("camera") || features.contains("contacts")
                || features.contains("microphone")) {
            m.add("Permissions requested in context after a one-line explanation (button not labelled Allow); a denial leaves the rest of the app working.");
        }
        return m;
    }

    // ------------------------------------------------------------- reporting

    static void report(List<String> manual) {
        long b = count(Sev.BLOCKER), h = count(Sev.HIGH), n = count(Sev.NOTE);
        System.out.println("Store audit of " + root);
        System.out.println("Uses: " + (features.isEmpty() ? "nothing review-sensitive detected" : String.join(", ", features)));
        System.out.println(b + " BLOCKER, " + h + " HIGH, " + n + " NOTE");
        for (Sev s : Sev.values()) {
            for (Finding f : findings) {
                if (f.sev == s) {
                    System.out.println();
                    System.out.println("[" + f.sev + "] " + f.id + "  " + f.where);
                    System.out.println("  " + f.msg);
                    System.out.println("  fix: " + f.fix);
                }
            }
        }
        System.out.println();
        System.out.println("MANUAL -- confirm with the developer (code cannot prove these):");
        for (String m : manual) {
            System.out.println("  - " + m);
        }
    }

    static void writeChecklist(Path out, List<String> manual) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Store submission checklist\n\n");
        sb.append("Generated by `tools/StoreAudit.java` for `").append(root.getFileName()).append("`. ");
        sb.append("Detected: ").append(features.isEmpty() ? "nothing review-sensitive" : String.join(", ", features)).append(".\n\n");
        sb.append("## Automated findings\n\n");
        if (findings.isEmpty()) {
            sb.append("- [x] No automated findings.\n");
        }
        for (Sev s : Sev.values()) {
            for (Finding f : findings) {
                if (f.sev == s) {
                    sb.append("- [ ] **").append(f.sev).append(" ").append(f.id).append("** `").append(f.where)
                      .append("` -- ").append(f.msg).append(" Fix: ").append(f.fix).append('\n');
                }
            }
        }
        sb.append("\n## Confirm with the developer\n\n");
        for (String m : manual) {
            sb.append("- [ ] ").append(m).append('\n');
        }
        sb.append("\n## Ship\n\n- [ ] All BLOCKER and HIGH items fixed and the audit re-run clean.\n");
        sb.append("- [ ] Release build uploaded with a higher build number.\n");
        sb.append("- [ ] Review notes written: demo account, what to test, anything that needs hardware or a location.\n");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
    }

    // --------------------------------------------------------------- helpers

    static List<Hit> grep(String regex) {
        Pattern p = Pattern.compile(regex);
        List<Hit> out = new ArrayList<>();
        for (Map.Entry<Path, List<String>> e : lines.entrySet()) {
            List<String> ls = e.getValue();
            for (int i = 0; i < ls.size(); i++) {
                String l = ls.get(i);
                String t = l.trim();
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("import ") || t.startsWith("#")) {
                    continue;
                }
                if (p.matcher(l).find()) {
                    out.add(new Hit(e.getKey(), i + 1, t));
                }
            }
        }
        return out;
    }

    static String at(Hit h) {
        return root.relativize(h.file) + ":" + h.line;
    }

    static void add(Sev s, String id, String where, String msg, String fix) {
        findings.add(new Finding(s, id, where, msg, fix));
    }

    static long count(Sev s) {
        return findings.stream().filter(f -> f.sev == s).count();
    }

    static int fetch(String u) {
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create(u).toURL().openConnection();
            c.setInstanceFollowRedirects(true);
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", "StoreAudit");
            int code = c.getResponseCode();
            c.disconnect();
            return code;
        } catch (IOException | IllegalArgumentException e) {
            return -1;
        }
    }

    static Path firstExisting(Path... ps) {
        for (Path p : ps) {
            if (Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    static String need(String[] a, int i) {
        if (i >= a.length) {
            usage();
            System.exit(2);
        }
        return a[i];
    }

    static void usage() {
        System.err.println("usage: java tools/StoreAudit.java [project-dir] [--checklist out.md] [--strict]"
                + " [--network [--url <listing-url>]...]");
    }
}
