# Evidence map

Source: `docs/website/content/blog/developer-workflow-debug-and-junit.md`
Canonical: https://www.codenameone.com/blog/developer-workflow-debug-and-junit/

## Thesis

JDWP debugging on real devices plus standard JUnit 5 simulator tests

## Supported beats

- **On-device debugging that treats Java as Java:** Codename One has always supported on-device debugging in the strict technical sense. You could attach Xcode to a .ipa, you could attach Android Studio to a running APK, you could read the native call stack, you could step through Objective-C or the C that ParparVM emits.
- **What it looks like:** The same Debug tool window you use for any other Java project. Frames panel on the left has the full Java call stack. The Variables panel shows this and the locals as Java values, with the same drill-down you would get on a regular JVM.
- **How the pieces fit together:** On iOS the IDE never talks to the device directly. The CN1 Debug Proxy is a small Java process you run on your developer machine.
- **Tutorial: IntelliJ + iOS:** The Codename One archetype now generates two run configurations under an On-Device Debug folder in the IntelliJ run-config dropdown: CN1 Debug Proxy and CN1 Attach iOS. The tutorial below assumes a project generated from the Initializr recently enough to have those.
- **NetBeans + iOS:** NetBeans uses the same proxy and the same connection order. Start the proxy, launch the app, and wait for the device handshake and symbol-loading lines shown above. Then choose Debug > Attach Debugger in NetBeans.
- **Tutorial: IntelliJ + Android:** Android is simpler because the proxy is not needed. The archetype generates two run configurations under the same On-Device Debug folder: CN1 Android On-Device Debug (Maven, builds and installs the APK and forwards JDWP) and CN1 Attach Android (Remote JVM Debug at localhost:5005).

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/4999
- https://github.com/codenameone/CodenameOne/pull/5012
- https://www.codenameone.com/developer-guide/#_on_device_debugging_ios
- https://www.codenameone.com/developer-guide/#_on_device_debugging_android
- https://github.com/codenameone/CodenameOne/pull/5032
- https://www.codenameone.com/developer-guide/#_testing_with_junit_5
