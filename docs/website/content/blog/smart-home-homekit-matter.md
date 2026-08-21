---
title: "Smart Home: HomeKit, Matter, and Google Home Behind One API"
slug: smart-home-homekit-matter
url: /blog/smart-home-homekit-matter/
date: '2026-08-24'
author: Shai Almog
description: "Codename One now exposes smart-home accessories, traits, scenes, change subscriptions, and Matter commissioning through one capability-driven API with native and simulated backends."
feed_html: '<img src="https://www.codenameone.com/blog/smart-home-homekit-matter.jpg" alt="Lights, locks, and thermostats mapped from HomeKit, Matter, and Google Home into one Java API" /> Codename One now exposes smart-home accessories, traits, scenes, change subscriptions, and Matter commissioning through one capability-driven API with native and simulated backends.'
series: ["release-2026-08-21"]
---

![Lights, locks, and thermostats mapped from HomeKit, Matter, and Google Home into one Java API](/blog/smart-home-homekit-matter.jpg)

A light can expose brightness through a HomeKit characteristic or a Matter cluster. Application code should ask for brightness, not carry both platform identifiers and two sets of value rules.

[PR #5554](https://github.com/codenameone/CodenameOne/pull/5554) adds `com.codename1.home`, a portable model for listing accessories, reading and writing traits, watching changes, running scenes, and commissioning Matter devices.

The difficult part is not naming the common methods. It is preserving the cases where the platforms cannot give the same answer. This post follows the [portable API theme in this week's release overview](/blog/sqlite-portable-encrypted/).

## Traits describe the device, not the platform

A `HomeStructure` contains rooms and accessories. Each accessory contains services, and each service exposes traits such as `ON_OFF`, `BRIGHTNESS`, or `TARGET_TEMPERATURE`.

{{< mermaid >}}
flowchart LR
    A[Application<br/>Trait.BRIGHTNESS] --> B[com.codename1.home]
    B --> C[HomeKit<br/>HMCharacteristicTypeBrightness]
    B --> D[Matter<br/>Level Control CurrentLevel]
    B --> E[Simulator<br/>scripted accessory graph]
    C --> F[Canonical TraitValue]
    D --> F
    E --> F
{{< /mermaid >}}

The port owns conversions that are easy to get wrong. Matter brightness uses a 0 to 254 level. Covering position runs in the opposite direction on some backends. Matter has no single thermostat setpoint in automatic mode. Air quality has a different number of levels in HomeKit and Matter.

`TraitValue` exposes the canonical value and retains the platform ordinal where a conversion loses detail. Proportional values become percentages. Temperature getters require the expected unit, preventing a Celsius value from being read as Fahrenheit by accident.

## Availability has more than two states

`SmartHome.getInstance()` never returns `null`. Unsupported ports return a fallback that reports `NOT_SUPPORTED`. Supported ports use more specific states so an empty accessory graph is not mistaken for a configured home with no devices.

```java
SmartHome home = SmartHome.getInstance();
home.refresh().onResult((structures, error) -> {
    HomeAvailability availability = home.getAvailability();

    if (availability == HomeAvailability.PROVIDER_NOT_INSTALLED
            || availability == HomeAvailability.PROVIDER_UPDATE_REQUIRED) {
        home.openProviderSetup();
        return;
    }
    if (availability == HomeAvailability.PERMISSION_REQUIRED) {
        home.requestAuthorization();
        return;
    }
    if (availability == HomeAvailability.PERMISSION_DENIED) {
        home.openHomeSettings();
        return;
    }
    if (availability == HomeAvailability.NOT_CONFIGURED) {
        home.openEcosystemApp();
        return;
    }
    if (error != null) {
        Log.e(error);
        return;
    }
    if (availability == HomeAvailability.COMMISSIONING_ONLY) {
        showAddDeviceOnlyUI();
    }
});
```

Read availability from the completion path even when `refresh()` fails. On iOS, the initial value is `NOT_STARTED` because connecting to HomeKit is what reveals the authorization and home state.

`COMMISSIONING_ONLY` is the normal Android result without Google Home developer setup. Play services can add a Matter accessory to the user's Google Home. Reading or controlling the accessory graph requires Google Home APIs, a Google Cloud project, and a Home Developer Console registration containing the application's signing-key SHA-1. Codename One cannot create those credentials for an application.

The Google Home accessory graph is not part of this release. Reporting full availability would make the same enum value mean different things on Android and iOS.

## Reads and writes can partly succeed

A batch read returns one `TraitReading` per requested value. A reading can contain a value, contain an error, or contain neither. The third state is valid when a sensor has not measured yet or a light in white mode has no meaningful hue.

A batch write follows the same rule. Turning off every light can succeed for three accessories and fail for one unreachable bulb. The result carries each row instead of collapsing the operation into one boolean.

```java
TraitReadRequest request = new TraitReadRequest()
        .add(thermostat, thermostatService, Trait.CURRENT_TEMPERATURE)
        .add(thermostat, thermostatService, Trait.CURRENT_HUMIDITY);

home.read(request).onResult((readings, error) -> {
        if (error != null) {
            Log.e(error);
            return;
        }
        for (TraitReading reading : readings) {
            if (reading.isFailed()) {
                showUnavailable(reading.getTrait());
            } else if (!reading.hasValue()) {
                showNoReadingYet(reading.getTrait());
            } else {
                show(reading.getTrait(), reading.getValue());
            }
        }
    });
```

Build controls from each service's `TraitConstraint`. A dimmer with a ten-percent floor reports that floor. Writing below it is refused rather than silently clamped.

## Changes are state updates, not an event log

HomeKit can push trait changes while the application is in the foreground. Other backends require polling. `TraitSubscription.isPushDelivery()` tells the application which model it received.

Where push delivery is unavailable, call `drainChanges()` when the application returns to the foreground. Changes are coalesced by accessory, service, and trait. Dragging a dimmer can therefore produce one update with the final value instead of forty intermediate events.

Nothing in this release wakes a stopped application for an accessory change. The home hub owns background automation. The phone application owns a foreground view of current state.

## Commissioning belongs to the operating system

Both mobile ports hand Matter commissioning to a system-owned flow. The user may need to power on the accessory, hold a physical button, scan a label, and join Wi-Fi. The application receives no determinate progress to display.

```java
SmartHome home = SmartHome.getInstance();
Commissioner commissioner = home.getCommissioner();
SetupPayload payload = SetupPayload.parse(scannedCode);

commissioner.commission(new CommissioningRequest()
        .setSetupPayload(payload)
        .setSuggestedName("Kettle"))
    .onResult((result, error) -> {
        if (error != null) {
            Log.e(error);
            return;
        }
        if (home.getAvailability() == HomeAvailability.COMMISSIONING_ONLY) {
            showAddedToHome(result.getAccessoryName());
            return;
        }

        home.refresh().onResult((structures, refreshError) -> {
            if (refreshError != null) {
                Log.e(refreshError);
                return;
            }
            showUpdatedHome(structures, result.getAccessoryId());
        });
    });
```

Success can mean the device joined the user's home without becoming addressable by the application. That is the normal result on an Android build with commissioning alone. On graph-capable builds, refresh after every successful flow. The default iOS Matter sheet does not return an accessory ID even though the new device appears in HomeKit, so the application must inspect the refreshed graph. `wasCommissionedToThisApp()` is useful when a backend returns a directly addressable ID, but it is not a substitute for that refresh.

The iOS build adds HomeKit entitlements only when application code touches accessories. Commissioning lives in its own package because it adds a generated app-extension target. An application that never references `com.codename1.home` gets no framework, entitlement, Play services dependency, or extension.

## The simulated house contains the awkward cases

The simulator, desktop ports, and JavaScript expose a local house with a two-gang switch, bridged lights, an unreachable socket, a thermostat in automatic mode, and a dimmer with a nonzero floor.

Those cases make the application handle missing values, partial failures, constraints, and delayed callbacks before it meets real hardware. The simulator does not push changes and never completes an operation inline, which prevents desktop-only timing assumptions from becoming device bugs.

Automations, triggers, background events, topology writes, camera streams, alarm panels, and Matter events remain outside this release. By default Codename One also commissions through the Apple Home or Google Home ecosystem rather than becoming a Matter controller with its own fabric.

This API expands Codename One onto another native surface without making the application carry platform cluster IDs or characteristic strings. The common model is useful because its capability queries preserve the differences that product code must handle.

The {{< post-link path="/blog/tapjacking-protection" text="next post adds tapjacking protection to the security work" >}}.

---

## Discussion

_Which smart-home difference belongs in a common trait, and which one should stay visible to application code?_

{{< giscus >}}
