---
title: WiFi, Bonjour, USB, And Network-Type Listeners
slug: wifi-bonjour-usb-network-type
url: /blog/wifi-bonjour-usb-network-type/
date: '2026-05-31'
author: Shai Almog
description: First-class WiFi info, WiFi scan and connect, Bonjour / mDNS discovery, WiFi Direct, USB host, and NetworkManager network-type listeners. All in the framework core. All with auto-injection of Android permissions and iOS entitlements based on which classes you reference.
feed_html: '<img src="https://www.codenameone.com/blog/wifi-bonjour-usb-network-type.jpg" alt="WiFi, Bonjour, USB, And Network-Type Listeners" /> First-class WiFi info, WiFi scan and connect, Bonjour / mDNS discovery, WiFi Direct, USB host, and NetworkManager network-type listeners. All in the framework core, with auto-injected permissions and entitlements.'
---

![WiFi, Bonjour, USB, And Network-Type Listeners](/blog/wifi-bonjour-usb-network-type.jpg)

[PR #5021](https://github.com/codenameone/CodenameOne/pull/5021) lands a new set of APIs in the framework core for apps that need to do more with the network than open an HTTP socket. The unifying idea is the same one we have been applying for the past few weeks (and that the [NFC, Crypto, Biometrics post](/blog/nfc-crypto-biometrics-and-build-cloud/) talked about explicitly): fundamental device APIs should not require you to track down a cn1lib and hope it is maintained. They should be part of the framework, with auto-injected permissions, conservative defaults, and a simulator path that lets you test without a real radio.

The new packages are:

- `com.codename1.io.wifi` for WiFi info, scan, and connect.
- `com.codename1.io.bonjour` for mDNS / Zeroconf discovery and publishing.
- `com.codename1.io.usb` for USB host (Android-only by platform, but the API surface is on every port).
- A new `NetworkManager.addNetworkTypeListener` and `NETWORK_TYPE_*` constants for apps that want to react to a transition between cellular, WiFi, ethernet, or "none".

There is a new chapter in the developer guide at [Network-Connectivity.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Network-Connectivity.asciidoc) that covers every surface in detail. The rest of this post is the highlight reel and the parts that are worth knowing before you reach for them.

## WiFi info

The simple case first. You have a device on a network and you want to know which network. `com.codename1.io.wifi.WiFi`:

```java
WiFi wifi = WiFi.getInstance();
String ssid    = wifi.getCurrentSSID();   // "MyNetwork"
String bssid   = wifi.getBSSID();         // "aa:bb:cc:dd:ee:ff"
String gateway = wifi.getGateway();       // "192.168.1.1"
String ip      = wifi.getIp();            // "192.168.1.42"
```

On Android this goes through `WifiManager` and `ConnectivityManager`. On iOS it routes through `CNCopySupportedInterfaces` / `CNCopyCurrentNetworkInfo` for SSID and BSSID and `getifaddrs` for IP and gateway. On the JavaSE simulator it derives what it can from `NetworkInterface` and logs the production permissions that would be required.

iOS, in particular, asks for the wide-net entitlement (`com.apple.developer.networking.wifi-info`) when the build server sees any reference to these methods on your classpath, and asks for the location-when-in-use Info.plist string (because Apple ties SSID access to having location permission). You do not have to add those by hand. The build pipeline scans your classpath and adds them; if you remove the WiFi calls again, the entitlement and the plist string go away on the next build.

## WiFi scan and connect

A step up from "what network am I on" is "what networks are around, and please connect me to one":

```java
WiFi.getInstance().scan(new ScanOptions().setTimeoutMillis(5000))
   .onResult((results, err) -> {
       if (err != null) return;
       for (ScanResult r : results) {
           System.out.println(r.ssid + " (" + r.bssid + "), "
                   + r.signalLevel + " dBm, "
                   + r.security);
       }
   });

WiFi.getInstance()
   .connect("MyNetwork", "hunter2", Security.WPA2_PSK)
   .onResult((success, err) -> {
       if (success) onConnected();
       else         onConnectFailed(err);
   });
```

This is the part that takes the platform-by-platform divergence seriously. Android implementations route through `WifiNetworkSpecifier` on API 29 and above, with a `WifiConfiguration` fallback for older releases. iOS uses `NEHotspotConfigurationManager`, and the entitlement that authorises it (`com.apple.developer.networking.HotspotConfiguration`) is auto-injected only when `WiFi.connect` is referenced. Apps that only read the SSID do not pay for the connect entitlement. JavaSE scan returns a small set of fabricated results and `connect` is a no-op with a log line. We are explicit in the dev guide that this is for code-path exercise, not for "the simulator behaves identically to the device" purposes.

iOS does not support programmatic WiFi scanning at all; the API throws `UnsupportedOperationException` on iOS for `scan()` regardless of what entitlements you add. That is not a Codename One limitation; it is Apple's. The simulator behaviour is documented and the iOS behaviour is documented. There is no surprise at runtime.

## Bonjour / mDNS

For apps that talk to printers, speakers, hubs, dev boards, or any of the other things that announce themselves on a local network, `com.codename1.io.bonjour`:

```java
BonjourBrowser browser = new BonjourBrowser("_http._tcp.");
browser.addListener(evt -> {
    BonjourService svc = evt.getService();
    System.out.println("Saw " + svc.getName() + " at "
            + svc.getHost() + ":" + svc.getPort());
});
browser.start();
```

```java
BonjourPublisher pub = new BonjourPublisher(
        "_myapp._tcp.", "Living Room Pi", 8080);
pub.addTxtRecord("version", "1.2");
pub.publish();
```

Android routes through `NsdManager`. iOS uses `NSNetServiceBrowser` / `NSNetService` (and asks for the `NSLocalNetworkUsageDescription` string, plus a `NSBonjourServices` array that defaults to `_http._tcp.` and is configurable via the `ios.NSBonjourServices` build hint if you publish or browse non-HTTP services). JavaSE will pick up JmDNS if it is on the classpath; if not, you get a no-op with a log.

## WiFi Direct and USB host (Android)

`com.codename1.io.wifi.WiFiDirect` and `com.codename1.io.usb.Usb` are the two surfaces that are Android-only by platform reality. iOS does not expose WiFi Direct to third-party apps, and iOS USB-host is an MFi-program affair we cannot ship a general-purpose API on top of. The API is still on every port, the iOS implementations report unsupported, and your code does not need a platform `if` statement; you call the method, you handle the unsupported result the same way you would handle a permission denial.

On Android both go through the standard system services (`WifiP2pManager` and `UsbManager`), the auto-injected permissions list is the right one (`NEARBY_WIFI_DEVICES` on `targetSdkVersion >= 33`, the `android.hardware.usb.host` feature for USB), and the simulator behaviour mirrors iOS.

## Network-type listeners

The smallest of the four additions and probably the most broadly useful:

```java
NetworkManager.getInstance().addNetworkTypeListener(evt -> {
    int type = evt.getNetworkType();
    if (type == NetworkManager.NETWORK_TYPE_NONE) {
        showOfflineBanner();
    } else if (type == NetworkManager.NETWORK_TYPE_CELLULAR) {
        suppressLargeBackgroundDownloads();
    } else {
        clearOfflineBanner();
    }
});
```

The constants are `NETWORK_TYPE_NONE`, `NETWORK_TYPE_WIFI`, `NETWORK_TYPE_CELLULAR`, `NETWORK_TYPE_ETHERNET`, `NETWORK_TYPE_OTHER`. On Android the listener is backed by `ConnectivityManager.NetworkCallback`; on iOS by `SCNetworkReachability`. The fast path is that the listener fires only on real transitions, not on every status poll, so it is safe to use it to drive UI banners or to throttle background work without worrying about flicker.

`NetworkManager.getCurrentNetworkType()` is the synchronous accessor for the same value when you want to make a one-shot decision (which transport to prefer for this single upload, say).

## On entitlements and Apple's API-usage scanner

The part of this PR that is least visible from application code but is structurally important is the iOS Objective-C gating. Three new compile-time defines (`CN1_INCLUDE_WIFI_INFO`, `CN1_INCLUDE_HOTSPOT`, `CN1_INCLUDE_BONJOUR`) wrap the native code that calls `CNCopyCurrentNetworkInfo`, `NEHotspotConfigurationManager`, and `NSNetServiceBrowser`. The build server sets each define only when your classpath scanner sees the corresponding Java API in use. The result is that an app that never references `WiFi.connect` does not even compile the `NEHotspotConfigurationManager` code, and the `HotspotConfiguration` entitlement is not requested.

The practical consequence is that Apple's API-usage scanner does not flag your binary for entitlements it has not seen the matching APIs in. This is the same shape we used for [NFC last week](/blog/nfc-crypto-biometrics-and-build-cloud/#nfc), and it is the right answer for "these APIs are part of the core, but apps that do not use them should not pay for them at review time".

## Wrapping up

WiFi, Bonjour, USB, and network-type listeners. All in the core. Auto-injected permissions, conservative defaults, simulator paths for the parts that can be simulated, and unsupported markers for the parts that cannot. Documented under [Network-Connectivity.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Network-Connectivity.asciidoc).

Companion cloud build server change tracked at [BuildDaemon #83](https://github.com/codenameone/BuildDaemon/pull/83); local builds and cloud builds match.

Tomorrow's post covers OIDC and the new passkey / WebAuthn stack.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
