// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::advanced-topics-under-the-hood-java-001[]
public interface MyNative extends NativeInterface {
    String hello(String name);
}
// end::advanced-topics-under-the-hood-java-001[]

// tag::advanced-topics-under-the-hood-java-002[]
android.debug
android.release
android.installLocation
android.licenseKey
android.stack_size
android.statusbar_hidden
android.googleAdUnitId
android.includeGPlayServices
android.headphoneCallback
android.gpsPermission
android.asyncPaint
android.supportV4
android.theme
android.cusom_layout1
android.versionCode
android.captureRecord
android.removeBasePermissions
android.blockExternalStoragePermission
android.min_sdk_version
android.smallScreens
android.streamMode
android.enableProguard
android.targetSDKVersion
android.web_loading_hidden
facebook.appId
facebook.clientToken
ios.keyboardOpen
ios.project_type
ios.newStorageLocation
ios.prerendered_icon
ios.application_exits
ios.themeMode
ios.xcode_version
javascript.inject_proxy
javascript.minifying
javascript.proxy.url
javascript.sourceFilesCopied
javascript.teavm.version
google.adUnitId
ios.includePush
ios.headphoneCallback
ios.enableAutoplayVideo
ios.googleAdUnitId
ios.googleAdUnitIdPadding
ios.enableBadgeClear
ios.locationUsageDescription
ios.bundleVersion
ios.objC
ios.testFlight
ios.metal
desktop.width
desktop.height
desktop.adaptToRetina
desktop.resizable
desktop.fontSizes
desktop.theme
desktop.themeMac
desktop.themeWin
desktop.windowsOutput
noExtraResources
android.permission.<PERMISSION_NAME>
// end::advanced-topics-under-the-hood-java-002[]

// tag::advanced-topics-under-the-hood-java-003[]
android.xapplication
android.xpermissions
android.xintent_filter
android.facebook_permissions
android.stringsXml
android.style
android.nonconsumable
android.xapplication_attr
android.xactivity
android.pushVibratePattern
android.proguardKeep
android.sharedUserId
android.sharedUserLabel
ios.urlScheme
ios.interface_orientation
ios.plistInject
ios.facebook_permissions
ios.applicationDidEnterBackground
ios.viewDidLoad
ios.glAppDelegateHeader
ios.glAppDelegateBody
ios.beforeFinishLaunching
ios.afterFinishLaunching
ios.add_libs
// end::advanced-topics-under-the-hood-java-003[]

// tag::advanced-topics-under-the-hood-java-004[]
Form hi = new Form("Rearrangeable Items", new BorderLayout());
String[] buttons = {"A Game of Thrones", "A Clash Of Kings",  "A Storm Of Swords",
    "A Feast For Crows", "A Dance With Dragons", "The Winds of Winter", "A Dream of Spring" };

Container box = new Container(BoxLayout.y());
box.setScrollableY(true);
box.setDropTarget(true);
java.util.List<String> got = Arrays.asList(buttons);
Collections.shuffle(got);
for(String current : got) {
    MultiButton mb = new MultiButton(current);
    box.add(mb);
    mb.setDraggable(true);
}

hi.add(BorderLayout.NORTH, "Arrange The Titles").add(BorderLayout.CENTER, box);
hi.show();
// end::advanced-topics-under-the-hood-java-004[]

// tag::advanced-topics-under-the-hood-java-005[]
private static boolean isTrustedCallerSignature(Context ctx, String packageName, Set<String> allowedSha256) {
    if (packageName == null || packageName.isEmpty()) return false;
    try {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo info;
        if (Build.VERSION.SDK_INT >= 28) {
            info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
            Signature[] sigs = info.signingInfo != null
                    ? info.signingInfo.getApkContentsSigners()
                    : null;
            return hasAllowedFingerprint(sigs, allowedSha256);
        } else {
            info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            return hasAllowedFingerprint(info.signatures, allowedSha256);
        }
    } catch (Exception ex) {
        return false;
    }
}

private static boolean hasAllowedFingerprint(Signature[] sigs, Set<String> allowedSha256) throws Exception {
    if (sigs == null) return false;
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    for (Signature s : sigs) {
        String fp = toHex(md.digest(s.toByteArray()));
        if (allowedSha256.contains(fp)) return true;
    }
    return false;
}

private static String toHex(byte[] data) {
    StringBuilder out = new StringBuilder(data.length * 2);
    for (byte b : data) out.append(String.format("%02X", b));
    return out.toString();
}
// end::advanced-topics-under-the-hood-java-005[]

// tag::advanced-topics-under-the-hood-java-006[]
public class WalletBridgeActivity extends Activity {
    private static WalletBridgeActivity active;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        active = this;

        Intent in = getIntent();
        String caller = getCallingPackage();

        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra("wallet_payload", in.getStringExtra("payload"));
        launch.putExtra("wallet_caller", caller == null ? "" : caller);
        startActivity(launch);
        // Do not finish yet. CN1 will finish this activity via native callback once verification completes.
    }

    @Override
    protected void onDestroy() {
        if (active == this) {
            active = null;
        }
        super.onDestroy();
    }

    public static WalletBridgeActivity getActive() {
        return active;
    }
}
// end::advanced-topics-under-the-hood-java-006[]

// tag::advanced-topics-under-the-hood-java-007[]
@Override
public void start() {
    Display d = Display.getInstance();
    String action = d.getProperty("android.intent.action", null);
    if ("com.example.wallet.ACTION_VERIFY".equals(action)) {
        boolean callerVerified = "true".equals(d.getProperty("android.intent.caller.verified", "false"));
        String payload = d.getProperty("android.intent.extra.wallet_payload", d.getProperty("AppArg", null));
        String caller = d.getProperty("android.intent.caller", "");

        if (!callerVerified || !isAllowedCaller(caller)) {
            failClosed(); // return declined/canceled via bridge completion API
            return;
        }

        // Run authentication + back end verification, then call native bridge completion API.
        return;
    }
    showMainForm();
}
// end::advanced-topics-under-the-hood-java-007[]
