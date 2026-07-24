---
title: "Calendar API: Local Calendars, Cloud Sync, and Conflict Handling"
slug: calendar-is-not-add-event
url: /blog/calendar-is-not-add-event/
date: '2026-07-25'
author: Shai Almog
description: "The new com.codename1.calendar API covers Android and Apple calendars, Google, Microsoft, CalDAV, recurrence, tasks, conflict handling, incremental sync, and offline mutation queues."
feed_html: '<img src="https://www.codenameone.com/blog/calendar-is-not-add-event.jpg" alt="Calendar API with local and cloud synchronization" /> The new calendar API handles local calendars, cloud providers, recurrence, conflicts, and offline changes through one capability-based model.'
series: ["release-2026-07-24"]
---

![Calendar API: Local Calendars, Cloud Sync, and Conflict Handling](/blog/calendar-is-not-add-event.jpg)

[PR #5413](https://github.com/codenameone/CodenameOne/pull/5413) adds `com.codename1.calendar`, a cross-platform API for local calendars and cloud providers. It covers events, tasks, recurrence, incremental sync, offline changes, and conflict handling.

A request to “add this event to the calendar” often grows into editing, recurrence, provider synchronization, and conflict resolution. The new API handles those requirements through one data model instead of a platform-specific intent.

## Events, recurrence, and tasks

An event can be timed or all-day. It can recur, invite attendees, carry alarms, link a conference, and belong to a provider with its own identifier and version.

```java
CalendarEvent event = new CalendarEvent()
        .setCalendarId(calendarId)
        .setTitle("Architecture review")
        .setStart(CalendarDateTime.instant(
                start, ZoneId.of("Europe/Paris")))
        .setEnd(CalendarDateTime.instant(
                end, ZoneId.of("Europe/Paris")))
        .setRecurrence(new CalendarRecurrenceRule()
                .setFrequency(CalendarRecurrenceRule.Frequency.WEEKLY)
                .addDayOfWeek(2))
        .addAttendee(new CalendarAttendee()
                .setName("Ari")
                .setEmail("ari@example.com"))
        .addAlarm(new CalendarAlarm()
                .setTimeBefore(Duration.ofMinutes(15)));

local.saveEvent(event, CalendarMutationScope.ALL)
        .ready(saved -> Log.p("Created " + saved.getId()));
```

The API uses `java.time`. Timed values carry a zone. All-day values use a date with no invented midnight. That distinction prevents an all-day event from moving to the previous day when it crosses a time-zone boundary.

Tasks use the same source model without pretending they are events. Google Calendar and Google Tasks share one provider class. Microsoft calendars and Microsoft To Do share another.

## Ask about capabilities, not platforms

`LocalCalendarSource` maps to Android's Calendar Provider and Apple EventKit on iOS and Mac Catalyst. The simulator gives tests an isolated in-memory calendar. It never writes to the developer's real calendar.

Online providers work anywhere the HTTP and OAuth layers work, including JavaScript and Linux:

{{< mermaid >}}
flowchart TD
    A["Application calendar model"] --> B["CalendarSource capability contract"]
    B --> C["Selected source: Android, Apple, simulator, Google, Microsoft, CalDAV, or RFC 5545"]
    C --> D["Capability queries expose supported operations"]
    D --> E["Change listeners, paging, provider versions, and delta tokens"]
{{< /mermaid >}}

Do not branch on `Display.getPlatformName()`. Ask the source:

```java
LocalCalendarSource local = LocalCalendarSource.getInstance();
CalendarCapabilities capabilities = local.getCapabilities();

if (capabilities.supports(CalendarCapability.READ_EVENTS)) {
    local.requestAuthorization(CalendarAccess.EVENTS_READ_ONLY)
            .ready(status -> {
        if (status == CalendarAuthorizationStatus.FULL) {
            local.queryEvents(new CalendarQuery()
                    .setCalendarId("primary")
                    .setStartTime(Instant.now()))
                 .ready(page -> page.getItems()
                         .forEach(System.out::println));
        }
    });
}
```

Windows, native macOS desktop, Linux, and JavaScript currently report no built-in local calendar. They can still use Google, Microsoft, CalDAV, and `.ics`. An empty capability set is more useful than a method that compiles and fails after deployment.

## A version token prevents silent overwrites

The `version` on a returned event is an optimistic concurrency token. Save the returned object when editing it. If somebody changed the provider copy after your read, the save returns `CalendarError.CONFLICT` instead of overwriting the newer change.

Offline mutation storage is opt-in:

```java
CalendarSyncEngine sync = new CalendarSyncEngine(
        google, new StorageCalendarCache("google-account-1"));

sync.queueEventSave(event, CalendarMutationScope.ALL);
sync.sync().ready(result -> {
    for (CalendarConflict conflict : result.getConflicts()) {
        // Show local and remote versions, then choose
        // KEEP_LOCAL, KEEP_REMOTE, or MERGED.
    }
});
```

The cache stores calendar data and pending mutations. It does not store OAuth credentials. Your application decides when background work runs and how a person resolves a conflict.

{{< mermaid >}}
flowchart TB
    A["Application queues an edit with version 17"] --> B["Sync sends the pending edit to the provider"]
    B --> C["Provider reports a conflict with remote version 18"]
    C --> D["Application chooses local, remote, or merged content"]
    D --> E["Sync saves the resolved version"]
    E --> F["Local cache records the new version and delta token"]
{{< /mermaid >}}

Creating the event is one operation. The version and sync APIs preserve the user's changes when another device or provider edits the same record.

## Provider sync is pull based

Local stores can emit `CalendarChange` callbacks. Online providers return sync or delta tokens. Store the token and pass it with the next query.

The API deliberately leaves Google and Microsoft webhooks to the application backend. Provider webhooks require a public endpoint, secret handling, renewal, and delivery policy that a client API cannot supply for every application.

Google and Microsoft OAuth credentials also belong to your application. The provider classes never embed a shared Codename One client secret and never persist credentials on your behalf.

CalDAV supports Basic, Bearer, and Digest authentication. Basic should only travel over HTTPS. Server implementations differ. Each asynchronous result can still report a collection-specific restriction.

## Import and export use the same model

`ICalendarCodec` reads and writes RFC 5545 events, tasks, alarms, recurrence, attendees, time zones, URI attachments, and unknown `X-` properties:

```java
String ics = ICalendarCodec.writeEvent(event);
CalendarEvent imported = ICalendarCodec.readEvent(ics);
```

The codec supports email attachments, backups, provider interop, and ports with no local calendar service. Imported objects enter the same save, query, and conflict flow as objects created through the Java API.

## Unused applications carry none of it

The builders detect calls to the local calendar entry points. Android then adds calendar permissions. Apple builds add EventKit and the relevant privacy strings.

An application that never references local calendar integration gets no permission or entitlement change. Online providers do not require device-calendar permissions.

The portability boundary remains explicit:

- Local change notifications depend on the operating system.
- Online notification is token-based polling, not an installed webhook.
- OAuth setup and credential storage remain application responsibilities.
- Conflict resolution requires a product decision. The framework cannot decide which edit matters.

Use the platform intent when your requirement ends after creating one event. Use `CalendarSource` when your application must read, update, synchronize, or resolve conflicts.

Tomorrow's post covers {{< post-link path="/blog/bluetooth-beyond-ble" text="Bluetooth support across every Codename One target" >}}.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
