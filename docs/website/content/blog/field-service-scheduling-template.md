---
title: "Fieldline: Launch Your Own Field Service App, With a Scheduler That Plans the Week"
slug: field-service-scheduling-template
url: /blog/field-service-scheduling-template/
date: '2026-11-24'
author: Shai Almog
description: "Fieldline is a complete field service app and server you can generate from the Initializr: technicians follow their day on a map, dispatchers drag work on a Gantt chart, an optimizer plans and repairs the week around skills, hours and travel, and an AI assistant re-plans from plain words. MIT-0, iOS, Android, desktop and web."
feed_html: "<img src=\"https://www.codenameone.com/blog/field-service-scheduling-template.jpg\" alt=\"Fieldline field service app\" /> A full field service product: routes on a map, checklists, photos with AI diagnosis, customer signatures, a Gantt dispatch board and a heuristic optimizer that plans the week. Generate it and make it yours."
series: ["release-2026-11-20"]
---

![Fieldline field service app](/blog/field-service-scheduling-template.jpg)

One of my first jobs was at a scheduling company. I wrote heuristic algorithms that assigned work to people under constraints, and drew the result as Gantt charts that planners stared at all day. It's a brutal field. Every customer has a rule nobody told you about, a technician who can't work Tuesdays, a site that only opens at nine, a van that can't carry two jobs' worth of parts. The optimizer that handles all of it is the product, and building one used to take a team several years.

Fieldline is that product as a template. Open the Initializr, pick the field service template, and you get a working app for technicians, dispatchers and administrators, the server behind it, and a scheduling engine that plans their week. The source is MIT-0. Change it, sell it, keep your changes private.

## The technician's day

A technician opens the app on today: the jobs as a numbered route on a map, with the travel time between stops. They drive, check in on site, work through a checklist that saves as they go, and photograph what they find. The assistant looks at the photo and suggests a diagnosis. At the end, the app drafts the report and the customer signs on the screen. The signature is sealed with a hash of the signature, the time and the place, so nobody argues later about whether the job was done.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,180px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/fieldline-today-route.png" alt="Fieldline showing today's jobs as a route on a map" loading="lazy" style="width:100%;height:auto" /><figcaption>Today, as a route.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/fieldline-checklist.png" alt="A job checklist in Fieldline" loading="lazy" style="width:100%;height:auto" /><figcaption>The checklist saves as you go.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/fieldline-assistant.png" alt="The Fieldline assistant diagnosing a photo" loading="lazy" style="width:100%;height:auto" /><figcaption>An expert on call, in the app.</figcaption></figure>
</div>
{{< /guide-block >}}

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/fieldline/fieldline-technician-tablet-poster.jpg" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="A technician working through a job on a tablet"><source src="/fieldline/fieldline-technician-tablet.mp4" type="video/mp4"><a href="/fieldline/fieldline-technician-tablet.mp4">Download the technician video</a>.</video>
{{< /guide-block >}}

## The dispatcher's board

A dispatcher sees the whole crew three ways: a timeline, a Gantt chart against the last plan, and a map. Drag a job to another technician and every constraint is checked before the move is applied. A new job is placed automatically. Someone calls in sick and their absence is covered in one tap. Every optimizer run lists what it changed and why, because a planner who can't see the reasoning will never trust the machine.

And they can just ask. "Dana is out Thursday, cover her jobs" re-plans Thursday. The assistant runs behind the server's own API key, so the key never ships inside the app.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,260px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/fieldline-gantt.png" alt="Fieldline's Gantt chart against the last plan" loading="lazy" style="width:100%;height:auto" /><figcaption>The Gantt chart against the last plan.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/fieldline-optimize.png" alt="An optimizer run listing what it changed" loading="lazy" style="width:100%;height:auto" /><figcaption>Every change, explained.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/fieldline-dispatch-assistant.png" alt="The dispatch assistant re-planning from a plain sentence" loading="lazy" style="width:100%;height:auto" /><figcaption>Re-planning in plain words.</figcaption></figure>
</div>
{{< /guide-block >}}

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/fieldline/fieldline-backoffice-desktop-poster.jpg" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="The Fieldline back office on the desktop"><source src="/fieldline/fieldline-backoffice-desktop.mp4" type="video/mp4"><a href="/fieldline/fieldline-backoffice-desktop.mp4">Download the back office video</a>.</video>
{{< /guide-block >}}

## The optimizer is plain Java you own

The scheduling engine lives in the `shared` module, with no dependencies and no I/O. It models technicians, jobs and shifts, with hard constraints that can never break (skills, working hours, time windows, time off, maximum driving) and soft ones you weigh against each other (travel, overtime, keeping work where it was).

```java {class="cn1-no-try"}
Problem problem = Instances.city(42, 8, 60, 3).problem();
Plan plan = new Solver(problem, 99).iterations(400).solve().toPlan();
List<String> broken = PlanChecker.violations(problem, plan);   // always empty
```

The `Solver` starts with regret insertion, then improves the plan with adaptive large neighborhood search. It's seeded, so the same seed gives the same plan, which makes it testable. The test suite runs 25 random cities with random crews and asserts that not one hard constraint is ever violated. The `Reoptimizer` handles the part that kills most scheduling software: the day changes at 10:40, and the new plan has to fix the problem without reshuffling every technician who's already on the road.

{{< mermaid >}}
flowchart LR
    J[Jobs, skills, hours,<br/>time off, travel] --> S[Solver<br/>regret insertion + ALNS]
    S --> P[Plan]
    P --> T[Technician app<br/>route on a map]
    P --> D[Dispatcher<br/>Gantt and map]
    E[10:40: Dana is sick] --> R[Reoptimizer<br/>keep work in place]
    P --> R
    R --> P
{{< /mermaid >}}

It runs on the server, and because it's written in the Java subset Codename One compiles, it builds for the client too. Planning a technician's own day on the device, with no signal, is a few lines away.

## Five languages, four databases, real maps

Fieldline runs on iOS, Android, the desktop and the web, in English, Spanish, French, German, and Hebrew. The maps are real OpenStreetMap vector tiles and need no key. The server pushes changes over a WebSocket as they happen, sends push notifications, and runs on SQLite, PostgreSQL, MySQL, or MariaDB. The app and the server share one contract module, so a field one side renamed is a compile error on the other.

The demo accounts are on the [Fieldline page](/fieldline/), and the [user's guide](/developer-guide/fieldline-users-guide/) walks through every screen.

## Scheduling problems are everywhere

Remember the scheduling company I mentioned at the start. Scheduling workers was only one of its projects.

I worked on a system that placed children moving from grade school into middle school: the right school, with the right class following them, under a pile of constraints about distance, siblings, class sizes and who must or must not end up together. I also worked on scheduling chips through the manufacturing pipeline of a major fab. It turns out the order in which chips are produced is crucial for the throughput of the whole line, and it's very hard to calculate.

This is a deep field, and once you start looking you see scheduling problems everywhere. Nurses and shifts. Trucks and loading docks. Exams and rooms. Machines and maintenance windows. Every one of them is the same shape as Fieldline's: resources, jobs, hard rules that can never break and soft goals to weigh against each other.

They used to need a specialist team and years of work. Now you can start from a solver that's already tested, model your own constraints in plain Java, and get to a working answer in days. The rules only your customers have, the integration with their ERP, the spreadsheet the operations manager won't give up (import it, [it's a spreadsheet now](/blog/a-million-rows-and-live-excel-formulas/)): that's where your work starts.

Tomorrow: {{< post-link path="/blog/diagrams-flowcharts-and-kanban-boards" text="diagrams, flowcharts and Kanban boards" >}} as standard components.
