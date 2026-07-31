---
title: "Health Data Without Fake Certainty"
slug: health-api-false-certainty
url: /blog/health-api-false-certainty/
date: '2026-08-01'
author: Shai Almog
description: "A deep look at Codename One's HealthKit, Health Connect, workout, nutrition, Bluetooth sensor, and simulator API, including the privacy rules that shape its unusual design."
feed_html: '<img src="https://www.codenameone.com/blog/health-api-false-certainty.jpg" alt="Health data moving through explicit permissions, stores, sensors, and workouts" /> A deep look at Codename One''s HealthKit, Health Connect, workout, nutrition, Bluetooth sensor, and simulator API, including the privacy rules that shape its unusual design.'
series: ["release-2026-07-31"]
---

![Health data moving through explicit permissions, stores, sensors, and workouts](/blog/health-api-false-certainty.jpg)

The hardest part of a health API is not reading a heart-rate number. It is knowing what that number means, which store supplied it, whether anything is missing, and what the application is legally allowed to do next.

[PR #5475](https://github.com/codenameone/CodenameOne/pull/5475) is in review with a cross-platform health API for HealthKit, Health Connect, recorded workouts, nutrition, Bluetooth health sensors, and deterministic simulation. This article describes the reviewed API contract. The PR has compile, link, and automated coverage, but device runtime validation is still part of the release work.

This is part of the [July 31 weekly release series](/blog/push-v3-new-cloud/), led by Push V3 and the new cloud migration.

## The API has four public layers

The packages are divided by the boundary they touch:

| Package | Responsibility |
| --- | --- |
| `com.codename1.health` | Health stores, permissions, samples, units, queries, aggregates, sources, change subscriptions, and errors |
| `com.codename1.health.workout` | Recorded workout sessions, events, configuration, and collected samples |
| `com.codename1.health.sensors` | Live standard Bluetooth health devices, independent of HealthKit or Health Connect |
| `com.codename1.health.nutrition` | Sparse nutrient records and nutrition-specific types |

`Health.getInstance()` is the single entry point and never returns `null`. An unsupported port returns a fallback whose operations fail with `HealthError.NOT_SUPPORTED`. Application code branches on capabilities instead of operating-system names.

```java
Health health = Health.getInstance();
if (health.getAvailability() != HealthAvailability.AVAILABLE) {
    health.openProviderSetup();
    return;
}

HealthStore store = health.getStore();
store.requestAuthorization(
        HealthAccess.read(HealthDataType.STEPS),
        HealthAccess.read(HealthDataType.HEART_RATE)
).onResult((asked, err) -> {
    if (err != null) {
        Log.e(err);
        return;
    }
    // asked means the sheet completed. It does not mean read access was granted.
});
```

Every result, sensor sample, workout event, and change batch arrives on the Codename One EDT. That rule is the same on a phone, in the simulator, and in a local desktop store.

{{< mermaid >}}
flowchart TB
    A["Application"] --> B["Health"]
    B --> C["HealthStore"]
    B --> D["WorkoutManager"]
    B --> E["HealthSensors"]
    C --> F["HealthKit on Apple platforms"]
    C --> G["Health Connect on Android"]
    C --> H["Local and simulated store"]
    E --> I["Bluetooth LE sensor profiles"]
    I --> D
    I --> C
{{< /mermaid >}}

## The strange permission answer preserves privacy

HealthKit does not tell an application whether the user denied read access. A denied read returns no data, which is indistinguishable from an empty store. That prevents an application from learning that a user chose to conceal a sensitive category.

This produces three rules that can look odd until the privacy boundary is understood:

1. A successful `requestAuthorization(...)` means the user was asked.
2. `getReadAuthorizationStatus(...)` remains `UNKNOWN` on iOS.
3. There is no `hasReadPermission()` method.

Android can report its runtime grant state, so the Android implementation answers that question. The shared API does not force HealthKit to tell a lie for symmetry.

Do not convert an empty read into “you denied access.” The accurate UI is “no data available,” with an option to open health settings. The simulator has a specific **Grant Write, Deny Read Without Error** mode because the happy path will not expose this mistake.

## Samples keep their units and identity

The core model covers quantity, category, series, session, sleep, blood pressure, workout, and nutrition samples. A `HealthQuantity` has no zero-argument `getValue()`. The caller must name a unit:

```java
double kilograms = sample.getQuantity().getValue(HealthUnit.KILOGRAM);
```

That extra word prevents a pound value from silently becoming kilograms in a chart or server payload.

Queries default to flattening Android heart-rate series into individual `QuantitySample` values so iOS and Android present the same common shape. Set `flattenSeries` to `false` when record identity matters, such as deletion. HealthKit has no equivalent series record, so it continues to return scalar samples.

Sample identifiers belong to the platform and the installation. They are not stable server primary keys. Arbitrary metadata currently round-trips through the local and simulator stores, but not through HealthKit or Health Connect. Keep application correlation identifiers in application storage.

High-frequency data must be paged. A year of frequent heart-rate samples can approach half a million values, so an unbounded read would be a memory bug disguised as convenience. The default cap is 10,000, and `readSamplePage(...)` exposes continuation.

## An absent value is not zero

An aggregate bucket with no samples returns `null`, never zero. No reading and a measured value of zero are different facts.

Time intervals carry a similar distinction. If a chart is labeled with dates, use calendar intervals with an explicit time zone:

```java
HealthInterval day = HealthInterval.calendarDays(1, userTimeZone);
```

A fixed 86,400,000 milliseconds is not a local day across daylight-saving changes. Reading the JVM default would also make a server or simulator silently file an evening walk under the wrong date.

There is one limitation developers need to see clearly: this release computes aggregates from shared raw samples. It does not use HealthKit's source-deduplicating statistics engine. If a phone and watch both record the same walk, the total can count both sources. Filter with `addSource(...)` and tell the user which source a number represents.

## Change subscriptions are a cursor, not a wake-up service

`HealthSubscription` persists a cursor and restores it across launches. Reuse a stable ID such as `steps-v1`, then call `drainChanges()` when the application enters the foreground and from background fetch.

No supported platform wakes a closed application for health changes in this release. `isPushDelivery()` returns `false` everywhere.

{{< mermaid >}}
sequenceDiagram
    participant OS as Health store
    participant App as Application lifecycle
    participant Sub as HealthSubscription
    OS->>OS: New or changed sample
    App->>Sub: drainChanges()
    Sub->>OS: Read after persisted cursor
    OS-->>Sub: Change batch
    Sub-->>App: Listener on EDT
    App->>Sub: Listener completes
    Sub->>Sub: Advance cursor
{{< /mermaid >}}

Android uses Health Connect change tokens. It can report additions and deletions without replaying an already consumed change. A token can expire, in which case the batch reports that a full resynchronization is required.

iOS currently uses a timestamp window. It reports additions, not deletions. A backdated reading or a watch sample that syncs after the cursor has advanced can fall behind that window. Applications that require completeness must run a periodic full range query and treat the subscription as a refresh hint. A future anchored HealthKit query can close this gap without changing the public capability model.

The local and simulator stores persist subscription registration, but they do not synthesize store mutations as change events. Test phone delivery on a phone.

## Workouts are recorded, not magically live

`WorkoutManager` can create a recorded workout session and accept samples supplied by the application or an attached sensor. `isLiveSessionSupported()` and `isSensorCollectionSupported()` are both false in this release.

That distinction matters. The framework does not claim that `HKWorkoutSession` or Wear OS Health Services is keeping the process alive. A recorded session lasts as long as the application process and stores only what the application feeds it.

```java
WorkoutConfiguration config = new WorkoutConfiguration()
        .setActivityType(WorkoutActivityType.CYCLING)
        .setLocationType(WorkoutLocationType.OUTDOOR);

Health.getInstance().getWorkouts().startSession(config)
        .onResult((session, err) -> {
            if (err != null) {
                Log.e(err);
                return;
            }
            session.start();
        });
```

When a workout ends, its result reports what a platform would not persist. Some sensor values have no single-value Health Connect record, and this API does not pretend otherwise. The complete recorded workout remains available to the application for its own chosen storage or upload path.

## Eight standard sensor profiles, without the health store

`com.codename1.health.sensors` covers the adopted Bluetooth SIG profiles for:

- Heart rate
- Cycling power
- Cycling speed and cadence
- Running speed and cadence
- Health thermometer
- Weight scale
- Blood pressure
- Glucose

This layer is built on `com.codename1.bluetooth.le`. An application can connect to a standard strap, scale, cuff, or meter without touching HealthKit or Health Connect. Sensor-only use therefore needs Bluetooth permissions, not health-store entitlements or a Play health permission review.

The implementation handles the unglamorous details that usually break field code: cumulative counter differences, timer rollover, IEEE 11073 floating-point values, reconnection, and GATT parsing.

Writing sensor measurements to the health store is off by default. A strap and the operating system might both record the same heart rate during a workout, and automatic write-through would double-count it. Attach the sensor to the recorded workout, or enable store writes only when that is truly the desired source.

Several limits are explicit:

- RR intervals are decoded, but not emitted as stored HRV samples.
- Live blood-pressure readings work, but phone-store persistence is not implemented.
- Stored glucose-record replay is not implemented.
- Sensor use on watchOS is more limited than phone and desktop support.

An explicit `TYPE_NOT_SUPPORTED` is less pleasant than a green check mark, but far safer than silently dropping a medical measurement.

## Nutrition is sparse by design

Food contains some measured nutrients, not forty meaningful zeroes. `NutritionSample` therefore stores a sparse nutrient map. An absent sodium value remains `null`.

The complete nutrition record currently works in local and simulator stores. On phones, hydration is available on both platforms and dietary energy is available on iOS as ordinary quantity samples. Multi-nutrient phone records are rejected until their native mappings exist.

Sleep follows a similar rule. The model and local implementation are present, but phone reads are not supported in this release. A phone query fails with `TYPE_NOT_SUPPORTED` rather than returning an empty list that could be mistaken for a successful read.

## Build configuration is intentionally strict

An iOS application that reads the store must provide a specific purpose:

```properties
ios.NSHealthShareUsageDescription=Reads step count to show weekly activity trends
ios.NSHealthUpdateUsageDescription=Saves workouts you choose to record
```

The HealthKit capability must also be enabled for the App ID and included in the provisioning profile.

Android requires an explicit type list and public privacy-policy URL:

```properties
android.health.read=STEPS,HEART_RATE
android.health.write=STEPS
android.health.privacyPolicyUrl=https://example.com/privacy
```

The build will not insert a generic purpose string. Apple reviews whether that text matches what the application does, and Google Play requires narrow permission declarations. A placeholder would defer a clear build failure into a confusing review rejection.

Health Connect also raises the Android minimum SDK to 26 and requires a target SDK of at least 30.

## HIPAA is not a framework switch

This API does not make an application HIPAA compliant.

HIPAA applies to covered entities and business associates, not to every application containing health data. The relationship and data flow determine the obligation. HHS explicitly notes that an app can fall outside HIPAA after receiving data at an individual's direction, while an app acting for a covered entity may be a business associate. Read the [HHS mobile health app guidance](https://www.hhs.gov/hipaa/for-professionals/special-topics/health-apps/index.html) and get advice for the specific product and market.

Being outside HIPAA does not mean being outside health privacy law. The FTC's [Health Breach Notification Rule guidance](https://www.ftc.gov/business-guidance/resources/complying-ftcs-health-breach-notification-rule-0) explains its reach into many health apps, connected devices, and related services that are not HIPAA covered.

If a cloud service creates, receives, maintains, or transmits electronic protected health information for a covered entity or business associate, [HHS cloud guidance](https://www.hhs.gov/hipaa/for-professionals/special-topics/health-information-technology/cloud-computing/index.html) says a business associate agreement may be required even when the provider cannot decrypt the data.

The framework can enforce some useful boundaries:

- It never uploads health data.
- The simulator logs types and counts, not sample values.
- Synthetic datasets contain no person's history.
- It refuses missing purpose strings and unsupported writes.

Your application still owns access control, encryption, audit records, retention, account deletion, breach handling, backend contracts, minimum-necessary collection, consent, and store disclosures. Do not place health samples in unencrypted `Storage`.

Google Play separately requires a health apps declaration and privacy disclosures for health features. The current [Health Content and Services policy](https://support.google.com/googleplay/android-developer/answer/16679511) treats Health Connect data as sensitive user data. Apple has its own HealthKit privacy and review requirements. Compliance is an application architecture, not a checkbox in a cross-platform API.

## The simulator tests what a permissive fake cannot

The simulator includes a deterministic seven-day dataset, HealthKit and Health Connect permission modes, provider availability controls, one-shot failures, and the silent read-denial case.

The release test I care about most is:

1. Select **Grant Write, Deny Read Without Error**.
2. Request authorization.
3. Read a type that has data in the synthetic dataset.
4. Confirm the UI says “no data available.”
5. Confirm it does not accuse the user or show a technical error.

That scenario explains why parts of this API look so cautious. Health data punishes false certainty.

Next in the series: [on-device AI and MCP over a guarded loopback transport](/blog/on-device-ai-mcp-loopback/).

---

## Discussion

_Which health-store or sensor path do you need us to validate on real hardware first?_

{{< giscus >}}
