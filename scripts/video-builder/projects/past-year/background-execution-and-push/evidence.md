# Evidence map

Source: `docs/website/content/blog/background-execution-and-push.md`
Canonical: https://www.codenameone.com/blog/background-execution-and-push/

## Thesis

Constraint-based background work with richer notifications and simulator coverage

## Supported beats

- **Background work with constraints:** The new com.codename1.background package schedules work that the OS runs when its conditions are met, mapping to Android JobScheduler and iOS BGTaskScheduler underneath. You describe what the work needs, not when to poll.
- **Notifications got a lot richer:** LocalNotification gained a long list of capabilities, all added backward-compatibly so every existing field, getter, and setter behaves exactly as before. New on top of that: an image attachment, multiple action buttons, inline quick reply, per-channel sound, grouping with a summary, full-screen intent, time-sensitive delivery, ongoing and progress notifications, a custom view, and a messaging-conversation style.
- **Push topics:** On Android these map to FCM topics. On iOS they are a documented no-op, because raw APNs has no topic concept; the call is safe to make on both so your code stays cross-platform.
- **Receiving shared content:** On Android this is backed by a share-receiver activity that handles SEND and SEND_MULTIPLE and hands files off through app storage. On iOS it reuses the share extension that landed two weeks ago and reads the App-Group payload when the app activates; the App Group is configured with the ios.shareAppGroup build hint.
- **All of it runs in the simulator:** The piece that makes this practical day to day is the JavaSE support.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5142
