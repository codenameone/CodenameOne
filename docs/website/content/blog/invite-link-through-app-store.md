---
title: "The Hard Part of Invite a Friend Is the Install"
slug: invite-link-through-app-store
url: /blog/invite-link-through-app-store/
date: '2026-09-21'
author: Shai Almog
description: "Follow an invitation from a share sheet through Android installation or an iOS App Clip, then connect exact attribution to Codename One analytics."
feed_html: '<img src="https://www.codenameone.com/blog/invite-link-through-app-store.jpg" alt="An invitation that survives installation" /> Carry an invitation code through installation with Android referrers and an iOS App Clip.'
series: ["release-2026-09-18"]
---

![An invitation that survives installation](/blog/invite-link-through-app-store.jpg)

“Can we add an invitation button?” is one of those requests that sounds like an afternoon. Then you discover that the person receiving the link doesn't have your app installed, and the next program in the conversation is an app store.

There are companies whose whole product lives in that gap. [This week's release](/blog/why-another-java-server/) puts the invitation flow into Codename One, including exact attribution and integration with the existing analytics stack.

The difficult part is getting the same code through every handoff. A link moves from your app to a messaging app, then to a browser or an installed application. If installation intervenes, the code has to survive that too. It's a volleyball rally where the players keep changing courts.

## Create the invitation where the user is

Use `com.codename1.analytics.invite`:

```java
Invite invite = Invites.create(InviteRequest.create()
        .campaign("team-launch")
        .channel("share_sheet")
        .title("Join our team")
        .description("Keep the team's work in one place.")
        .build());

Invites.share(invite, "Come and try this with me");
```

Creation returns immediately, including offline. The app generates the code locally and registers it with the link service in the background. The share sheet doesn't wait for that request to complete.

The analytics distinction matters: opening the sheet isn't a completed share. The flow reports `invite_shared` when the platform confirms sharing, and `invite_share_dismissed` for dismissal. If your own UI sends the invitation, report the result through `Invites.reportShareResult()` from the share callback.

## Three roads to the same code

{{< mermaid >}}
flowchart TD
    Link[Recipient opens invitation link] --> Installed{App installed?}
    Installed -->|Yes| Direct[App receives code directly]
    Installed -->|No, Android| Play[Play Store install referrer]
    Installed -->|No, iOS| Clip[Generated App Clip receives code]
    Play --> Android[Installed app reads referrer]
    Clip --> Group[Code saved in App Group]
    Group --> iOS[Installed app reads handoff]
    Direct --> Claim[Claim exact invitation code]
    Android --> Claim
    iOS --> Claim
    Claim --> Consent[Consent-gated analytics attribution]
{{< /mermaid >}}

Android has an install-referrer path. The invitation code passes through Play and returns to the installed app.

The App Store doesn't provide the equivalent parameter. On iOS, the generated App Clip receives the invitation URL, stores its code in the shared App Group, and offers the full app. After installation, the app reads that handoff. The clip is a small generated UIKit application; you don't maintain a second Codename One UI for it.

The attribution reports how the code arrived:

| Match type | Handoff |
| --- | --- |
| `MATCH_DIRECT` | A link opened an already installed app. |
| `MATCH_REFERRER` | Android returned the code through the install referrer. |
| `MATCH_APP_CLIP` | An iOS App Clip passed the code to the full app. |

All three carry an exact code. No device fingerprint is needed to guess which installation followed a click. Exact attribution identifies the invitation; your server still decides whether an account qualifies for a reward.

## Receive it during application startup

Register a listener and call `checkForInvite()` from the application's `start()` flow:

```java
Invites.setInviteListener(new InviteListener() {
    public void inviteReceived(InviteAttribution attribution) {
        showInvitedWelcome(attribution.getCampaign());
    }

    public void attributionUnavailable(String reason) {
        showOrdinaryWelcome();
    }
});
Invites.checkForInvite();
```

The two `show...` methods are application UI methods. An installation without an invitation is an ordinary result. The API delivers one terminal listener result per install, and retains an early result until a listener is available. That matters on a cold launch, where link processing can finish before your UI registers.

Android can deliver a link through a replacement activity intent; iOS uses its launch-property path. The pull in `start()` gives application code one place to check both.

## Finish the platform configuration

The build adds native wiring when the application references the invitation package: Android App Links and the Play Install Referrer dependency, iOS associated domains, and the generated App Clip.

There are account-side settings the build can't create for you. Complete the invitation setup in the Codename One console and use its link prefix for Apple's App Clip Experience.

For Android, put the installed application's signing SHA-256 in this build hint:

```properties
codename1.arg.android.invite.signingFingerprint=YOUR_APP_SIGNING_SHA256
```

Under Play App Signing, that's the **app-signing certificate** in Play Console. Your upload certificate is different. App Links verification checks the signature of the APK on the device.

For iOS:

```properties
codename1.arg.ios.invite.appStoreId=YOUR_NUMERIC_APP_STORE_ID
```

Enable Associated Domains and the App Clip on your App ID. Register the App Group used by the generated app and clip, and make sure the provisioning profiles include it. The builder derives a group from the package name; `ios.invite.appGroup` lets you select an existing registered group.

Finally, register an **Advanced App Clip Experience** in App Store Connect for the invite prefix shown by the console. Authorizing a domain and mapping its URL to an App Clip are separate steps. Without the experience, the link won't offer the expected clip card.

If you already ship your own App Clip, `ios.invite.appClip=false` disables generation. Your clip then needs to write the expected handoff. The [Analytics guide](https://www.codenameone.com/developer-guide/) covers the build hints and lifecycle in its invitation section.

## Attribute what happened after installation

Once attribution resolves, the analytics context carries `cn1_campaign`, `cn1_channel`, `cn1_invite_code`, and `cn1_invite_match`. Later events inherit those dimensions, including the framework's purchase events.

For another business milestone, report a conversion when it actually happens:

```java
Invites.conversion("first_project_created");
```

That lets you distinguish an invitation that led to an installation from one that led to someone using the product. Avoid counting an explicit purchase conversion a second time if the framework's purchase event already represents the same action.

Collection and reporting follow the analytics consent category. `Analytics.resetClientId()` erases invitation attribution with the identity, so a new client ID doesn't remain linked through the old referral. Setting the attribution window to zero stops deferred lookup while still allowing an exact code already delivered by a link.

## One less platform relay to maintain

[PR #5751](https://github.com/codenameone/CodenameOne/pull/5751) brings the link, installation handoff, lifecycle handling, and analytics context together. The button is the visible part; the platform relay is the work the framework takes off your hands.

Alongside this week's vault and backend work, it gives us another place to make the safer implementation the convenient one. Carry a code instead of a fingerprint. Respect consent before transmission. Erase attribution when the identity is reset. Keep reward authorization on the server. Those decisions should survive the move from one platform to another.

---

## Discussion

_Where has your invitation flow lost people: the share sheet, the store, the first launch, or the first useful action?_

{{< giscus >}}
