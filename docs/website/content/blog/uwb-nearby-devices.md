---
title: "UWB and Nearby Devices: Distance, Direction, Association, and Transport"
slug: uwb-nearby-devices
url: /blog/uwb-nearby-devices/
date: '2026-08-30'
author: Shai Almog
description: "Codename One now exposes UWB distance and direction, companion-device association, presence, and local device transport through separate capability-driven APIs."
feed_html: '<img src="https://www.codenameone.com/blog/uwb-nearby-devices.jpg" alt="A phone measuring the distance and direction to nearby devices" /> Codename One now exposes UWB distance and direction, companion-device association, presence, and local device transport through separate capability-driven APIs.'
series: ["release-2026-08-28"]
---

![A phone measuring the distance and direction to nearby devices](/blog/uwb-nearby-devices.jpg)

Bluetooth can tell you that a device is probably nearby. Ultra-wideband can tell you that it is 42 centimeters away and 18 degrees to the right, until the person holding it turns and the direction disappears.

That last clause is part of the API contract. Nearby hardware gives partial answers, permissions change by operating-system version, and Apple's local transport cannot discover Google's. [PR #5589](https://github.com/codenameone/CodenameOne/pull/5589) exposes those boundaries through three packages under `com.codename1.nearby`.

For multiple native windows, Document Provider, and the rest of this release, see the [weekly release overview](/blog/native-desktop-windows/).

## Three packages answer three different questions

The API is split by capability:

| Package | Question |
| --- | --- |
| `com.codename1.nearby.ranging` | How far away is the peer, and in which direction? |
| `com.codename1.nearby.companion` | Which accessory did the user associate with this app? |
| `com.codename1.nearby.transport` | How do two nearby instances exchange a payload? |

{{< mermaid >}}
flowchart LR
    A[Nearby device feature] --> R[Ranging<br/>UWB distance and direction]
    A --> C[Companion<br/>user-approved association]
    A --> T[Transport<br/>discovery and payloads]
    R --> X[Existing token channel<br/>usually Bluetooth LE]
    C --> B[Bluetooth device handle]
    T --> P[Same-platform peer]
{{< /mermaid >}}

Referencing a package is the opt-in. An application that imports only ranging does not receive Nearby Connections, MultipeerConnectivity, local-network prompts, or companion-profile permissions. The build scans the bytecode and includes the frameworks needed by the packages it finds.

Use capability queries instead of platform checks:

```java
if (Ranging.isSupported()) {
    // Offer precision finding.
}
if (CompanionDevices.isSupported()) {
    // Offer association.
}
if (NearbyTransport.isSupported()) {
    // Offer local peer transfer.
}
```

UWB hardware is absent on many current phones. The application still needs a useful state when ranging is unavailable.

## Ranging starts after the peers exchange tokens

Ultra-wideband measures radio time of flight. The practical target is roughly ten-centimeter ranging, while a Bluetooth signal-strength estimate can swing by meters when a hand covers the phone. UWB still does not discover or identify the peer by itself.

Both devices first exchange a ranging token over a channel they already share. A Bluetooth LE GATT characteristic is the common choice. Once the other token arrives, the session can start:

```java
Ranging.prepareSession(RangingRole.CONTROLLER).onResult((session, err) -> {
    if (err != null) {
        showBluetoothFallback();
        return;
    }

    bluetoothCharacteristic.write(
            session.getLocalToken().toByteArray());

    session.addRangingListener(new RangingAdapter() {
        public void updated(RangingUpdate update) {
            if (update.hasDistance()) {
                distance.setText(Math.round(update.getDistance(
                        RangingUnit.CENTIMETERS)) + " cm");
            }
            if (update.hasDirection()) {
                arrow.setAngle(update.getAzimuth());
            }
        }
    });

    session.start(RangingToken.fromByteArray(theirToken));
});
```

Every `RangingUpdate` field except its timestamp is optional. A peer behind the phone can report distance without direction. At the edge of radio range, both can disappear temporarily. Check `hasDistance()` and `hasDirection()` independently.

Azimuth runs from -180 to 180 degrees, with zero straight ahead and positive values to the right. Elevation runs from -90 to 90. Android reports the angles. iOS reports a direction vector, and the port derives the same angles while retaining the original vector.

One session tracks one peer. This reflects Apple's `NINearbyPeerConfiguration` limit. Track several peers with several sessions.

Accessory ranging has different handshakes. iOS uses the Nearby Interaction Accessory Protocol and returns bytes that must be forwarded to the accessory. Android joins the UWB address, channel, preamble, session id, and key named by the accessory. A token serialized on one platform is rejected on the other instead of being handed to an incompatible native API.

## Association asks the user which device belongs to the app

Companion association is not Bluetooth pairing. The operating system draws a chooser, the user selects one accessory, and the app receives a durable association id.

```java
AssociationRequest request = new AssociationRequest.Builder()
        .addFilter(DeviceFilter.bleService("180D"))
        .build();

CompanionDevices.associate(request).onResult((device, err) -> {
    if (err == null) {
        Preferences.set("sensor", device.getId());
        CompanionDevices.startObservingPresence(device.getId());
    }
});
```

The association survives restarts and reboots. `CompanionDevice.getAddress()` is also the identifier accepted by `BluetoothLE.getPeripheral(String)`, so the result connects directly to the existing BLE API.

Android can report companion presence from API 31. Register the listener during `init()` because the operating system can record a sighting while no form exists. Codename One replays the stored event when the app next initializes. It does not silently turn presence into unrestricted background execution.

iOS 18 uses AccessorySetupKit for association. It does not provide the same live presence event. An application that needs proximity on iOS should scan with the Bluetooth API. The service UUIDs iOS may show must be declared through `ios.nearby.accessoryServices` before the app is built.

## Nearby transport is local, but not cross-ecosystem

`NearbyTransport` maps to Google Nearby Connections on Android and MultipeerConnectivity on Apple platforms:

```java
NearbyTransport.addTransportListener(new TransportAdapter() {
    public void endpointFound(Endpoint endpoint) {
        NearbyTransport.requestConnection(endpoint, "Shai's phone");
    }

    public void connectionRequested(IncomingConnection request) {
        showCodeOnScreen(request.getAuthenticationToken());
        request.accept();
    }

    public void connected(Endpoint endpoint) {
        NearbyTransport.send(endpoint, Payload.fromBytes(data));
    }

    public void payloadReceived(Endpoint endpoint, Payload payload) {
        process(payload.getBytes());
    }
});

NearbyTransport.startAdvertising(
        "chat", "Shai's phone", TransportStrategy.CLUSTER);
NearbyTransport.startDiscovery("chat", TransportStrategy.CLUSTER);
```

Android derives a short authentication token from the connection's key exchange. Show it on both screens before accepting a sensitive connection. iOS supplies no equivalent token. The API returns an empty value there instead of constructing a decorative code a relay could reproduce.

An Android phone discovers another Android endpoint. An iPhone discovers another Apple endpoint. The two native stacks do not share a wire protocol. Use BLE L2CAP or Bonjour and sockets when the product needs iPhone-to-Android communication.

Byte payloads are intentionally small. Larger data uses a file payload and reports progress. A resolved send means the platform accepted the payload; `PayloadStatus.SUCCESS` means the peer received it.

## Develop the failure states without UWB hardware

The simulator, desktop ports, and JavaScript port implement local simulated sessions rather than returning a stub. They report `NearbyAvailability.LOCAL_ONLY`, which lets the app label the peer as simulated.

This covers the application logic that usually consumes the time: an arrow that loses direction, a distance label that becomes unavailable, an association chooser that is canceled, a connection that is rejected, and a payload that fails after it was accepted for sending.

The simulator cannot prove antenna behavior, radio coexistence, entitlement signing, or real-device permission prompts. Run those on the target hardware before treating the feature as complete.

## Reach without pretending the platforms agree

Nearby Devices expands the physical reach of a shared application without flattening three different system contracts into one unreliable call. Ranging admits that direction can disappear. Companion association names the versions and profiles that exist. Transport says that the Apple and Google protocols cannot find each other.

The rest of this week's release keeps those boundaries visible. Desktop windows exist only where the port has a window manager. Document Provider publishes a named read-only tree. Watch complications lower the shared surface model into the constrained data a watch face actually accepts. Rootless jailbreak detection adds current signals, then leaves high-value authorization on the backend.

Secure-by-default programming often starts with an API that can say no. Capability queries, optional ranging fields, a user-approved association, and a visible authentication boundary make failure part of the normal path instead of a surprise native exception.

The {{< post-link path="/blog/watch-complications-wear-companion" text="next post closes the watch gaps we documented last week" >}}.

---

## Discussion

_Would distance, direction, or a durable companion association change how your application finds a device?_

{{< giscus >}}
