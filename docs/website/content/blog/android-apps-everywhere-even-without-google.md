---
title: "Your Android App on iPhone, and on Phones Without Google"
slug: android-apps-everywhere-even-without-google
url: /blog/android-apps-everywhere-even-without-google/
date: '2026-11-04'
author: Shai Almog
description: "The Android compatibility layer now runs Room, WorkManager, location, sensors, foreground services and document picker intents on iOS, desktop and the web. And Codename One apps run on de-Googled Android such as GrapheneOS with no build hints, with push through UnifiedPush."
feed_html: "<img src=\"https://www.codenameone.com/blog/android-apps-everywhere-even-without-google.jpg\" alt=\"An Android app running on Android and on Codename One side by side\" /> The Android layer now runs Room, WorkManager, location and document pickers on iOS and desktop, and Codename One apps run on GrapheneOS with no build hints."
series: ["release-2026-10-30"]
---

![An Android app running on Android and on Codename One side by side](/blog/android-apps-everywhere-even-without-google.jpg)

This post is about Android in two directions. The first is taking an existing Android app, written with activities, XML layouts and AndroidX, and running it on iPhone, desktop and the web. The second is running Codename One apps on Android phones that don't have Google at all.

## Classic Android apps, everywhere

[Three weeks ago](/blog/android-apps-beyond-android/) we showed that an Android Studio module's `src/main` can build unmodified as a Codename One app. There's no Android runtime inside it. Views are Codename One components, layouts follow Android's measure and layout rules, and resources compile to a binary table at build time. Nothing parses XML or uses reflection on the device. You import a project with one command:

```bash
mvn cn1:import-android-project -Dcn1.android.import=/path/to/MyAndroidApp
mvn cn1:build -Dcodename1.platform=ios
```

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,200px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/android-interop-material.png"><img width="736" height="800" src="/developer-guide/img/android-interop-material.png" alt="A Material 3 screen on Android and on Codename One" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Material 3.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/android-interop-recycler.png"><img width="736" height="800" src="/developer-guide/img/android-interop-recycler.png" alt="A RecyclerView on Android and on Codename One" loading="lazy" style="width:100%;height:auto" /></a><figcaption>RecyclerView.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/android-interop-kotlin.png"><img width="736" height="800" src="/developer-guide/img/android-interop-kotlin.png" alt="A Kotlin activity on Android and on Codename One" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Kotlin activities.</figcaption></figure>
</div>
{{< /guide-block >}}

Each pair shows Android on the left and Codename One on the right, from the same source.

### What's new: the parts real apps lean on

Toy apps are mostly UI. Real apps store data, run work in the background and talk to the hardware. The layer now covers those:

- **Room.** The build generates the `_Impl` classes, the way Android's annotation processor does. Features we don't support yet (`@Relation`, `suspend` DAOs, Flow, Rx, Paging, `@AutoMigration` and FTS) fail the build and name the member.
- **WorkManager.** Chains, unique work, retries and LiveData status, persisted across app restarts.
- **Location and sensors.** `LocationManager` and `SensorManager`.
- **Services.** Manifest services, including `startForeground()` mapped to Codename One's `ForegroundService`. Notifications from `NotificationManager` and `NotificationCompat` show as local notifications.
- **Keystore.** AES keys from `AndroidKeyStore` with GCM and CBC.
- **Document pickers.** `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT` and `ACTION_GET_CONTENT`, plus the ActivityResult contracts, open the platform's native open and save dialogs.

So this is ordinary Android code, and it now runs on an iPhone:

```java
@Dao
public interface NoteDao {
    @Query("SELECT * FROM Note ORDER BY created DESC")
    List<Note> all();

    @Insert
    long insert(Note note);
}

Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
save.addCategory(Intent.CATEGORY_OPENABLE);
save.setType("text/plain");
save.putExtra(Intent.EXTRA_TITLE, "notes.txt");
startActivityForResult(save, SAVE_NOTES);
```

### Where iOS says no

Some Android behavior has no iOS equivalent, and the [Android chapter](/developer-guide/android-interop/) has a table for it. Location in a foreground service keeps running in the background when the manifest asks for background location. Other foreground service work gets only the short window iOS allows. Constrained WorkManager jobs go to iOS's background task scheduler, and iOS decides when they run. On iOS, `ACTION_CREATE_DOCUMENT` returns `RESULT_OK` right away and shows the save dialog when your app closes the output stream, so a cancel isn't reported back.

Other limits: Keystore keys aren't hardware backed there, `Parcel` is an in process container, SQLite uses a single connection, there are no system content providers or broadcasts, and reflection based libraries won't run on iOS.

## Android without Google

I was a member of Team OS/2. I know what it's like to use an operating system the big vendors forgot, and to find that half the apps you need just don't start. People choose GrapheneOS, LineageOS and other AOSP builds for good reasons, usually privacy, and they deserve apps that work.

Codename One apps now run on them out of the box. One APK serves both kinds of phone: at run time it checks whether Google Play Services is there. No build hint, no flavor, no second build. The Android port never calls Play services, Play Core or Firebase without checking first, and CI enforces that, so a future change can't quietly break it.

| Feature | With Google Play | Without Google Play |
| --- | --- | --- |
| UI, storage, networking, SQLite, media | Works | Works |
| On device ML Kit models | Works | Works |
| Location and geofences | Fused location | Framework `LocationManager` |
| Maps | Google Maps | Vector `MapView` |
| Google sign in | Native | Browser OAuth flow |
| Push | Firebase | UnifiedPush (ntfy, NextPush, Sunup) |
| In-app purchase | Play Billing | Not available |
| Integrity attestation, in-app review | Works | Not available |

The location fallback fixed something on Google phones too: builds without Play location now get proximity alerts and background location through the framework API.

### Push, with one line

Push needs one line of configuration, because the phone has no Firebase to deliver it. UnifiedPush lets the user pick the app that holds the push connection, called a distributor. A UnifiedPush endpoint is a standard Web Push subscription, so the Codename One push service delivers to it with the Web Push credential your app already has. You copy its public key into a build hint:

```properties
codename1.arg.android.unifiedPush.vapidKey=<your 87 character VAPID public key>
```

Your app code doesn't change, and neither does your server. The registration order is automatic:

{{< mermaid >}}
flowchart TD
    S[PushClient registers] --> G{Google Play Services?}
    G -->|yes| F[Firebase Cloud Messaging]
    G -->|no| H{Huawei HMS?}
    H -->|yes| K[Huawei Push Kit]
    H -->|no| U{UnifiedPush distributor installed?}
    U -->|yes| P[UnifiedPush, delivered as Web Push]
    U -->|no| E[pushRegistrationError: a push service is needed]
{{< /mermaid >}}

The one real gap is in-app purchase. Play Billing needs the Play Store. If selling on de-Googled phones matters to you, tell me; it's something we can add.

To test, use the Android emulator's AOSP system images, the `default` target, which have no Google apps. The [Android without Google Play chapter](/developer-guide/android-without-google-play/) has the details.

Tomorrow, the last post of the week: {{< post-link path="/blog/backend-caches-certificates-and-a-simulator-that-types" text="Caches, Certificates, Streaming Uploads and a Simulator That Types" >}}.

{{< giscus >}}
